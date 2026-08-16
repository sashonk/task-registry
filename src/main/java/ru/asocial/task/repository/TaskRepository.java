package ru.asocial.task.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import ru.asocial.task.model.Task;
import ru.asocial.task.model.TaskStatus;

public interface TaskRepository extends JpaRepository<Task, Long> {

	Page<Task> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Task> findFirstByStatusOrderByCreatedAtAsc(TaskStatus status);

	long countByStatus(TaskStatus status);

	long countByStatusAndCreatedAtAfter(TaskStatus status, LocalDateTime createdAt);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM Task t WHERE t.completedAt < :cutoff AND t.status IN :statuses")
	int deleteTerminalTasksBefore(@Param("cutoff") LocalDateTime cutoff, @Param("statuses") List<TaskStatus> statuses);
}
