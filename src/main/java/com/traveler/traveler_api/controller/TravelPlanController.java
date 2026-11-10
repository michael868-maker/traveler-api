package com.traveler.traveler_api.controller;

import com.traveler.traveler_api.dto.TravelPlanCreateRequest;
import com.traveler.traveler_api.dto.TravelPlanResponse;
import com.traveler.traveler_api.dto.TravelPlanUpdateRequest;
import com.traveler.traveler_api.service.TravelPlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/travel-plans")
public class TravelPlanController {
    private final TravelPlanService planService;

    public TravelPlanController(TravelPlanService planService){
        this.planService = planService;
    }

    @GetMapping
    public List<TravelPlanResponse> getAll(){
        return planService.findAll();
    }

    @PostMapping
    public ResponseEntity<TravelPlanResponse> create(@Valid @RequestBody TravelPlanCreateRequest req){
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.create(req));
    }

    @GetMapping("/{id}")
    public TravelPlanResponse getById(@PathVariable("id") UUID id){
        return planService.findById(id);
    }

    @PutMapping("/{id}")
    public TravelPlanResponse update(@PathVariable("id") UUID id,
                                     @Valid @RequestBody TravelPlanUpdateRequest req){
        return planService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id){
        planService.delete(id);
        return ResponseEntity.noContent().build();
    }
}