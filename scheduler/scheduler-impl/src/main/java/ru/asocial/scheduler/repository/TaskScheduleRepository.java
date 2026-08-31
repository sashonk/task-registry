package ru.asocial.scheduler.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.asocial.scheduler.model.TaskSchedule;

public interface TaskScheduleRepository extends JpaRepository<TaskSchedule, Long> {

	List<TaskSchedule> findByEnabledTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(LocalDateTime now);
}
