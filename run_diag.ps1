# Optional Standalone Database Diagnostics Runner
# Usage: powershell -ExecutionPolicy Bypass -File .\run_diag.ps1
$java = "C:\Program Files\Java\jdk-25.0.2\bin\java.exe"
$cp = "target/test-classes;target/classes;C:\Users\vijay\.m2\repository\com\oracle\database\jdbc\ojdbc11\23.6.0.24.10\ojdbc11-23.6.0.24.10.jar"
& $java -cp $cp com.placementplanner.DbDiagnostic
