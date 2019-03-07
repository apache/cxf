@echo off
rem
rem
rem    Licensed to the Apache Software Foundation (ASF) under one
rem    or more contributor license agreements. See the NOTICE file
rem    distributed with this work for additional information
rem    regarding copyright ownership. The ASF licenses this file
rem    to you under the Apache License, Version 2.0 (the
rem    "License"); you may not use this file except in compliance
rem    with the License. You may obtain a copy of the License at
rem
rem    http://www.apache.org/licenses/LICENSE-2.0
rem
rem    Unless required by applicable law or agreed to in writing,
rem    software distributed under the License is distributed on an
rem    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
rem    KIND, either express or implied. See the License for the
rem    specific language governing permissions and limitations
rem    under the License.
rem
rem

rem  invoke the Apache CXF wsdlvalidator tool
rem 
@setlocal

if not defined CXF_HOME goto set_cxf_home
                                                                                                                                                             
:cont
if not defined JAVA_HOME goto no_java_home

rem Retrieve java version
for /f tokens^=2-5^ delims^=.-_+^" %%j in ('"%JAVA_HOME%\bin\java" -fullversion 2^>^&1') do (
    if %%j==1 (set JAVA_VERSION=%%k) else (set JAVA_VERSION=%%j)
)

if %JAVA_VERSION% LSS 9 (
    set TOOLS_JAR=%JAVA_HOME%\lib\tools.jar;
)

if not exist "%CXF_HOME%\lib\cxf-manifest.jar" goto no_cxf_jar

set CXF_JAR=%CXF_HOME%\lib\cxf-manifest.jar

if "%JAVA_MAX_MEM%" == "" (
    set JAVA_MAX_MEM=512M
)

if %JAVA_VERSION% GTR 8 (
    "%JAVA_HOME%\bin\java" -Xmx%JAVA_MAX_MEM% -cp "%CXF_JAR%;%CLASSPATH%" -Djava.util.logging.config.file="%CXF_HOME%\etc\logging.properties" org.apache.cxf.tools.validator.WSDLValidator %*
) else (
    "%JAVA_HOME%\bin\java" -Xmx%JAVA_MAX_MEM% -Djava.endorsed.dirs="%CXF_HOME%\lib\endorsed" -cp "%CXF_JAR%;%TOOLS_JAR%;%CLASSPATH%" -Djava.util.logging.config.file="%CXF_HOME%\etc\logging.properties" org.apache.cxf.tools.validator.WSDLValidator %*
)

@endlocal

goto end

:no_cxf_jar
echo ERROR: Unable to find cxf.jar in %cxf_home/lib
goto end

:no_java_home
echo ERROR: Set JAVA_HOME to the path where the JDK (6.0 or higher) is installed
goto end 

:set_cxf_home
set CXF_HOME=%~dp0..
goto cont
:end
