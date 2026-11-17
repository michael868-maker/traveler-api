/**
 * ============================================================================
 * STRESS TEST (Завдання 3)
 * ============================================================================
 * Мета: знайти межі системи - коли починається деградація, точку відмови
 * (breaking point) та перевірити відновлення після зниження навантаження.
 *
 * Етапи (по 2 хв на кожну сходинку):
 *   0 -> 100 -> 200 -> 400 -> 600 -> 800 -> 1000 користувачів,
 *   далі 1000 -> 50 (2 хв), утримання 50 (3 хв, перевірка відновлення), 50 -> 0.
 *
 * Мінімальний think time (0.1-0.5 с), щоб кожен користувач створював
 * суттєве навантаження. Кожна сходинка позначається тегом level -
 * у підсумку k6 видно час відповіді та помилки окремо для кожної сходинки.
 * ============================================================================
 */

import { sleep } from 'k6';
import exec from 'k6/execution';
import {
    createTravelPlan,
    getTravelPlan,
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

// Test phases: end minute of each phase and its label
const LEVELS = [
    { until: 2, name: '0-100' },
    { until: 4, name: '100-200' },
    { until: 6, name: '200-400' },
    { until: 8, name: '400-600' },
    { until: 10, name: '600-800' },
    { until: 12, name: '800-1000' },
    { until: 14, name: '1000-50' },
    { until: 17, name: 'recovery-50' },
    { until: 18, name: '50-0' },
];

// Same limits for every level, so the summary shows a line for each of them
const levelThresholds = {};
for (const level of LEVELS) {
    levelThresholds[`http_req_duration{level:${level.name}}`] = ['avg<500', 'p(95)<2000'];
    levelThresholds[`http_req_failed{level:${level.name}}`] = ['rate<0.05'];
}

export const options = {
    stages: [
        { duration: '2m', target: 100 },
        { duration: '2m', target: 200 },
        { duration: '2m', target: 400 },
        { duration: '2m', target: 600 },
        { duration: '2m', target: 800 },
        { duration: '2m', target: 1000 },  // critical load
        { duration: '2m', target: 50 },    // load drop
        { duration: '3m', target: 50 },    // recovery check
        { duration: '1m', target: 0 },
    ],

    thresholds: {
        'http_req_duration': ['p(95)<2000'],
        'http_req_failed': ['rate<0.05'],
        ...levelThresholds,
    },

    userAgent: 'K6-StressTest/1.0',
};

// Mark all requests of this iteration with the current level
function markLevel() {
    const minutes = exec.instance.currentTestRunDuration / 60000;
    let name = '50-0';
    for (const level of LEVELS) {
        if (minutes < level.until) {
            name = level.name;
            break;
        }
    }
    exec.vu.metrics.tags.level = name;
}

export default function () {
    markLevel();

    const planData = generateTravelPlan();
    const plan = createTravelPlan(planData);
    if (!plan) {
        sleep(0.5);
        return;
    }
    thinkTime(0.1, 0.3);

    const location = addLocation(plan.id, generateLocation());
    thinkTime(0.1, 0.3);

    const fullPlan = getTravelPlan(plan.id);
    thinkTime(0.1, 0.3);

    if (fullPlan) {
        updateTravelPlan(plan.id, {
            ...planData,
            title: `Stress: ${planData.title}`,
            version: fullPlan.version,
        });
        thinkTime(0.1, 0.3);
    }

    if (location) {
        updateLocation(location.id, {
            notes: 'Updated during stress test',
            version: location.version,
        });
        thinkTime(0.1, 0.3);
    }

    deleteTravelPlan(plan.id);
    thinkTime(0.2, 0.5);
}

export function setup() {
    console.log('='.repeat(80));
    console.log('STRESS TEST: 100 -> 200 -> 400 -> 600 -> 800 -> 1000 users,');
    console.log('then recovery at 50 users (18 min total)');
    console.log('='.repeat(80));
}