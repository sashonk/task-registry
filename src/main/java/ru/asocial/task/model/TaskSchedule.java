package ru.asocial.task.model;

import java.time.LocalDateTime;

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
	private LocalDateTime nextRunAt;

	@Column
	private Long repeatIntervalMinutes;

	@Column(nullable = false)
	private boolean enabled = true;

	private LocalDateTime lastTriggeredAt;

	@Column(nullable = false)
	private LocalDateTime createdAt;

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

	public LocalDateTime getNextRunAt() {
		return nextRunAt;
	}

	public void setNextRunAt(LocalDateTime nextRunAt) {
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

	public LocalDateTime getLastTriggeredAt() {
		return lastTriggeredAt;
	}

	public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) {
		this.lastTriggeredAt = lastTriggeredAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
