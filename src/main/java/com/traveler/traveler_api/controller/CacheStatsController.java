package com.traveler.traveler_api.controller;

import com.traveler.traveler_api.cache.TravelPlanCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CacheStatsController {

    private final TravelPlanCache planCache;

    public CacheStatsController(TravelPlanCache planCache){
        this.planCache = planCache;
    }

    @GetMapping("/cache/stats")
    public Map<String, Object> stats(){
        return planCache.stats();
    }
}