package com.pantry.repository;

import com.pantry.model.MovementType;
import com.pantry.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("""
            select coalesce(sum(m.quantity), 0) from StockMovement m
            where m.user.id = :userId and m.type = :type and m.createdAt >= :since
            """)
    long sumQuantitySince(@Param("userId") Long userId,
                         @Param("type") MovementType type,
                         @Param("since") LocalDateTime since);

    List<StockMovement> findTop8ByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByProductId(Long productId);
}
