package com.traveler.traveler_api.dto;

import com.traveler.traveler_api.entity.Location;
import com.traveler.traveler_api.entity.TravelPlan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record TravelPlanResponse(
        UUID id,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budget,
        String currency,
        Boolean isPublic,
        Integer version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<LocationResponse> locations
) {

    public static TravelPlanResponse from(TravelPlan plan){
        List<LocationResponse> locations = new ArrayList<>();
        for (Location location : plan.getLocations()) {
            locations.add(LocationResponse.from(location));
        }
        return new TravelPlanResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getDescription(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getBudget(),
                plan.getCurrency(),
                plan.getIsPublic(),
                plan.getVersion(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                locations
        );
    }
}