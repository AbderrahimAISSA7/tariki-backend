package com.tariki.backend.repository;

import com.tariki.backend.model.Livraison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LivraisonRepository extends JpaRepository<Livraison, Long> {
    @org.springframework.data.jpa.repository.Query(value = "SELECT id FROM livraison WHERE id = :id FOR UPDATE", nativeQuery = true)
    Long lockRow(@org.springframework.data.repository.query.Param("id") Long id);
    boolean existsByReferenceIgnoreCaseAndEntrepriseId(String reference, Long entrepriseId);
    java.util.List<Livraison> findByChauffeurId(Long id);
    java.util.List<Livraison> findByClientId(Long id);
}
