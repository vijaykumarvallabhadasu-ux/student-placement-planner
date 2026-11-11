$ErrorActionPreference = "Stop"
$cp = "C:\Users\vijay\.m2\repository\javax\servlet\javax.servlet-api\4.0.1\javax.servlet-api-4.0.1.jar;C:\Users\vijay\.m2\repository\javax\servlet\jsp\javax.servlet.jsp-api\2.3.3\javax.servlet.jsp-api-2.3.3.jar;C:\Users\vijay\.m2\repository\com\oracle\database\jdbc\ojdbc11\23.6.0.24.10\ojdbc11-23.6.0.24.10.jar"
$mainFiles = Get-ChildItem -Path "src\main\java\com\placementplanner" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
$testFiles = Get-ChildItem -Path "src\test\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
$allFiles = @($mainFiles) + @($testFiles)

Write-Host "Compiling $($allFiles.Count) Java files..."
& "C:\Program Files\Java\jdk-25.0.2\bin\javac.exe" -cp $cp -d "target\classes" --release 17 $allFiles
Write-Host "Compilation finished with exit code: $LASTEXITCODE"

if ($LASTEXITCODE -eq 0) {
    Write-Host "Running ComprehensiveRepairVerificationTest..."
    $runCp = "target\classes;C:\Users\vijay\.m2\repository\com\oracle\database\jdbc\ojdbc11\23.6.0.24.10\ojdbc11-23.6.0.24.10.jar;C:\Users\vijay\.m2\repository\javax\servlet\javax.servlet-api\4.0.1\javax.servlet-api-4.0.1.jar"
    & "C:\Program Files\Java\jdk-25.0.2\bin\java.exe" -cp $runCp com.placementplanner.ComprehensiveRepairVerificationTest
}
