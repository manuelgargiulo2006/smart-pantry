package com.pantry.repository;

import com.pantry.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    List<Recipe> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
