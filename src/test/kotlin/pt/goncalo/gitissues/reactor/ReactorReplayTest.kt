package pt.goncalo.gitissues.reactor

import org.junit.jupiter.api.Test
import reactor.core.publisher.Sinks
import reactor.test.StepVerifier
import java.time.Duration
import java.util.stream.IntStream

class ReactorReplayTest {


    /**
     * This test is failing, because MulticastReplaySpec seems to only clear expired items after an operation
     * (like next) is called
     *
     */
    @Test
    fun `it should receive no event if sent before maxAge gap - even without add`() {
        val maxAge = Duration.ofSeconds(1)
        val instance = Sinks.many().replay().limit<String>(maxAge)

        IntStream
            .range(0, 100)
            .mapToObj { "$it" }
            .forEach {
                instance.tryEmitNext(it)
            }
        // Sleep for maxAge / TTL
        Thread.sleep(maxAge.toMillis())
        instance.tryEmitComplete()
        StepVerifier
            .create(instance.asFlux())
            .expectComplete()
            .verify(Duration.ofSeconds(1))


    }

    /**
     * This test passes ( as expected ) but only because next was called before the completion of the publisher.
     */
    @Test
    fun `it should receive no event if sent before maxAge gap`() {
        val maxAge = Duration.ofSeconds(1)
        val instance = Sinks.many().replay().limit<String>(maxAge)
        IntStream
            .range(0, 100)
            .mapToObj { "$it" }
            .forEach {
                instance.tryEmitNext(it)
            }
        Thread.sleep(maxAge.toMillis())

        // This shouldn't be required.. but if i emit this value, previous values (expired ) are correctly removed
        instance.tryEmitNext("lastOne")
        instance.tryEmitComplete()
        StepVerifier
            .create(instance.asFlux())
            .expectNext("lastOne")
            .expectComplete()
            .verify(Duration.ofSeconds(1))


    }
}
