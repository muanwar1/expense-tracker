package com.speed_anwer.expensetracker.repository;

import com.speed_anwer.expensetracker.entity.Category;
import com.speed_anwer.expensetracker.entity.Expense;
import com.speed_anwer.expensetracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUser(User user);
    List<Expense> findByUserAndCategory(User user, Category category);
    List<Expense> findByUserAndExpenseDate(User user, LocalDate expenseDate);
    List<Expense> findByUserAndExpenseDateBetween(
            User user,
            LocalDate startDate,
            LocalDate endDate
    );
    Optional<Expense> findByIdAndUser(Long id, User user);

}