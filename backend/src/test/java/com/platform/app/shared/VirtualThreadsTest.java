package com.platform.app.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.AsyncTaskExecutor;

class VirtualThreadsTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
      .withPropertyValues("spring.threads.virtual.enabled=true");

  @Test
  @DisplayName("Should execute tasks on virtual threads when spring.threads.virtual.enabled is true")
  void shouldExecuteTasksOnVirtualThreads() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(AsyncTaskExecutor.class);
      AsyncTaskExecutor executor = context.getBean(AsyncTaskExecutor.class);

      CountDownLatch latch = new CountDownLatch(1);
      AtomicBoolean isVirtualThread = new AtomicBoolean(false);

      executor.execute(() -> {
        isVirtualThread.set(Thread.currentThread().isVirtual());
        latch.countDown();
      });

      boolean completed = latch.await(5, TimeUnit.SECONDS);
      assertThat(completed).isTrue();
      assertThat(isVirtualThread.get()).isTrue();
    });
  }

  @Test
  @DisplayName("Should verify JVM platform supports Loom Virtual Threads")
  void shouldVerifyJvmSupportsVirtualThreads() throws InterruptedException {
    AtomicBoolean isVirtualThread = new AtomicBoolean(false);
    Thread thread = Thread.ofVirtual().name("vt-verification-thread").start(() -> {
      isVirtualThread.set(Thread.currentThread().isVirtual());
    });
    thread.join(5000);

    assertThat(thread.isVirtual()).isTrue();
    assertThat(isVirtualThread.get()).isTrue();
  }
}
