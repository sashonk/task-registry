package ru.asocial.task.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import ru.asocial.task.dto.ExecutorCreateRequest;
import ru.asocial.task.dto.ExecutorResponse;
import ru.asocial.task.model.Executor;
import ru.asocial.task.model.Task;
import ru.asocial.task.repository.ExecutorRepository;
import ru.asocial.task.service.task.TaskExecutionContext;
import ru.asocial.task.service.task.TaskExecutionService;

@Component
@ConditionalOnProperty(name = "worker.enabled", havingValue = "true", matchIfMissing = true)
public class WorkerService {

	private static final Logger log = LoggerFactory.getLogger(WorkerService.class);
	private static final Object WORKER_LOCK = new Object();
	private static volatile boolean initialWorkersStarted = false;

	private static final List<String> EXECUTOR_NAMES = List.of(
			"Сириус", "Вега", "Альтаир", "Ригель", "Бетельгейзе", "Антарес", "Арктур",
			"Поллукс", "Кастор", "Денеб", "Альдебаран", "Спика", "Регул", "Фомальгаут",
			"Процион", "Мирак", "Алькор", "Мицар", "Шедар", "Алгениб");

	private final ExecutorRepository executorRepository;
	private final TaskService taskService;
	private final ExecutorService executorService;
	private final TaskExecutionService taskExecutionService;
	private final Map<Long, Thread> workerThreads = new ConcurrentHashMap<>();

	@Value("${worker.poll-delay-ms:1000}")
	private long pollDelayMs;

	private volatile boolean running = true;

	public WorkerService(
			ExecutorRepository executorRepository,
			TaskService taskService,
			ExecutorService executorService,
			TaskExecutionService taskExecutionService) {
		this.executorRepository = executorRepository;
		this.taskService = taskService;
		this.executorService = executorService;
		this.taskExecutionService = taskExecutionService;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void startWorkersFromDatabase() {
		synchronized (WORKER_LOCK) {
			if (initialWorkersStarted) {
				log.info("Workers already started, skipping duplicate startup");
				return;
			}
			initialWorkersStarted = true;
		}

		List<Executor> executors = executorService.findAllExecutors();
		if (executors.isEmpty()) {
			log.info("No executors in database, no workers started");
			return;
		}

		executorService.resetAllToIdle();

		for (Executor executor : executors) {
			startWorkerThread(executor.getId());
			log.info("Worker started for executor: {}", executor.getName());
		}
	}

	public ExecutorResponse spawnWorker() {
		synchronized (WORKER_LOCK) {
			String name = generateUniqueWorkerName();
			ExecutorResponse worker = executorService.createExecutor(new ExecutorCreateRequest(name));
			startWorkerThread(worker.id());
			log.info("Worker registered as executor: {}", worker.name());
			return worker;
		}
	}

	@PreDestroy
	public void stopAllWorkers() {
		running = false;
		synchronized (WORKER_LOCK) {
			workerThreads.values().forEach(Thread::interrupt);
			workerThreads.clear();
			initialWorkersStarted = false;
		}
	}

	private void startWorkerThread(Long executorId) {
		if (workerThreads.containsKey(executorId)) {
			log.warn("Worker thread already running for executor {}", executorId);
			return;
		}

		Thread thread = new Thread(() -> runLoop(executorId), "task-worker-" + executorId);
		thread.setDaemon(true);
		workerThreads.put(executorId, thread);
		thread.start();
	}

	private void runLoop(Long executorId) {
		while (running && workerThreads.containsKey(executorId)) {
			try {
				Optional<Task> claimedTask = taskService.claimNextTask(executorId);
				if (claimedTask.isPresent()) {
					TaskExecutionContext context = () -> running && workerThreads.containsKey(executorId);
					taskExecutionService.execute(executorId, claimedTask.get().getId(), context);
				}
				else {
					Thread.sleep(pollDelayMs);
				}
			}
			catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				break;
			}
			catch (Exception exception) {
				log.error("Worker {} loop error", executorId, exception);
				sleepQuietly(pollDelayMs);
			}
		}

		workerThreads.remove(executorId);
	}

	private String generateUniqueWorkerName() {
		java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();

		for (int attempt = 0; attempt < 100; attempt++) {
			String candidate = EXECUTOR_NAMES.get(random.nextInt(EXECUTOR_NAMES.size()));
			if (!executorRepository.existsByName(candidate)) {
				return candidate;
			}
		}

		String fallback;
		do {
			fallback = EXECUTOR_NAMES.get(random.nextInt(EXECUTOR_NAMES.size())) + "-" + random.nextInt(1000, 9999);
		}
		while (executorRepository.existsByName(fallback));

		return fallback;
	}

	private void sleepQuietly(long delayMs) {
		try {
			Thread.sleep(delayMs);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
	}
}
