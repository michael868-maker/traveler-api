package com.traveler.traveler_api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TravelPlanCreateRequest(
        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        String description,

        LocalDate startDate,

        LocalDate endDate,

        @DecimalMin(value = "0.00", message = "budget must be >= 0")
        @Digits(integer = 8, fraction = 2, message = "budget must have at most 2 decimal places")
        BigDecimal budget,

        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be 3 uppercase letters")
        String currency,

        Boolean isPublic
) {
}