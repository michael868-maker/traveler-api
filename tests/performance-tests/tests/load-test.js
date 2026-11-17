/**
 * ============================================================================
 * LOAD TEST (Завдання 2)
 * ============================================================================
 * Мета: перевірити поведінку API під очікуваним навантаженням.
 * Етапи (з тексту завдання):
 *   5 хв  - поступове наростання до 100 користувачів
 *   10 хв - утримання 100 користувачів (ПІК)
 *   5 хв  - зниження до 0
 *
 * Кожна ітерація - типовий користувач:
 *   іноді переглядає список планів -> створює план -> додає 2 локації ->
 *   читає план -> оновлює план (version) -> оновлює локацію (version) ->
 *   ще раз читає план -> видаляє план (cleanup)
 *
 * Запити під час піку позначаються тегом phase:peak, щоб окремо отримати
 * середній час відповіді та рівень помилок саме при піковому навантаженні.
 * ============================================================================
 */

import { sleep } from 'k6';
import exec from 'k6/execution';
import { DEFAULT_THRESHOLDS } from '../config/endpoints.js';
import {
    createTravelPlan,
    getTravelPlan,
    listTravelPlans,
    addLocation,
    updateTravelPlan,
    updateLocation,
    deleteTravelPlan,
    thinkTime,
} from '../utils/api-client.js';
import {
    generateTravelPlan,
    generateLocation,
} from '../utils/data-generator.js';

const RAMP_UP_MS = 5 * 60 * 1000;   // 5 min
const PEAK_END_MS = 15 * 60 * 1000; // 5 + 10 min

export const options = {
    stages: [
        { duration: '5m', target: 100 },  // ramp-up
        { duration: '10m', target: 100 }, // peak: 100 users
        { duration: '5m', target: 0 },    // ramp-down
    ],

    thresholds: {
        ...DEFAULT_THRESHOLDS,
        // Separate metrics for the peak phase (shown in the summary as own lines)
        'http_req_duration{phase:peak}': ['avg<500', 'p(95)<1000'],
        'http_req_failed{phase:peak}': ['rate<0.01'],
    },

    userAgent: 'K6-LoadTest/1.0',
};

// Mark all requests of this iteration with the current test phase
function markPhase() {
    const elapsed = exec.instance.currentTestRunDuration;
    if (elapsed >= RAMP_UP_MS && elapsed < PEAK_END_MS) {
        exec.vu.metrics.tags.phase = 'peak';
    } else {
        exec.vu.metrics.tags.phase = 'ramp';
    }
}

export default function () {
    markPhase();

    // 1. Sometimes the user looks at the list of plans first (20%)
    if (Math.random() < 0.2) {
        listTravelPlans();
        thinkTime(1, 2);
    }

    // 2. Create a plan
    const planData = generateTravelPlan();
    const plan = createTravelPlan(planData);
    if (!plan) {
        sleep(1);
        return;
    }
    thinkTime(1, 2);

    // 3. Add two locations (auto-ordering)
    const location1 = addLocation(plan.id, generateLocation());
    thinkTime(0.5, 1.5);
    addLocation(plan.id, generateLocation());
    thinkTime(1, 2);

    // 4. Read the plan with locations
    const fullPlan = getTravelPlan(plan.id);
    thinkTime(1, 3);

    // 5. Update the plan (optimistic locking: send the version we have read)
    if (fullPlan) {
        updateTravelPlan(plan.id, {
            ...planData,
            title: `Updated: ${planData.title}`,
            version: fullPlan.version,
        });
        thinkTime(1, 2);
    }

    // 6. Update a location (optimistic locking for locations, lab 1)
    if (location1) {
        updateLocation(location1.id, {
            notes: 'Updated during load test',
            budget: 99.99,
            version: location1.version,
        });
        thinkTime(1, 2);
    }

    // 7. Final check and cleanup
    getTravelPlan(plan.id);
    thinkTime(0.5, 1);
    deleteTravelPlan(plan.id);

    sleep(1);
}

export function setup() {
    console.log('='.repeat(80));
    console.log('LOAD TEST: 5m ramp-up -> 10m at 100 users -> 5m ramp-down (20 min)');
    console.log('='.repeat(80));
}