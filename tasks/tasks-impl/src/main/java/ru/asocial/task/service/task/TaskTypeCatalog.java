package ru.asocial.task.service.task;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import ru.asocial.task.dto.TaskTypeInfoResponse;
import ru.asocial.task.model.TaskType;

@Service
public class TaskTypeCatalog {

	public List<TaskTypeInfoResponse> getAll() {
		return Arrays.stream(TaskType.values())
				.map(this::toInfo)
				.toList();
	}

	private TaskTypeInfoResponse toInfo(TaskType type) {
		return switch (type) {
			case CALCULATION -> new TaskTypeInfoResponse(
					type.name(),
					"Расчёт",
					"Вычисляет математическую формулу с операциями +, −, *, / и скобками.",
					"Строка с формулой.",
					"2 + 3 * (4 - 1)",
					true);
			case DELAY -> new TaskTypeInfoResponse(
					type.name(),
					"Задержка",
					"Приостанавливает выполнение на заданное время с пошаговым обновлением процента выполнения.",
					"JSON: durationSeconds — длительность в секундах.",
					"{\"durationSeconds\": 30}",
					true);
			case RANDOM -> new TaskTypeInfoResponse(
					type.name(),
					"Случайное число",
					"Генерирует целое случайное число в указанном диапазоне включительно.",
					"JSON: min и max — границы диапазона.",
					"{\"min\": 1, \"max\": 100}",
					true);
			case TEXT_TRANSFORM -> new TaskTypeInfoResponse(
					type.name(),
					"Текст",
					"Преобразует строку: верхний/нижний регистр, разворот или обрезка пробелов.",
					"JSON: text — исходный текст; operation — upper, lower, reverse или trim.",
					"{\"text\": \"Hello\", \"operation\": \"upper\"}",
					true);
			case HASH -> new TaskTypeInfoResponse(
					type.name(),
					"Хеш",
					"Вычисляет криптографический хеш текста.",
					"JSON: text — данные; algorithm — MD5 или SHA-256.",
					"{\"text\": \"hello\", \"algorithm\": \"SHA-256\"}",
					true);
			case HTTP_REQUEST -> new TaskTypeInfoResponse(
					type.name(),
					"HTTP-запрос",
					"Выполняет HTTP-запрос к указанному URL и записывает код ответа в лог.",
					"JSON: url (обязательно); method, timeoutSeconds, body — необязательно.",
					"{\"url\": \"http://localhost:8080/api/tasks\", \"method\": \"GET\", \"timeoutSeconds\": 10}",
					true);
			case FILE_CHECK -> new TaskTypeInfoResponse(
					type.name(),
					"Проверка файла",
					"Проверяет файл или каталог внутри каталога ./data.",
					"JSON: path — относительный путь; check — exists, size или isDirectory.",
					"{\"path\": \".\", \"check\": \"exists\"}",
					true);
			case SQL_QUERY -> new TaskTypeInfoResponse(
					type.name(),
					"SQL-запрос",
					"Выполняет SELECT-запрос к базе данных приложения.",
					"JSON: sql — только SELECT без точки с запятой.",
					"{\"sql\": \"SELECT COUNT(*) AS cnt FROM tasks\"}",
					true);
			case REPORT -> new TaskTypeInfoResponse(
					type.name(),
					"Отчёт",
					"Формирует сводку по количеству задач в каждом статусе.",
					"JSON: days — необязательно, число дней для фильтрации по дате создания.",
					"{\"days\": 7}",
					false);
			case CLEANUP -> new TaskTypeInfoResponse(
					type.name(),
					"Очистка",
					"Удаляет старые логи или завершённые задачи.",
					"JSON: target — logs или tasks; daysOld — возраст записей в днях.",
					"{\"target\": \"logs\", \"daysOld\": 30}",
					true);
			case BATCH -> new TaskTypeInfoResponse(
					type.name(),
					"Пакет",
					"Последовательно выполняет несколько шагов разных типов в одной задаче.",
					"JSON: steps — массив объектов с полями type и parameters.",
					"{\"steps\": [{\"type\": \"RANDOM\", \"parameters\": {\"min\": 1, \"max\": 10}}, {\"type\": \"CALCULATION\", \"parameters\": \"2+2\"}]}",
					true);
			case SIMULATION -> new TaskTypeInfoResponse(
					type.name(),
					"Симуляция",
					"Имитирует длительную работу с периодическими сообщениями в лог.",
					"JSON: durationSeconds — длительность; intensity (1–100) — необязательно.",
					"{\"durationSeconds\": 15, \"intensity\": 50}",
					true);
			case SCRIPT -> new TaskTypeInfoResponse(
					type.name(),
					"Groovy-скрипт",
					"Выполняет Groovy-скрипт с доступом к функции log() для записи в лог задачи.",
					"JSON: script — текст скрипта.",
					"{\"script\": \"log(\\\"Hello\\\"); 2 + 2\"}",
					true);
			case RSS_READ -> new TaskTypeInfoResponse(
					type.name(),
					"Чтение RSS",
					"Скачивает RSS или Atom ленту и записывает заголовки, ссылки и даты записей в лог.",
					"JSON: url — адрес ленты; maxItems и timeoutSeconds — необязательно.",
					"{\"url\": \"https://example.com/feed.xml\", \"maxItems\": 10}",
					true);
		};
	}
}
