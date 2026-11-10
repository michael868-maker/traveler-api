package com.traveler.traveler_api.repository;

import com.traveler.traveler_api.entity.TravelPlan;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM TravelPlan p WHERE p.id = :id")
    Optional<TravelPlan> findByIdForUpdate(@Param("id") UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE TravelPlan p SET p.title = :title, p.description = :description, " +
            "p.startDate = :startDate, p.endDate = :endDate, p.budget = :budget, " +
            "p.currency = :currency, p.isPublic = :isPublic, p.version = p.version + 1 " +
            "WHERE p.id = :id AND p.version = :version")
    int updateWithVersion(@Param("id") UUID id,
                          @Param("title") String title,
                          @Param("description") String description,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate,
                          @Param("budget") BigDecimal budget,
                          @Param("currency") String currency,
                          @Param("isPublic") Boolean isPublic,
                          @Param("version") Integer version);
}