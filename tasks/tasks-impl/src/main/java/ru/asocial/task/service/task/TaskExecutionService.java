package ru.asocial.task.service.task;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import ru.asocial.task.model.Task;
import ru.asocial.task.model.TaskStatus;
import ru.asocial.task.model.TaskType;
import ru.asocial.task.repository.TaskRepository;
import ru.asocial.task.service.TaskLogService;
import ru.asocial.task.service.TaskService;
import ru.asocial.task.service.formula.FormulaEvaluationResult;
import ru.asocial.task.service.formula.FormulaParseException;
import ru.asocial.task.service.formula.FormulaService;

@Service
public class TaskExecutionService {

	private static final Logger log = LoggerFactory.getLogger(TaskExecutionService.class);

	private final TaskService taskService;
	private final TaskLogService taskLogService;
	private final FormulaService formulaService;
	private final GroovyScriptRunner groovyScriptRunner;
	private final JdbcTemplate jdbcTemplate;
	private final TaskRepository taskRepository;
	private final TaskCleanupService taskCleanupService;
	private final Path allowedFileBasePath;

	public TaskExecutionService(
			TaskService taskService,
			TaskLogService taskLogService,
			FormulaService formulaService,
			GroovyScriptRunner groovyScriptRunner,
			JdbcTemplate jdbcTemplate,
			TaskRepository taskRepository,
			TaskCleanupService taskCleanupService,
			@Value("${task.file-check.base-path:./data}") String fileCheckBasePath) {
		this.taskService = taskService;
		this.taskLogService = taskLogService;
		this.formulaService = formulaService;
		this.groovyScriptRunner = groovyScriptRunner;
		this.jdbcTemplate = jdbcTemplate;
		this.taskRepository = taskRepository;
		this.taskCleanupService = taskCleanupService;
		this.allowedFileBasePath = Path.of(fileCheckBasePath).toAbsolutePath().normalize();
	}

