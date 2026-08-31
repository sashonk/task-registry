package ru.asocial.task.service.task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

@Component
public class GroovyScriptRunner {

	private final long timeoutSeconds;

	public GroovyScriptRunner(@Value("${task.script.timeout-seconds:10}") long timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public Object evaluate(String script, Consumer<String> logger) throws Exception {
		Binding binding = new Binding();
		binding.setVariable("log", logger);

		GroovyShell shell = new GroovyShell(binding);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			Callable<Object> task = () -> shell.evaluate(script);
			Future<Object> future = executor.submit(task);
			return future.get(timeoutSeconds, TimeUnit.SECONDS);
		}
		catch (TimeoutException exception) {
			throw new IllegalStateException("Script execution timed out after " + timeoutSeconds + " seconds");
		}
		catch (ExecutionException exception) {
			Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
			if (cause instanceof Exception runtime) {
				throw runtime;
			}
			throw new IllegalStateException(cause.getMessage(), cause);
		}
		finally {
			executor.shutdownNow();
		}
	}
}
