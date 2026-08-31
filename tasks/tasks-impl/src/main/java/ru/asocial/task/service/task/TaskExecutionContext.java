package ru.asocial.task.service.task;

@FunctionalInterface
public interface TaskExecutionContext {

	boolean isActive();

	default void sleep(long delayMs) throws InterruptedException {
		Thread.sleep(delayMs);
	}
}
