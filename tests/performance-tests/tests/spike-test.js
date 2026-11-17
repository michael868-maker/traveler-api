/**
 * ============================================================================
 * SPIKE TEST (Завдання 4)
 * ============================================================================
 * Мета: перевірити реакцію системи на раптовий сплеск навантаження,
 * чи втрачаються запити, та час відновлення після сплеску.
 *
 * Етапи:
 *   1 хв   0 -> 100          (розігрів)
 *   2 хв   100               (нормальний рівень, базова лінія)
 *   30 с   100 -> 2000       (різкий сплеск)
 *   1 хв   2000              (пік сплеску)
 *   30 с   2000 -> 100       (різке зниження)
 *   3 хв   100               (відновлення, поділене на відрізки)
 *   30 с   100 -> 0
 *
 * Кожен відрізок позначається тегом phase, тож у підсумку k6 видно
 * час відповіді та помилки окремо для базової лінії, сплеску
 * та кожного відрізку відновлення.
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

// Test phases: end second of each phase and its label
const PHASES = [
    { until: 60, name: '1-warmup' },
    { until: 180, name: '2-baseline-100' },
    { until: 210, name: '3-spike-up' },
    { until: 270, name: '4-spike-2000' },
    { until: 300, name: '5-spike-down' },
    { until: 330, name: '6-recovery-0-30s' },
    { until: 360, name: '7-recovery-30-60s' },
    { until: 420, name: '8-recovery-1-2m' },
    { until: 480, name: '9-recovery-2-3m' },
    { until: 510, name: '10-end' },
];

// Same limits for every phase, so the summary shows a line for each of them
const phaseThresholds = {};
for (const phase of PHASES) {
    phaseThresholds[`http_req_duration{phase:${phase.name}}`] = ['avg<500', 'p(95)<2000'];
    phaseThresholds[`http_req_failed{phase:${phase.name}}`] = ['rate<0.05'];
}

export const options = {
    stages: [
        { duration: '1m', target: 100 },
        { duration: '2m', target: 100 },
        { duration: '30s', target: 2000 },  // sudden spike
        { duration: '1m', target: 2000 },
        { duration: '30s', target: 100 },   // sudden drop
        { duration: '3m', target: 100 },    // recovery
        { duration: '30s', target: 0 },
    ],

    thresholds: {
        'http_req_duration': ['p(95)<2000'],
        'http_req_failed': ['rate<0.05'],
        ...phaseThresholds,
    },

    userAgent: 'K6-SpikeTest/1.0',
};

// Mark all requests of this iteration with the current phase
function markPhase() {
    const seconds = exec.instance.currentTestRunDuration / 1000;
    let name = '10-end';
    for (const phase of PHASES) {
        if (seconds < phase.until) {
            name = phase.name;
            break;
        }
    }
    exec.vu.metrics.tags.phase = name;
}

export default function () {
    markPhase();

    const planData = generateTravelPlan();
    const plan = createTravelPlan(planData);
    if (!plan) {
        sleep(1);
        return;
    }
    thinkTime(1, 2);

    const location = addLocation(plan.id, generateLocation());
    thinkTime(1, 2);

    const fullPlan = getTravelPlan(plan.id);
    thinkTime(1, 2);

    if (fullPlan) {
        updateTravelPlan(plan.id, {
            ...planData,
            title: `Spike: ${planData.title}`,
            version: fullPlan.version,
        });
        thinkTime(1, 2);
    }

    if (location) {
        updateLocation(location.id, {
            notes: 'Updated during spike test',
            version: location.version,
        });
        thinkTime(1, 2);
    }

    deleteTravelPlan(plan.id);
    sleep(1);
}

export function setup() {
    console.log('='.repeat(80));
    console.log('SPIKE TEST: 100 users -> sudden spike to 2000 -> back to 100 (8.5 min)');
    console.log('='.repeat(80));
}