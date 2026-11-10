package com.traveler.traveler_api.dto;

import com.traveler.traveler_api.entity.Location;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LocationResponse(
        UUID id,
        UUID travelPlanId,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer visitOrder,
        OffsetDateTime arrivalDate,
        OffsetDateTime departureDate,
        BigDecimal budget,
        String notes,
        OffsetDateTime createdAt
) {

    public static LocationResponse from(Location location){
        return new LocationResponse(
                location.getId(),
                location.getTravelPlan().getId(),
                location.getName(),
                location.getAddress(),
                location.getLatitude(),
                location.getLongitude(),
                location.getVisitOrder(),
                location.getArrivalDate(),
                location.getDepartureDate(),
                location.getBudget(),
                location.getNotes(),
                location.getCreatedAt()
        );
    }
}