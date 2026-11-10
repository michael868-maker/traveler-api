package com.traveler.traveler_api.controller;

import com.traveler.traveler_api.dto.LocationCreateRequest;
import com.traveler.traveler_api.dto.LocationResponse;
import com.traveler.traveler_api.dto.LocationUpdateRequest;
import com.traveler.traveler_api.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService){
        this.locationService = locationService;
    }

    @PostMapping("/api/travel-plans/{id}/locations")
    public ResponseEntity<LocationResponse> create(@PathVariable("id") UUID planId,
                                                   @Valid @RequestBody LocationCreateRequest req){
        return ResponseEntity.status(HttpStatus.CREATED).body(locationService.create(planId, req));
    }

    @PutMapping("/api/locations/{id}")
    public LocationResponse update(@PathVariable("id") UUID id,
                                   @Valid @RequestBody LocationUpdateRequest req){
        return locationService.update(id, req);
    }

    @DeleteMapping("/api/locations/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id){
        locationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}