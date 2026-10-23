package com.pantry.repository;

import com.pantry.model.PantryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {

    List<PantryItem> findByUserIdOrderByExpirationDateAsc(Long userId);

    List<PantryItem> findByUserIdAndProductIdOrderByExpirationDateAsc(Long userId, Long productId);

    /** Cerca un elemento solo tra quelli dell'utente: impedisce di modificare la dispensa di altri. */
    Optional<PantryItem> findByIdAndUserId(Long id, Long userId);

    /** Prodotti con scadenza entro la data indicata (compresi quelli già scaduti). */
    List<PantryItem> findByUserIdAndExpirationDateLessThanEqualOrderByExpirationDateAsc(Long userId, LocalDate date);

    @Query("select coalesce(sum(p.quantity), 0) from PantryItem p where p.user.id = :userId and p.product.id = :productId")
    long sumQuantity(@Param("userId") Long userId, @Param("productId") Long productId);

    boolean existsByProductId(Long productId);
}
