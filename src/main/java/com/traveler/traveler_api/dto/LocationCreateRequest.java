package com.traveler.traveler_api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LocationCreateRequest(
        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name must be at most 200 characters")
        String name,

        String address,

        @DecimalMin(value = "-90", message = "latitude must be between -90 and 90")
        @DecimalMax(value = "90", message = "latitude must be between -90 and 90")
        BigDecimal latitude,

        @DecimalMin(value = "-180", message = "longitude must be between -180 and 180")
        @DecimalMax(value = "180", message = "longitude must be between -180 and 180")
        BigDecimal longitude,

        OffsetDateTime arrivalDate,

        OffsetDateTime departureDate,

        @DecimalMin(value = "0.00", message = "budget must be >= 0")
        @Digits(integer = 8, fraction = 2, message = "budget must have at most 2 decimal places")
        BigDecimal budget,

        String notes
) {
}