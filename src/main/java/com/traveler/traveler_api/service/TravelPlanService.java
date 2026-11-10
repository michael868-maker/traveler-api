package com.traveler.traveler_api.service;

import com.traveler.traveler_api.dto.TravelPlanCreateRequest;
import com.traveler.traveler_api.dto.TravelPlanResponse;
import com.traveler.traveler_api.dto.TravelPlanUpdateRequest;
import com.traveler.traveler_api.entity.TravelPlan;
import com.traveler.traveler_api.exception.BadRequestException;
import com.traveler.traveler_api.exception.ConflictException;
import com.traveler.traveler_api.exception.NotFoundException;
import com.traveler.traveler_api.repository.TravelPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TravelPlanService {
    private final TravelPlanRepository planRepository;

    public TravelPlanService(TravelPlanRepository planRepository){
        this.planRepository = planRepository;
    }

    @Transactional(readOnly = true)
    public List<TravelPlanResponse> findAll(){
        List<TravelPlanResponse> result = new ArrayList<>();
        for (TravelPlan plan : planRepository.findAll()) {
            result.add(TravelPlanResponse.from(plan));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public TravelPlanResponse findById(UUID id){
        return TravelPlanResponse.from(getPlanOrThrow(id));
    }

    @Transactional
    public TravelPlanResponse create(TravelPlanCreateRequest req){
        checkDates(req.startDate(), req.endDate());

        TravelPlan plan = new TravelPlan();
        plan.setTitle(req.title().trim());
        plan.setDescription(req.description());
        plan.setStartDate(req.startDate());
        plan.setEndDate(req.endDate());
        plan.setBudget(req.budget());
        plan.setCurrency(req.currency());
        plan.setIsPublic(req.isPublic());

        TravelPlan saved = planRepository.save(plan);
        return TravelPlanResponse.from(saved);
    }

    @Transactional
    public TravelPlanResponse update(UUID id, TravelPlanUpdateRequest req){
        TravelPlan current = getPlanOrThrow(id);

        String title = current.getTitle();
        if (req.title() != null) {
            title = req.title().trim();
        }
        String description = valueOrDefault(req.description(), current.getDescription());
        LocalDate startDate = valueOrDefault(req.startDate(), current.getStartDate());
        LocalDate endDate = valueOrDefault(req.endDate(), current.getEndDate());
        BigDecimal budget = valueOrDefault(req.budget(), current.getBudget());
        String currency = valueOrDefault(req.currency(), current.getCurrency());
        Boolean isPublic = valueOrDefault(req.isPublic(), current.getIsPublic());

        checkDates(startDate, endDate);

        int updatedRows = planRepository.updateWithVersion(id, title, description, startDate,
                endDate, budget, currency, isPublic, req.version());

        if (updatedRows == 0) {
            Integer currentVersion = null;
            Optional<TravelPlan> fresh = planRepository.findById(id);
            if (fresh.isPresent()) {
                currentVersion = fresh.get().getVersion();
            }
            throw new ConflictException("Conflict: travel plan was modified by another user", currentVersion);
        }

        return TravelPlanResponse.from(getPlanOrThrow(id));
    }

    @Transactional
    public void delete(UUID id){
        if (!planRepository.existsById(id)) {
            throw new NotFoundException("Travel plan not found");
        }
        planRepository.deleteById(id);
    }

    private TravelPlan getPlanOrThrow(UUID id){
        Optional<TravelPlan> plan = planRepository.findById(id);
        if (plan.isEmpty()) {
            throw new NotFoundException("Travel plan not found");
        }
        return plan.get();
    }

    private void checkDates(LocalDate startDate, LocalDate endDate){
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BadRequestException("Validation error: end_date must be on or after start_date");
        }
    }

    private static <T> T valueOrDefault(T value, T fallback){
        if (value != null) {
            return value;
        }
        return fallback;
    }
}