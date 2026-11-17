/**
 * ============================================================================
 * CACHE BENCHMARK (Бонус 4)
 * ============================================================================
 * Мета: порівняти продуктивність читання планів без кешу та з LRU кешем.
 *
 * Підготовка: створюється 200 планів по 3 локації.
 * 200 користувачів з мінімальними паузами:
 *   90% - читають випадковий план (GET /api/travel-plans/{id}),
 *   10% - оновлюють план (кеш для нього інвалідується).
 * Етапи: 30 с наростання -> 2 хв 200 користувачів -> 30 с зниження (3 хв).
 * Тест запускається двічі: з app.cache.plans.enabled=false та =true.
 * ============================================================================
 */

import {
    createTravelPlan,
    getTravelPlan,
    addLocation,
    updateTravelPlan,
    deleteTravelPlan,
    thinkTime,
} from '../utils/api-client.js';
import {
    generateTravelPlan,
    generateLocation,
} from '../utils/data-generator.js';

const PLANS_COUNT = 200;

export const options = {
    stages: [
        { duration: '30s', target: 200 },
        { duration: '2m', target: 200 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        'http_req_duration{type:read}': ['avg<1000', 'p(95)<2000'],
        'http_req_duration{type:write}': ['avg<1000', 'p(95)<2000'],
    },
    userAgent: 'K6-CacheBenchmark/1.0',
};

export function setup() {
    const planIds = [];
    for (let i = 0; i < PLANS_COUNT; i++) {
        const plan = createTravelPlan(generateTravelPlan());
        if (plan) {
            for (let j = 0; j < 3; j++) {
                addLocation(plan.id, generateLocation());
            }
            planIds.push(plan.id);
        }
    }
    console.log(`Created ${planIds.length} plans for the cache benchmark`);
    return { planIds: planIds };
}

export default function (data) {
    const planId = data.planIds[Math.floor(Math.random() * data.planIds.length)];
    const plan = getTravelPlan(planId);

    // 10% of iterations update the plan -> cache invalidation
    if (plan && Math.random() < 0.1) {
        updateTravelPlan(planId, {
            title: plan.title,
            budget: parseFloat(((plan.budget || 1000) + 1).toFixed(2)),
            version: plan.version,
        });
    }

    thinkTime(0.05, 0.15);
}

export function teardown(data) {
    for (const planId of data.planIds) {
        deleteTravelPlan(planId);
    }
}