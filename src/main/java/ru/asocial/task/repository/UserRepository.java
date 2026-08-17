package ru.asocial.task.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.asocial.task.model.AppUser;

public interface UserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByUsername(String username);
}
