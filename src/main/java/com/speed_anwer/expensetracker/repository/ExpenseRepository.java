package com.speed_anwer.expensetracker.repository;

import com.speed_anwer.expensetracker.dto.response.CategorySummary;
import com.speed_anwer.expensetracker.entity.Expense;
import com.speed_anwer.expensetracker.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
            SELECT e FROM Expense e
            WHERE e.user = :user
              AND (:categoryId IS NULL OR e.category.id = :categoryId)
              AND (:startDate IS NULL OR e.expenseDate >= :startDate)
              AND (:endDate IS NULL OR e.expenseDate <= :endDate)
            """)
    Page<Expense> findByUserWithFilters(
            @Param("user") User user,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("""
            SELECT new com.speed_anwer.expensetracker.dto.response.CategorySummary(
                       c.id, c.name, COALESCE(SUM(e.amount), 0), COUNT(e))
            FROM Expense e JOIN e.category c
            WHERE e.user = :user
              AND (:startDate IS NULL OR e.expenseDate >= :startDate)
              AND (:endDate IS NULL OR e.expenseDate <= :endDate)
            GROUP BY c.id, c.name
            ORDER BY SUM(e.amount) DESC
            """)
    List<CategorySummary> summarizeByCategory(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0), COUNT(e), COALESCE(AVG(e.amount), 0)
            FROM Expense e
            WHERE e.user = :user
              AND (:startDate IS NULL OR e.expenseDate >= :startDate)
              AND (:endDate IS NULL OR e.expenseDate <= :endDate)
            """)
    List<Object[]> aggregateTotals(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<Expense> findByIdAndUser(Long id, User user);
}
