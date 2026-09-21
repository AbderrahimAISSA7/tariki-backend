package com.tariki.backend.repository;

import com.tariki.backend.model.CamionEntretien;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CamionEntretienRepository extends JpaRepository<CamionEntretien, Long> {
    List<CamionEntretien> findByCamionIdOrderByIdDesc(Long camionId);
    List<CamionEntretien> findByCamionIdInAndEffectueLeIsNull(List<Long> camionIds);
    boolean existsByCamionId(Long camionId);
}
