package ru.asocial.task.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.asocial.task.model.TaskLog;
import ru.asocial.task.model.TaskStatus;

public interface TaskLogRepository extends JpaRepository<TaskLog, Long> {

	List<TaskLog> findByTask_IdOrderByCreatedAtAsc(Long taskId);

	@Query("SELECT l FROM TaskLog l JOIN FETCH l.task ORDER BY l.createdAt DESC, l.id DESC")
	List<TaskLog> findAllWithTaskOrderByCreatedAtDesc();

	@EntityGraph(attributePaths = "task")
	Page<TaskLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM TaskLog l WHERE l.createdAt < :cutoff")
	int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM TaskLog l WHERE l.task.id IN ("
			+ "SELECT t.id FROM Task t WHERE t.completedAt < :cutoff AND t.status IN :statuses)")
	int deleteLogsForCompletedTasksBefore(
			@Param("cutoff") LocalDateTime cutoff,
			@Param("statuses") List<TaskStatus> statuses);
}
