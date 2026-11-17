@echo off
REM Runs a k6 scenario with the web dashboard and saves results.
REM Usage (from tests\performance-tests):  run-test.bat load
REM   -> runs tests\load-test.js
REM   -> HTML report with charts: results\load-report.html
REM   -> summary metrics:         results\load-summary.json
REM Live dashboard during the run: http://localhost:5665

if "%1"=="" (
    echo Usage: run-test.bat [smoke^|load^|stress^|spike^|endurance]
    exit /b 1
)

set K6_WEB_DASHBOARD=true
set K6_WEB_DASHBOARD_EXPORT=results\%1-report.html
k6 run --summary-export=results\%1-summary.json tests\%1-test.js