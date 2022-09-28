package ilia.isakhin.timed.coroutines.annotation

import java.lang.annotation.Inherited

@Target(allowedTargets = [AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION])
@Repeatable
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class TimedCoroutine(
    val name: String = "",
    val extraTags: Array<String> = [],
    val longTask: Boolean = false,
    val percentiles: DoubleArray = [],
    val histogram: Boolean = false,
    val description: String = "",
)
