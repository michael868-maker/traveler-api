package com.traveler.traveler_api.service;

import com.traveler.traveler_api.dto.LocationCreateRequest;
import com.traveler.traveler_api.dto.LocationResponse;
import com.traveler.traveler_api.dto.LocationUpdateRequest;
import com.traveler.traveler_api.entity.Location;
import com.traveler.traveler_api.entity.TravelPlan;
import com.traveler.traveler_api.exception.BadRequestException;
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
        Location location = getLocationOrThrow(id);

        OffsetDateTime arrival = location.getArrivalDate();
        if (req.arrivalDate() != null) {
            arrival = req.arrivalDate();
        }
        OffsetDateTime departure = location.getDepartureDate();
        if (req.departureDate() != null) {
            departure = req.departureDate();
        }
        checkTimes(arrival, departure);

        if (req.name() != null) {
            location.setName(req.name().trim());
        }
        if (req.address() != null) {
            location.setAddress(req.address());
        }
        if (req.latitude() != null) {
            location.setLatitude(req.latitude());
        }
        if (req.longitude() != null) {
            location.setLongitude(req.longitude());
        }
        if (req.visitOrder() != null) {
            location.setVisitOrder(req.visitOrder());
        }
        if (req.budget() != null) {
            location.setBudget(req.budget());
        }
        if (req.notes() != null) {
            location.setNotes(req.notes());
        }
        location.setArrivalDate(arrival);
        location.setDepartureDate(departure);

        Location saved = locationRepository.saveAndFlush(location);
        return LocationResponse.from(saved);
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
}