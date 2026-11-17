/**
 * ============================================================================
 * ISOLATION LEVEL TEST (Бонус 3)
 * ============================================================================
 * Мета: порівняти вплив рівня ізоляції транзакцій PostgreSQL
 * (READ COMMITTED / REPEATABLE READ / SERIALIZABLE) на продуктивність.
 *
 * Щоб рівень ізоляції мав значення, потрібна конкуренція: 50 користувачів
 * одночасно редагують ОДНІ Й ТІ САМІ 5 планів (оновлення плану, оновлення
 * локацій, додавання/видалення локацій).
 *
 * Етапи: 30 с наростання -> 2 хв 50 користувачів -> 30 с зниження (3 хв).
 * Тест запускається 3 рази, по одному на кожен рівень ізоляції.
 * ============================================================================
 */

import { sleep } from 'k6';
import { Counter } from 'k6/metrics';
import {
    createTravelPlan,
    getTravelPlan,
    addLocation,
    updateTravelPlan,
    updateLocation,
    deleteLocation,
    deleteTravelPlan,
    thinkTime,
} from '../utils/api-client.js';
import {
    generateTravelPlan,
    generateLocation,
} from '../utils/data-generator.js';

const SHARED_PLANS = 5;

// Outcome counters for concurrent updates
const planUpdateOk = new Counter('plan_update_ok');
const planUpdateConflict = new Counter('plan_update_conflict_409');
const planUpdateError = new Counter('plan_update_error');
const locationUpdateOk = new Counter('location_update_ok');
const locationUpdateConflict = new Counter('location_update_conflict_409');
const locationUpdateError = new Counter('location_update_error');
const locationAddFailed = new Counter('location_add_failed');

export const options = {
    stages: [
        { duration: '30s', target: 50 },
        { duration: '2m', target: 50 },
        { duration: '30s', target: 0 },
    ],
    // No strict thresholds: this test compares numbers between runs
    thresholds: {
        'http_req_duration': ['p(95)<2000'],
    },
    userAgent: 'K6-IsolationTest/1.0',
};

// Create shared plans that all users will edit at the same time
export function setup() {
    const planIds = [];
    for (let i = 0; i < SHARED_PLANS; i++) {
        const plan = createTravelPlan(generateTravelPlan());
        if (plan) {
            for (let j = 0; j < 3; j++) {
                addLocation(plan.id, generateLocation());
            }
            planIds.push(plan.id);
        }
    }
    console.log(`Created ${planIds.length} shared plans for the isolation test`);
    return { planIds: planIds };
}

export default function (data) {
    const planId = data.planIds[Math.floor(Math.random() * data.planIds.length)];

    // 1. Read the shared plan (we get its version and its locations' versions)
    const plan = getTravelPlan(planId);
    if (!plan) {
        sleep(0.5);
        return;
    }
    thinkTime(0.1, 0.3);

    // 2. Update the plan with the version we have read (others may be faster -> 409)
    const updated = updateTravelPlan(planId, {
        title: plan.title,
        budget: parseFloat(((plan.budget || 1000) + 1).toFixed(2)),
        version: plan.version,
    });
    if (updated && !updated.conflict) {
        planUpdateOk.add(1);
    } else if (updated && updated.conflict) {
        planUpdateConflict.add(1);
    } else {
        planUpdateError.add(1);
    }
    thinkTime(0.1, 0.3);

    // 3. Update a random location of this plan
    if (plan.locations && plan.locations.length > 0) {
        const location = plan.locations[Math.floor(Math.random() * plan.locations.length)];
        const updatedLocation = updateLocation(location.id, {
            notes: `Edited by VU ${__VU}`,
            version: location.version,
        });
        if (updatedLocation && !updatedLocation.conflict) {
            locationUpdateOk.add(1);
        } else if (updatedLocation && updatedLocation.conflict) {
            locationUpdateConflict.add(1);
        } else {
            locationUpdateError.add(1);
        }
        thinkTime(0.1, 0.3);
    }

    // 4. Sometimes add a location (auto-ordering under contention) and remove it again
    if (Math.random() < 0.3) {
        const newLocation = addLocation(planId, generateLocation());
        if (newLocation) {
            thinkTime(0.1, 0.3);
            deleteLocation(newLocation.id);
        } else {
            locationAddFailed.add(1);
        }
    }

    thinkTime(0.2, 0.5);
}

export function teardown(data) {
    for (const planId of data.planIds) {
        deleteTravelPlan(planId);
    }
}