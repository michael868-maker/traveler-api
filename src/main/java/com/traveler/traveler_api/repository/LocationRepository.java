package com.traveler.traveler_api.repository;

import com.traveler.traveler_api.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
    @Query("SELECT COALESCE(MAX(l.visitOrder), 0) FROM Location l WHERE l.travelPlan.id = :planId")
    int findMaxVisitOrder(@Param("planId") UUID planId);
}