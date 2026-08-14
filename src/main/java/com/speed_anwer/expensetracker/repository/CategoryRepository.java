package com.speed_anwer.expensetracker.repository;

import com.speed_anwer.expensetracker.entity.Category;
import com.speed_anwer.expensetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUser(User user);

    Optional<Category> findByNameAndUser(String name, User user);

    boolean existsByNameAndUser(String name, User user);

    Optional<Category> findByIdAndUser(Long id, User user);

    Optional<Category> findByNameAndUserAndIdNot(
            String name,
            User user,
            Long id
    );

}