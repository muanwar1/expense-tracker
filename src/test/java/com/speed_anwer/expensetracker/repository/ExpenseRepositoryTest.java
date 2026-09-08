package com.speed_anwer.expensetracker.repository;

import com.speed_anwer.expensetracker.dto.response.CategorySummary;
import com.speed_anwer.expensetracker.entity.Category;
import com.speed_anwer.expensetracker.entity.Expense;
import com.speed_anwer.expensetracker.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ExpenseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ExpenseRepository expenseRepository;

    private User userA;
    private User userB;
    private Category food;
    private Category transport;

    @BeforeEach
    void setUp() {
        userA = entityManager.persist(newUserData("a@test.com"));
        userB = entityManager.persist(newUserData("b@test.com"));
        food = entityManager.persist(category("Food", userA));
        transport = entityManager.persist(category("Transport", userA));

        persistExpense(userA, food, "10.00", LocalDate.of(2026, 8, 1));
        persistExpense(userA, food, "20.00", LocalDate.of(2026, 8, 15));
        persistExpense(userA, transport, "30.00", LocalDate.of(2026, 8, 20));
        persistExpense(userA, transport, "40.00", LocalDate.of(2026, 8, 22));
        // another user's expense must never leak into queries of user A
        Category bCategory = entityManager.persist(category("OtherB", userB));
        persistExpense(userB, bCategory, "999.00", LocalDate.of(2026, 8, 2));
        entityManager.flush();
        entityManager.clear();
    }

    private User newUserData(String email) {
        User user = new User();
        user.setFullName("User " + email);
        user.setEmail(email);
        user.setPassword("hashed");
        return user;
    }

    private Category category(String name, User user) {
        Category category = new Category();
        category.setName(name);
        category.setUser(user);
        return category;
    }

    private void persistExpense(User user, Category category, String amount, LocalDate date) {
        Expense expense = new Expense();
        expense.setTitle("t-" + amount);
        expense.setAmount(new BigDecimal(amount));
        expense.setExpenseDate(date);
        expense.setUser(user);
        if (category != null) {
            expense.setCategory(category);
        }
        entityManager.persist(expense);
    }

    @Test
    void filters_noFilters_returnsOnlyOwnExpenses() {
        Page<Expense> page = expenseRepository.findByUserWithFilters(
                userA, null, null, null, PageRequest.of(0, 100));

        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getContent()).allSatisfy(e -> assertThat(e.getUser().getId()).isEqualTo(userA.getId()));
    }

    @Test
    void filters_byCategory() {
        Page<Expense> page = expenseRepository.findByUserWithFilters(
                userA, food.getId(), null, null, PageRequest.of(0, 100));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void filters_byDateRange() {
        Page<Expense> page = expenseRepository.findByUserWithFilters(
                userA, null,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                PageRequest.of(0, 100));

        assertThat(page.getTotalElements()).isEqualTo(4);
    }

    @Test
    void filters_categoryAndRangeCombined() {
        Page<Expense> page = expenseRepository.findByUserWithFilters(
                userA, transport.getId(),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                PageRequest.of(0, 100));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void summarizeByCategory_sumsPerCategoryOrderedDesc() {
        List<CategorySummary> summaries = expenseRepository.summarizeByCategory(userA, null, null);

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).categoryName()).isEqualTo("Transport");
        assertThat(summaries.get(0).totalAmount()).isEqualByComparingTo("70.00");
        assertThat(summaries.get(0).expenseCount()).isEqualTo(2);
        assertThat(summaries.get(1).categoryName()).isEqualTo("Food");
        assertThat(summaries.get(1).totalAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void summarizeByCategory_respectsDateRange() {
        List<CategorySummary> summaries = expenseRepository.summarizeByCategory(
                userA, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).categoryName()).isEqualTo("Transport");
        assertThat(summaries.get(0).totalAmount()).isEqualByComparingTo("70.00");
        assertThat(summaries.get(1).categoryName()).isEqualTo("Food");
        assertThat(summaries.get(1).totalAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void aggregateTotals_sumsAllOwnedExpenses() {
        List<Object[]> rows = expenseRepository.aggregateTotals(userA, null, null);

        Object[] totals = rows.get(0);
        assertThat(new java.math.BigDecimal(totals[0].toString())).isEqualByComparingTo("100.00");
        assertThat(((Number) totals[1]).longValue()).isEqualTo(4L);
        assertThat(new java.math.BigDecimal(totals[2].toString())).isEqualByComparingTo("25.00");
    }
}
