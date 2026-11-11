@echo off
set JAVAC="C:\Program Files\Java\jdk-25.0.2\bin\javac.exe"
set CP=C:\Users\vijay\.m2\repository\javax\servlet\javax.servlet-api\4.0.1\javax.servlet-api-4.0.1.jar;C:\Users\vijay\.m2\repository\javax\servlet\jsp\javax.servlet.jsp-api\2.3.3\javax.servlet.jsp-api-2.3.3.jar;C:\Users\vijay\.m2\repository\com\oracle\database\jdbc\ojdbc11\23.6.0.24.10\ojdbc11-23.6.0.24.10.jar
dir /s /b src\main\java\com\placementplanner\*.java > sources.txt
%JAVAC% -cp "%CP%" -d target\classes --release 17 @sources.txt
set RESULT=%ERRORLEVEL%
del sources.txt
exit /b %RESULT%
