package com.pantry.repository;

import com.pantry.model.StockThreshold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockThresholdRepository extends JpaRepository<StockThreshold, Long> {

    List<StockThreshold> findByUserIdOrderByProductNameAsc(Long userId);

    Optional<StockThreshold> findByUserIdAndProductId(Long userId, Long productId);

    Optional<StockThreshold> findByIdAndUserId(Long id, Long userId);

    boolean existsByProductId(Long productId);
}
