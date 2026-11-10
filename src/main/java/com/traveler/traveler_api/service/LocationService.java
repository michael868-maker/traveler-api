package com.traveler.traveler_api.service;

import com.traveler.traveler_api.dto.LocationCreateRequest;
import com.traveler.traveler_api.dto.LocationResponse;
import com.traveler.traveler_api.dto.LocationUpdateRequest;
import com.traveler.traveler_api.entity.Location;
import com.traveler.traveler_api.entity.TravelPlan;
import com.traveler.traveler_api.exception.BadRequestException;
import com.traveler.traveler_api.exception.ConflictException;
import java.math.BigDecimal;
import com.traveler.traveler_api.exception.NotFoundException;
import com.traveler.traveler_api.repository.LocationRepository;
import com.traveler.traveler_api.repository.TravelPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class LocationService {
    private final LocationRepository locationRepository;
    private final TravelPlanRepository planRepository;

    public LocationService(LocationRepository locationRepository, TravelPlanRepository planRepository){
        this.locationRepository = locationRepository;
        this.planRepository = planRepository;
    }

    @Transactional
    public LocationResponse create(UUID planId, LocationCreateRequest req){
        checkTimes(req.arrivalDate(), req.departureDate());

        Optional<TravelPlan> planOptional = planRepository.findByIdForUpdate(planId);
        if (planOptional.isEmpty()) {
            throw new NotFoundException("Travel plan not found");
        }
        TravelPlan plan = planOptional.get();

        int nextOrder = locationRepository.findMaxVisitOrder(planId) + 1;

        Location location = new Location();
        location.setTravelPlan(plan);
        location.setName(req.name().trim());
        location.setAddress(req.address());
        location.setLatitude(req.latitude());
        location.setLongitude(req.longitude());
        location.setVisitOrder(nextOrder);
        location.setArrivalDate(req.arrivalDate());
        location.setDepartureDate(req.departureDate());
        location.setBudget(req.budget());
        location.setNotes(req.notes());

        Location saved = locationRepository.save(location);
        return LocationResponse.from(saved);
    }

    @Transactional
    public LocationResponse update(UUID id, LocationUpdateRequest req){
        Location current = getLocationOrThrow(id);

        String name = current.getName();
        if (req.name() != null) {
            name = req.name().trim();
        }
        String address = valueOrDefault(req.address(), current.getAddress());
        BigDecimal latitude = valueOrDefault(req.latitude(), current.getLatitude());
        BigDecimal longitude = valueOrDefault(req.longitude(), current.getLongitude());
        Integer visitOrder = valueOrDefault(req.visitOrder(), current.getVisitOrder());
        OffsetDateTime arrival = valueOrDefault(req.arrivalDate(), current.getArrivalDate());
        OffsetDateTime departure = valueOrDefault(req.departureDate(), current.getDepartureDate());
        BigDecimal budget = valueOrDefault(req.budget(), current.getBudget());
        String notes = valueOrDefault(req.notes(), current.getNotes());

        checkTimes(arrival, departure);

        int updatedRows = locationRepository.updateWithVersion(id, name, address, latitude,
                longitude, visitOrder, arrival, departure, budget, notes, req.version());

        if (updatedRows == 0) {
            Integer currentVersion = null;
            Optional<Location> fresh = locationRepository.findById(id);
            if (fresh.isPresent()) {
                currentVersion = fresh.get().getVersion();
            }
            throw new ConflictException("Conflict: location was modified by another user", currentVersion);
        }

        return LocationResponse.from(getLocationOrThrow(id));
    }

    @Transactional
    public void delete(UUID id){
        if (!locationRepository.existsById(id)) {
            throw new NotFoundException("Location not found");
        }
        locationRepository.deleteById(id);
    }

    private Location getLocationOrThrow(UUID id){
        Optional<Location> location = locationRepository.findById(id);
        if (location.isEmpty()) {
            throw new NotFoundException("Location not found");
        }
        return location.get();
    }

    private void checkTimes(OffsetDateTime arrival, OffsetDateTime departure){
        if (arrival != null && departure != null && departure.isBefore(arrival)) {
            throw new BadRequestException("Validation error: departure_date must be on or after arrival_date");
        }
    }

    private static <T> T valueOrDefault(T value, T fallback){
        if (value != null) {
            return value;
        }
        return fallback;
    }
}