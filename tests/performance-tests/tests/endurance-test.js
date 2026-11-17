/**
 * ============================================================================
 * ENDURANCE / SOAK TEST (Завдання 5)
 * ============================================================================
 * Мета: перевірити стабільність системи під помірним навантаженням
 * протягом тривалого часу, виявити деградацію продуктивності та витоки пам'яті.
 *
 * Етапи:
 *   5 хв   0 -> 100       (наростання)
 *   30 хв  100            (тривале навантаження, поділене на 6 відрізків по 5 хв)
 *   5 хв   100 -> 0       (зниження)
 *
 * Кожен 5-хвилинний відрізок позначається тегом window, тож у підсумку k6
 * видно, чи змінюється час відповіді з часом.
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

// Time windows: end minute of each window and its label
const WINDOWS = [
    { until: 5, name: '0-ramp-up' },
    { until: 10, name: '1-hold-00-05m' },
    { until: 15, name: '2-hold-05-10m' },
    { until: 20, name: '3-hold-10-15m' },
    { until: 25, name: '4-hold-15-20m' },
    { until: 30, name: '5-hold-20-25m' },
    { until: 35, name: '6-hold-25-30m' },
    { until: 40, name: '7-ramp-down' },
];

// Same limits for every window, so the summary shows a line for each of them
const windowThresholds = {};
for (const window of WINDOWS) {
    windowThresholds[`http_req_duration{window:${window.name}}`] = ['avg<500', 'p(95)<1000'];
    windowThresholds[`http_req_failed{window:${window.name}}`] = ['rate<0.01'];
}

export const options = {
    stages: [
        { duration: '5m', target: 100 },   // ramp-up
        { duration: '30m', target: 100 },  // long steady load (8h in real life)
        { duration: '5m', target: 0 },     // ramp-down
    ],

    thresholds: {
        ...DEFAULT_THRESHOLDS,
        ...windowThresholds,
    },

    userAgent: 'K6-EnduranceTest/1.0',
};

// Mark all requests of this iteration with the current time window
function markWindow() {
    const minutes = exec.instance.currentTestRunDuration / 60000;
    let name = '7-ramp-down';
    for (const window of WINDOWS) {
        if (minutes < window.until) {
            name = window.name;
            break;
        }
    }
    exec.vu.metrics.tags.window = name;
}

export default function () {
    markWindow();

    // Sometimes the user looks at the list of plans first (20%)
    if (Math.random() < 0.2) {
        listTravelPlans();
        thinkTime(1, 2);
    }

    const planData = generateTravelPlan();
    const plan = createTravelPlan(planData);
    if (!plan) {
        sleep(1);
        return;
    }
    thinkTime(1, 2);

    const location1 = addLocation(plan.id, generateLocation());
    thinkTime(0.5, 1.5);
    addLocation(plan.id, generateLocation());
    thinkTime(1, 2);

    const fullPlan = getTravelPlan(plan.id);
    thinkTime(1, 3);

    if (fullPlan) {
        updateTravelPlan(plan.id, {
            ...planData,
            title: `Endurance: ${planData.title}`,
            version: fullPlan.version,
        });
        thinkTime(1, 2);
    }

    if (location1) {
        updateLocation(location1.id, {
            notes: 'Updated during endurance test',
            version: location1.version,
        });
        thinkTime(1, 2);
    }

    getTravelPlan(plan.id);
    thinkTime(0.5, 1);
    deleteTravelPlan(plan.id);

    sleep(1);
}

export function setup() {
    console.log('='.repeat(80));
    console.log('ENDURANCE TEST: 5m ramp-up -> 30m at 100 users -> 5m ramp-down (40 min)');
    console.log('='.repeat(80));
}