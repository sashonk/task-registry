package ru.asocial.task.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.asocial.task.model.Executor;

public interface ExecutorRepository extends JpaRepository<Executor, Long> {

	Optional<Executor> findByName(String name);

	boolean existsByName(String name);
}
