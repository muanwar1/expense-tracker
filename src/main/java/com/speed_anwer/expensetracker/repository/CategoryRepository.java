package com.speed_anwer.expensetracker.repository;

import com.speed_anwer.expensetracker.entity.Category;
import com.speed_anwer.expensetracker.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Page<Category> findByUser(User user, Pageable pageable);

    Optional<Category> findByNameAndUser(String name, User user);

    Optional<Category> findByIdAndUser(Long id, User user);

    Optional<Category> findByNameAndUserAndIdNot(
            String name,
            User user,
            Long id
    );

}