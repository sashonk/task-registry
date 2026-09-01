package ru.asocial.scheduler.dto;

import java.time.Instant;

import ru.asocial.scheduler.service.TaskDisplayNames;

public record ScheduleTableRowResponse(
		Long id,
		String taskName,
		Instant nextRunAt,
		String repeatText,
		boolean enabled,
		String statusText,
		Instant lastTriggeredAt) {

	public static ScheduleTableRowResponse from(ScheduleResponse schedule) {
		return new ScheduleTableRowResponse(
				schedule.id(),
				formatTaskName(schedule),
				schedule.nextRunAt(),
				formatRepeat(schedule.repeatIntervalMinutes()),
				schedule.enabled(),
				schedule.enabled() ? "Включено" : "Выключено",
				schedule.lastTriggeredAt());
	}

	private static String formatTaskName(ScheduleResponse schedule) {
		return TaskDisplayNames.format(schedule.taskType(), schedule.formula());
	}

	private static String formatRepeat(Long repeatIntervalMinutes) {
		if (repeatIntervalMinutes == null || repeatIntervalMinutes <= 0) {
			return "Один раз";
		}

		if (repeatIntervalMinutes % 60 == 0 && repeatIntervalMinutes >= 60) {
			long hours = repeatIntervalMinutes / 60;
			return "Каждые " + hours + " ч.";
		}

		return "Каждые " + repeatIntervalMinutes + " мин.";
	}
}
