package com.tariki.backend.repository;

import com.tariki.backend.model.LivePosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface LivePositionRepository extends JpaRepository<LivePosition, Long> {
    @Modifying
    @Query("delete from LivePosition p where p.observedAt is null and p.updatedAt < :cutoff")
    int deleteEmptySessions(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("update LivePosition p set p.sharingActive = false where p.livraisonId = :id")
    void stopForDelivery(@Param("id") Long id);
}
