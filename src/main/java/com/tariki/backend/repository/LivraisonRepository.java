package com.tariki.backend.repository;

import com.tariki.backend.model.Livraison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LivraisonRepository extends JpaRepository<Livraison, Long> {
    java.util.List<Livraison> findByChauffeurId(Long id);
    java.util.List<Livraison> findByClientId(Long id);
}
