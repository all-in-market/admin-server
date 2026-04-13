package com.example.allinmarket.domain.category.repository;

import com.example.allinmarket.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByIdAndDeletedAtIsNull(Long id);
    List<Category> findAllAndDeletedAtIsNull();
}
