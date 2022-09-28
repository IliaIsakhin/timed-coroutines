package ilia.isakhin.timed.coroutines.aspect

import SuspendTimedService
import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory

class SuspendTimedAspectTest {

    private lateinit var registry: MeterRegistry
    private lateinit var aspectJProxyFactory: AspectJProxyFactory

    @BeforeEach
    fun cleanup() {
        registry = SimpleMeterRegistry()
        aspectJProxyFactory = AspectJProxyFactory(SuspendTimedService())
    }

    @Test
    fun timedMethod() {
        aspectJProxyFactory.addAspect(TimedAspect(registry))
        aspectJProxyFactory.addAspect(TimedCoroutineAspect(registry))

        aspectJProxyFactory.getProxy<SuspendTimedService>().func()

        assertThat(
            registry.get("call").timer().count()
        ).isNotEqualTo(0)
    }
}
