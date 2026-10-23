package com.pantry.repository;

import com.pantry.model.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShoppingListRepository extends JpaRepository<ShoppingListItem, Long> {

    List<ShoppingListItem> findByUserIdAndBoughtFalseOrderByProductCategoryNameAscProductNameAsc(Long userId);

    List<ShoppingListItem> findByUserIdAndBoughtTrueOrderByBoughtAtDesc(Long userId);

    Optional<ShoppingListItem> findFirstByUserIdAndProductIdAndBoughtFalse(Long userId, Long productId);

    Optional<ShoppingListItem> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndBoughtFalse(Long userId);

    boolean existsByProductId(Long productId);

    @Modifying
    @Query("delete from ShoppingListItem s where s.user.id = :userId and s.bought = true")
    int deleteBought(@Param("userId") Long userId);
}
