package com.team08.backend.domain.content.category.repository;

import com.team08.backend.domain.content.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
