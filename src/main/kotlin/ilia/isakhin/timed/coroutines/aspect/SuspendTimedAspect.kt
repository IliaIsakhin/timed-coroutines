package ilia.isakhin.timed.coroutines.aspect

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

@Aspect
@Component
class CoroutineLoggingAspect(private val meterRegistry: MeterRegistry) {

    @Around("@annotation(ilia.isakhin.timed.coroutines.aspect.SuspendTimed) && args(.., kotlin.coroutines.Continuation)")
    fun logResult(joinPoint: ProceedingJoinPoint): Any? {
        @Suppress("UNCHECKED_CAST")
        val continuationParameter = joinPoint.args.last() as Continuation<Any?>
        val otherArgs = joinPoint.args.sliceArray(0 until joinPoint.args.size - 1)

        return runCoroutine(continuationParameter) {
            val timer = Timer.start(meterRegistry)

            try {
                suspendCoroutineUninterceptedOrReturn { joinPoint.proceed(otherArgs + it) }
            } finally {
                timer.stop(
                    Timer.builder("my-suspend-metric")
                        .register(meterRegistry)
                )
            }
        }
    }

    fun runCoroutine(continuationParameter: Continuation<Any?>, block: suspend () -> Any?): Any? =
        block.startCoroutineUninterceptedOrReturn(continuationParameter)
}

annotation class TimedCoroutine
