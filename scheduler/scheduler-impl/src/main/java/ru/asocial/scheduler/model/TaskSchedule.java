package ru.asocial.scheduler.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import ru.asocial.task.model.TaskType;

@Entity
@Table(name = "task_schedules")
public class TaskSchedule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 32)
	private TaskType taskType;

	@Column(length = 4000)
	private String formula;

	@Column(nullable = false)
	private Instant nextRunAt;

	@Column
	private Long repeatIntervalMinutes;

	@Column(nullable = false)
	private boolean enabled = true;

	private Instant lastTriggeredAt;

	@Column(nullable = false)
	private Instant createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public TaskType getTaskType() {
		return taskType;
	}

	public void setTaskType(TaskType taskType) {
		this.taskType = taskType;
	}

	public String getFormula() {
		return formula;
	}

	public void setFormula(String formula) {
		this.formula = formula;
	}

	public Instant getNextRunAt() {
		return nextRunAt;
	}

	public void setNextRunAt(Instant nextRunAt) {
		this.nextRunAt = nextRunAt;
	}

	public Long getRepeatIntervalMinutes() {
		return repeatIntervalMinutes;
	}

	public void setRepeatIntervalMinutes(Long repeatIntervalMinutes) {
		this.repeatIntervalMinutes = repeatIntervalMinutes;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Instant getLastTriggeredAt() {
		return lastTriggeredAt;
	}

	public void setLastTriggeredAt(Instant lastTriggeredAt) {
		this.lastTriggeredAt = lastTriggeredAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