	public void execute(Long executorId, Long taskId, TaskExecutionContext context) {
		Task task = taskService.getTaskEntity(taskId);
		if (task.getStatus() != TaskStatus.IN_PROGRESS) {
			return;
		}

		try {
			switch (task.getType()) {
				case CALCULATION -> {
					runCalculation(taskId, task.getFormula());
					taskService.completeTask(taskId);
				}
				case DELAY -> {
					runDelay(taskId, task.getFormula(), context);
					taskService.completeTask(taskId);
				}
				case RANDOM -> {
					runRandom(taskId, task.getFormula());
					taskService.completeTask(taskId);
				}
				case TEXT_TRANSFORM -> {
					runTextTransform(taskId, task.getFormula());
					taskService.completeTask(taskId);
				}
				case HASH -> {
					runHash(taskId, task.getFormula());
					taskService.completeTask(taskId);
				}
				case HTTP_REQUEST -> {
					runHttpRequest(taskId, task.getFormula());
					taskService.completeTask(taskId);
				}
				case FILE_CHECK -> {
					runFileCheck(taskId, task.getFormula());
					taskService.completeTask(taskId);
				}
				case SQL_QUERY -> {
					runSqlQuery(taskId, task.getFormula());
					taskService.completeTask(taskId);
				}
				case REPORT -> {
					runReport(taskId, task.getFormula());
					taskService.completeTask(taskId);
				}
				case CLEANUP -> {
					runCleanup(taskId, task.getFormula());
					taskService.completeTask(taskId);
				}
				case BATCH -> executeBatch(taskId, task.getFormula(), context);
				case SIMULATION -> {
					runSimulation(taskId, task.getFormula(), context);
					taskService.completeTask(taskId);
				}
				case SCRIPT -> {
					runScript(taskId, task.getFormula());
					taskService.completeTask(taskId);
				}
			}
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
		catch (FormulaParseException exception) {
			fail(taskId, "Не удалось распарсить формулу: " + exception.getMessage());
		}
		catch (Exception exception) {
			fail(taskId, formatError(exception));
			log.error("Task {} failed", taskId, exception);
		}
	}

	private void runDelay(Long taskId, String parameters, TaskExecutionContext context) throws InterruptedException {
		JsonNode node = TaskParametersMapper.readTree(parameters);
		int durationSeconds = node.get("durationSeconds").asInt();
		long stepMs = Math.max(500L, durationSeconds * 1000L / 10L);
		long elapsedMs = 0;
		long totalMs = durationSeconds * 1000L;

		while (context.isActive() && elapsedMs < totalMs) {
			if (!isInProgress(taskId)) {
				return;
			}
			long remaining = totalMs - elapsedMs;
			long sleepMs = Math.min(stepMs, remaining);
			context.sleep(sleepMs);
			elapsedMs += sleepMs;
			long percent = Math.min(100L, elapsedMs * 100 / totalMs);
			taskService.updateProgress(taskId, percent);
			taskLogService.addLog(taskId, "Ожидание: " + percent + "% (" + (elapsedMs / 1000) + " / " + durationSeconds + " сек.)");
		}

		taskLogService.addLog(taskId, "Задержка " + durationSeconds + " сек. завершена");
	}

	private void runRandom(Long taskId, String parameters) {
		JsonNode node = TaskParametersMapper.readTree(parameters);
		long min = node.get("min").asLong();
		long max = node.get("max").asLong();
		long result = ThreadLocalRandom.current().nextLong(min, max + 1);
		taskLogService.addLog(taskId, "Диапазон: " + min + " .. " + max);
		taskLogService.addLog(taskId, "Результат: " + result);
	}

	private void runCalculation(Long taskId, String formula) throws FormulaParseException {
		FormulaEvaluationResult result = formulaService.evaluate(formula);
		taskLogService.addLog(taskId, "Исходные параметры: " + result.original());
		taskLogService.addLog(taskId, "Распарсенные параметры: " + result.parsed());
		taskLogService.addLog(taskId, "Результат вычисления: " + result.result());
	}

	private void runTextTransform(Long taskId, String parameters) {
		JsonNode node = TaskParametersMapper.readTree(parameters);
		String text = node.get("text").asText();
		String operation = node.get("operation").asText().toLowerCase();
		String result = switch (operation) {
			case "upper" -> text.toUpperCase();
			case "lower" -> text.toLowerCase();
			case "reverse" -> new StringBuilder(text).reverse().toString();
			case "trim" -> text.trim();
			default -> text;
		};
		taskLogService.addLog(taskId, "Исходный текст: " + text);
		taskLogService.addLog(taskId, "Операция: " + operation);
		taskLogService.addLog(taskId, "Результат: " + result);
	}

	private void runHash(Long taskId, String parameters) throws Exception {
		JsonNode node = TaskParametersMapper.readTree(parameters);
		String text = node.get("text").asText();
		String algorithm = node.get("algorithm").asText();
		MessageDigest digest = MessageDigest.getInstance(algorithm);
		byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
		taskLogService.addLog(taskId, "Алгоритм: " + algorithm);
		taskLogService.addLog(taskId, "Результат: " + HexFormat.of().formatHex(hash));
	}

	private void runHttpRequest(Long taskId, String parameters) throws IOException, InterruptedException {
		JsonNode node = TaskParametersMapper.readTree(parameters);
		String url = node.get("url").asText();
		String method = node.has("method") ? node.get("method").asText().toUpperCase() : "GET";
		int timeoutSeconds = node.has("timeoutSeconds") ? node.get("timeoutSeconds").asInt() : 10;
		String body = node.has("body") ? node.get("body").asText() : null;

		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(Duration.ofSeconds(timeoutSeconds));
		if (body != null && !body.isBlank()) {
			builder.method(method, HttpRequest.BodyPublishers.ofString(body));
		}
		else {
			builder.method(method, HttpRequest.BodyPublishers.noBody());
		}

		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build();
		HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
		taskLogService.addLog(taskId, method + " " + url);
		taskLogService.addLog(taskId, "HTTP " + response.statusCode());
		String responseBody = response.body();
		if (responseBody.length() > 500) {
			responseBody = responseBody.substring(0, 497) + "...";
		}
		taskLogService.addLog(taskId, "Ответ: " + responseBody);
	}

	private void runFileCheck(Long taskId, String parameters) throws IOException {
		JsonNode node = TaskParametersMapper.readTree(parameters);
		Path path = resolveAllowedPath(node.get("path").asText());
		String check = node.has("check") ? node.get("check").asText().toLowerCase() : "exists";
		taskLogService.addLog(taskId, "Путь: " + path);
		switch (check) {
			case "exists" -> taskLogService.addLog(taskId, "Существует: " + Files.exists(path));
			case "isdirectory" -> taskLogService.addLog(taskId, "Каталог: " + Files.isDirectory(path));
			case "size" -> {
				if (!Files.exists(path)) {
					throw new IllegalStateException("File not found: " + path);
				}
				taskLogService.addLog(taskId, "Размер: " + Files.size(path) + " байт");
			}
			default -> throw new IllegalStateException("Unsupported check: " + check);
		}
	}

	private void runSqlQuery(Long taskId, String parameters) {
		JsonNode node = TaskParametersMapper.readTree(parameters);
		String sql = node.get("sql").asText().trim();
		TaskSqlGuard.validateSelect(sql);
		int limit = node.has("limit") ? node.get("limit").asInt() : 100;
		String limitedSql = TaskSqlGuard.withLimit(sql, limit);
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(limitedSql);
		taskLogService.addLog(taskId, "SQL: " + limitedSql);
		taskLogService.addLog(taskId, "Строк: " + rows.size());
		taskLogService.addLog(taskId, "Результат: " + TaskParametersMapper.writeValue(rows));
	}

	private void runReport(Long taskId, String parameters) {
		Integer days = null;
		if (parameters != null && !parameters.isBlank()) {
			JsonNode node = TaskParametersMapper.readTree(parameters);
			if (node.has("days")) {
				days = node.get("days").asInt();
			}
		}

		LocalDateTime cutoff = days != null ? LocalDateTime.now().minusDays(days) : null;
		taskLogService.addLog(taskId, days != null ? "Отчёт за последние " + days + " дн." : "Отчёт по всем задачам");
		taskLogService.addLog(taskId, "Всего: " + taskRepository.count());
		taskLogService.addLog(taskId, "Ожидает: " + countTasks(TaskStatus.NEW, cutoff)
				+ ", выполняется: " + countTasks(TaskStatus.IN_PROGRESS, cutoff));
		taskLogService.addLog(taskId, "Выполнено: " + countTasks(TaskStatus.DONE, cutoff)
				+ ", ошибок: " + countTasks(TaskStatus.ERROR, cutoff)
				+ ", прервано: " + countTasks(TaskStatus.ABORTED, cutoff));
	}

	private void runCleanup(Long taskId, String parameters) {
		JsonNode node = TaskParametersMapper.readTree(parameters);
		String target = node.get("target").asText().toLowerCase();
		int daysOld = node.get("daysOld").asInt();

		int deleted = taskCleanupService.cleanup(target, daysOld);

		taskLogService.addLog(taskId, "Очистка: " + target + ", старше " + daysOld + " дн.");
		taskLogService.addLog(taskId, "Удалено записей: " + deleted);
	}

	private void executeBatch(Long taskId, String parameters, TaskExecutionContext context)
			throws InterruptedException, FormulaParseException {
		JsonNode node = TaskParametersMapper.readTree(parameters);
		int index = 1;
		for (JsonNode step : node.get("steps")) {
			if (!context.isActive() || !isInProgress(taskId)) {
				return;
			}

			TaskType stepType = TaskType.valueOf(step.get("type").asText());
			String stepParameters = extractStepParameters(step);
			taskLogService.addLog(taskId, "Шаг " + index + ": " + stepType);
			runBatchStep(taskId, stepType, stepParameters, context);
			index++;
		}

		taskService.completeTask(taskId);
		taskLogService.addLog(taskId, "Пакет из " + (index - 1) + " шагов выполнен");
	}

	private void runBatchStep(Long taskId, TaskType type, String parameters, TaskExecutionContext context)
			throws InterruptedException, FormulaParseException {
		switch (type) {
			case CALCULATION -> runCalculation(taskId, parameters);
			case DELAY -> runDelay(taskId, parameters, context);
			case RANDOM -> runRandom(taskId, parameters);
			case TEXT_TRANSFORM -> runTextTransform(taskId, parameters);
			case HASH -> {
				try {
					runHash(taskId, parameters);
				}
				catch (Exception exception) {
					throw new IllegalStateException(exception);
				}
			}
			case HTTP_REQUEST -> {
				try {
					runHttpRequest(taskId, parameters);
				}
				catch (Exception exception) {
					throw new IllegalStateException(exception);
				}
			}
			case FILE_CHECK -> {
				try {
					runFileCheck(taskId, parameters);
				}
				catch (Exception exception) {
					throw new IllegalStateException(exception);
				}
			}
			case SQL_QUERY -> runSqlQuery(taskId, parameters);
			case REPORT -> runReport(taskId, parameters);
			case CLEANUP -> runCleanup(taskId, parameters);
			case SIMULATION -> runSimulation(taskId, parameters, context);
			case SCRIPT -> {
				try {
					runScript(taskId, parameters);
				}
				catch (Exception exception) {
					throw new IllegalStateException(exception);
				}
			}
			default -> throw new IllegalStateException("Unsupported batch step: " + type);
		}
	}

	private void runSimulation(Long taskId, String parameters, TaskExecutionContext context) throws InterruptedException {
		JsonNode node = TaskParametersMapper.readTree(parameters);
		int durationSeconds = node.get("durationSeconds").asInt();
		int intensity = node.has("intensity") ? node.get("intensity").asInt() : 50;
		long stepMs = Math.max(500L, durationSeconds * 1000L / 10L);
		long elapsedMs = 0;
		long totalMs = durationSeconds * 1000L;

		while (context.isActive() && elapsedMs < totalMs) {
			if (!isInProgress(taskId)) {
				return;
			}
			runCpuBurst(intensity);
			long remaining = totalMs - elapsedMs;
			long sleepMs = Math.min(stepMs, remaining);
			context.sleep(sleepMs);
			elapsedMs += sleepMs;
			long percent = Math.min(100L, elapsedMs * 100 / totalMs);
			taskService.updateProgress(taskId, percent);
			taskLogService.addLog(taskId, "Симуляция: " + percent + "%, интенсивность " + intensity);
		}
	}

	private void runScript(Long taskId, String parameters) throws Exception {
		JsonNode node = TaskParametersMapper.readTree(parameters);
		String script = node.get("script").asText();
		Object result = groovyScriptRunner.evaluate(script, message -> taskLogService.addLog(taskId, message));
		taskLogService.addLog(taskId, "Результат скрипта: " + result);
	}

	private long countTasks(TaskStatus status, LocalDateTime cutoff) {
		if (cutoff == null) {
			return taskRepository.countByStatus(status);
		}
		return taskRepository.countByStatusAndCreatedAtAfter(status, cutoff);
	}

	private boolean isInProgress(Long taskId) {
		return taskService.getTaskEntity(taskId).getStatus() == TaskStatus.IN_PROGRESS;
	}

	private Path resolveAllowedPath(String rawPath) {
		Path resolved = allowedFileBasePath.resolve(rawPath).normalize();
		if (!resolved.startsWith(allowedFileBasePath)) {
			throw new IllegalStateException("Path is outside allowed directory");
		}
		return resolved;
	}

	private String extractStepParameters(JsonNode step) {
		if (!step.has("parameters")) {
			return null;
		}
		JsonNode parameters = step.get("parameters");
		return parameters.isTextual() ? parameters.asText() : parameters.toString();
	}

	private void fail(Long taskId, String message) {
		taskLogService.addLog(taskId, message);
		taskService.updateTaskStatus(taskId, TaskStatus.ERROR);
	}

	private String formatError(Throwable throwable) {
		String message = throwable.getClass().getSimpleName();
		if (throwable.getMessage() != null) {
			message += ": " + throwable.getMessage();
		}
		return message.length() > 1000 ? message.substring(0, 997) + "..." : message;
	}

	private void runCpuBurst(int intensity) {
		long iterations = intensity * 50_000L;
		double value = 0;
		for (long index = 0; index < iterations; index++) {
			value += Math.sqrt(index % 1000 + 1);
		}
		if (value < 0) {
			log.debug("Simulation sink: {}", value);
		}
	}
}
