import ilia.isakhin.timed.coroutines.annotation.TimedCoroutine
import io.micrometer.core.annotation.Timed
import kotlinx.coroutines.delay

@TimedCoroutine("call", extraTags = ["one", "two"])
@Timed("call")
open class SuspendTimedService {

    @TimedCoroutine("call", extraTags = ["one", "two"])
    @Timed("call")
    fun func() {

    }

    @TimedCoroutine("longCall", extraTags = ["one", "two"], longTask = true)
    suspend fun longFunc() {
        delay(0)
    }
}