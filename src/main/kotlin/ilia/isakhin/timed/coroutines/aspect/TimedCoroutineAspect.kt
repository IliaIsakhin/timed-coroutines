package ilia.isakhin.timed.coroutines.aspect

import ilia.isakhin.timed.coroutines.annotation.TimedCoroutine
import io.micrometer.core.aop.TimedAspect.DEFAULT_EXCEPTION_TAG_VALUE
import io.micrometer.core.aop.TimedAspect.DEFAULT_METRIC_NAME
import io.micrometer.core.aop.TimedAspect.EXCEPTION_TAG
import io.micrometer.core.instrument.LongTaskTimer
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

@Aspect
class TimedCoroutineAspect(
    private val meterRegistry: MeterRegistry,
    private val tagsBasedOnJoinPoint: (ProceedingJoinPoint) -> Iterable<Tag> = CLASS_AND_METHOD_TAGS,
    private val shouldSkip: ((ProceedingJoinPoint) -> Boolean) = DONT_SKIP_ANYTHING,
) {

    @Around("@annotation(ilia.isakhin.timed.coroutines.annotation.TimedCoroutine)")
    fun timedMethod(pjp: ProceedingJoinPoint): Any? {
        if (shouldSkip.invoke(pjp)) pjp.proceed()

        var method: Method = (pjp.signature as MethodSignature).method
        var timedCoroutine: TimedCoroutine? = method.getAnnotation(TimedCoroutine::class.java)
        if (timedCoroutine == null) {
            method = pjp.target::class.java.getMethod(method.name, *method.parameterTypes)
            timedCoroutine = method.getAnnotation(TimedCoroutine::class.java)
        }

        return perform(pjp, timedCoroutine as TimedCoroutine)
    }

    private fun perform(pjp: ProceedingJoinPoint, timedCoroutine: TimedCoroutine): Any? {
        val metricName = timedCoroutine.name.ifEmpty { DEFAULT_METRIC_NAME }
        @Suppress("UNCHECKED_CAST")
        val continuationParameter = pjp.args.last() as Continuation<Any?>
        val otherArgs = pjp.args.sliceArray(0 until pjp.args.size - 1)

        return runCoroutine(continuationParameter) {
            if (timedCoroutine.longTask) {
                processWithTimer(pjp, timedCoroutine, metricName, otherArgs)
            } else {
                processWithLongTaskTimer(pjp, timedCoroutine, metricName, otherArgs)
            }
        }
    }

    private suspend fun processWithTimer(pjp: ProceedingJoinPoint, timedCoroutine: TimedCoroutine, metricName: String, otherArgs: Array<Any?>): Any? {
        val timer = Timer.start(meterRegistry)
        var exception = DEFAULT_EXCEPTION_TAG_VALUE

        return try {
            suspendCoroutineUninterceptedOrReturn { pjp.proceed(otherArgs + it) }
        } catch (ex: Exception) {
            exception = exception::class.java.simpleName
        } finally {
            record(pjp, timedCoroutine, metricName, timer, exception)
        }
    }

    private suspend fun processWithLongTaskTimer(
        pjp: ProceedingJoinPoint,
        timedCoroutine: TimedCoroutine,
        metricName: String,
        otherArgs: Array<Any?>,
    ): Any? {
        val longTimer: LongTaskTimer.Sample? = buildLongTaskTimer(metricName, timedCoroutine, pjp)

        return try {
            suspendCoroutineUninterceptedOrReturn { pjp.proceed(otherArgs + it) }
        } finally {
            longTimer?.stop()
        }
    }

    private fun buildLongTaskTimer(metricName: String, timedCoroutine: TimedCoroutine, pjp: ProceedingJoinPoint) =
        try {
            LongTaskTimer.builder(metricName)
                .description(timedCoroutine.description)
                .tags(*timedCoroutine.extraTags)
                .tags(tagsBasedOnJoinPoint.invoke(pjp))
                .register(meterRegistry)
                .start()
        } catch (ex: Exception) {
            null
        }

    private fun record(
        pjp: ProceedingJoinPoint,
        timedCoroutine: TimedCoroutine,
        metricName: String,
        timer: Timer.Sample,
        exception: String
    ) {
        try {
            timer.stop(
                Timer.builder(metricName)
                    .description(timedCoroutine.description.ifEmpty { null })
                    .tags(*timedCoroutine.extraTags)
                    .tags(EXCEPTION_TAG, exception)
                    .tags(tagsBasedOnJoinPoint.invoke(pjp))
                    .publishPercentileHistogram(timedCoroutine.histogram)
                    .publishPercentiles(*timedCoroutine.percentiles)
                    .register(meterRegistry)
            )
        } catch (ignored: Exception) {
        }
    }

    private fun runCoroutine(continuationParameter: Continuation<Any?>, block: suspend () -> Any?): Any? =
        block.startCoroutineUninterceptedOrReturn(continuationParameter)

    companion object {
        private val DONT_SKIP_ANYTHING = { _: ProceedingJoinPoint -> false }
        private val CLASS_AND_METHOD_TAGS = { pjp: ProceedingJoinPoint ->
            Tags.of(
                "class", pjp.staticPart.signature.declaringTypeName,
                "method", pjp.staticPart.signature.name
            )
        }
    }
}
