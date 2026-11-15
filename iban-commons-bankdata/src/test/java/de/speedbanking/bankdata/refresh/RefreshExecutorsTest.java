package de.speedbanking.bankdata.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.speedbanking.bankdata.BankDataRegistry;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for {@link RefreshExecutors}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class RefreshExecutorsTest {

    @Test
    void get_returnsSameSharedInstanceEveryTime() {
        assertThat(RefreshExecutors.get()).isSameAs(RefreshExecutors.get());
    }

    @Test
    void get_executesSubmittedTaskOnNamedDaemonThread() throws InterruptedException {
        AtomicReference<Thread> executingThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        RefreshExecutors.get().execute(() -> {
            executingThread.set(Thread.currentThread());
            latch.countDown();
        });

        latch.await();
        assertThat(executingThread.get().getName()).startsWith("bankdata-refresh-");
        assertThat(executingThread.get().isDaemon()).isTrue();
    }

    @Test
    void get_poolSizeTracksRegisteredCountryCount_withinMinMaxClamp() {
        int countryCount = BankDataRegistry.getSupportedCountryCodes().size();
        int expectedPoolSize = Math.max(2, Math.min(8, countryCount));

        assertThat(RefreshExecutors.get()).isInstanceOfSatisfying(ThreadPoolExecutor.class,
            executor -> assertThat(executor.getCorePoolSize()).isEqualTo(expectedPoolSize));
    }

    @Test
    void constructor_private_throwsUnsupportedOperationException() throws NoSuchMethodException {
        Constructor<RefreshExecutors> constructor = RefreshExecutors.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertThat(exception.getCause()).isInstanceOf(UnsupportedOperationException.class);
    }

}
