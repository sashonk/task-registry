package ru.asocial.task.service.task;

import ru.asocial.task.model.TaskType;

public final class TaskDisplayNames {

	private TaskDisplayNames() {
	}

	public static String format(TaskType type, String parameters) {
		return switch (type) {
			case CALCULATION -> parameters != null && !parameters.isBlank() ? "Расчёт: " + parameters : "Расчёт";
			case DELAY -> "Задержка";
			case RANDOM -> "Случайное число";
			case TEXT_TRANSFORM -> "Текст";
			case HASH -> "Хеш";
			case HTTP_REQUEST -> "HTTP-запрос";
			case FILE_CHECK -> "Проверка файла";
			case SQL_QUERY -> "SQL-запрос";
			case REPORT -> "Отчёт";
			case CLEANUP -> "Очистка";
			case BATCH -> "Пакет";
			case SIMULATION -> "Симуляция";
			case SCRIPT -> "Groovy-скрипт";
			case RSS_READ -> "Чтение RSS";
		};
	}
}
