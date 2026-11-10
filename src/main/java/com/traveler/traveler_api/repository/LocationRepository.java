package com.traveler.traveler_api.repository;

import com.traveler.traveler_api.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
    @Query("SELECT COALESCE(MAX(l.visitOrder), 0) FROM Location l WHERE l.travelPlan.id = :planId")
    int findMaxVisitOrder(@Param("planId") UUID planId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Location l SET l.name = :name, l.address = :address, " +
            "l.latitude = :latitude, l.longitude = :longitude, l.visitOrder = :visitOrder, " +
            "l.arrivalDate = :arrivalDate, l.departureDate = :departureDate, " +
            "l.budget = :budget, l.notes = :notes, l.version = l.version + 1 " +
            "WHERE l.id = :id AND l.version = :version")
    int updateWithVersion(@Param("id") UUID id,
                          @Param("name") String name,
                          @Param("address") String address,
                          @Param("latitude") BigDecimal latitude,
                          @Param("longitude") BigDecimal longitude,
                          @Param("visitOrder") Integer visitOrder,
                          @Param("arrivalDate") OffsetDateTime arrivalDate,
                          @Param("departureDate") OffsetDateTime departureDate,
                          @Param("budget") BigDecimal budget,
                          @Param("notes") String notes,
                          @Param("version") Integer version);
}