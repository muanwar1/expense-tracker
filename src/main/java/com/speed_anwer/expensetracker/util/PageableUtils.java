package com.speed_anwer.expensetracker.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageableUtils {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 10;

    private PageableUtils() {
    }

    public static Pageable of(int page, int size, String sortBy, String sortDir, Set<String> allowedSortFields) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        String safeSortBy = (sortBy != null && allowedSortFields.contains(sortBy)) ? sortBy : null;
        Sort sort = safeSortBy == null
                ? Sort.unsorted()
                : Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, safeSortBy);

        return PageRequest.of(safePage, safeSize, sort);
    }
}
