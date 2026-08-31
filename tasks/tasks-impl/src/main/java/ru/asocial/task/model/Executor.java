package ru.asocial.task.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "executors")
public class Executor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ExecutorState state;

	@Column(name = "successful_tasks_count", nullable = false)
	private long successfulTasksCount;

	@Column(name = "error_tasks_count", nullable = false)
	private long errorTasksCount;

	@Column(name = "aborted_tasks_count", nullable = false)
	private long abortedTasksCount;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ExecutorState getState() {
		return state;
	}

	public void setState(ExecutorState state) {
		this.state = state;
	}

	public long getSuccessfulTasksCount() {
		return successfulTasksCount;
	}

	public void setSuccessfulTasksCount(long successfulTasksCount) {
		this.successfulTasksCount = successfulTasksCount;
	}

	public long getErrorTasksCount() {
		return errorTasksCount;
	}

	public void setErrorTasksCount(long errorTasksCount) {
		this.errorTasksCount = errorTasksCount;
	}

	public long getAbortedTasksCount() {
		return abortedTasksCount;
	}

	public void setAbortedTasksCount(long abortedTasksCount) {
		this.abortedTasksCount = abortedTasksCount;
	}
}
