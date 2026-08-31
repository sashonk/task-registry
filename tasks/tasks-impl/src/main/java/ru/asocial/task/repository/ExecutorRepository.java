package ru.asocial.task.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import ru.asocial.task.model.Executor;

public interface ExecutorRepository extends JpaRepository<Executor, Long> {

	Optional<Executor> findByName(String name);

	boolean existsByName(String name);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT e FROM Executor e WHERE e.id = :id")
	Optional<Executor> findByIdForUpdate(@Param("id") Long id);
}
