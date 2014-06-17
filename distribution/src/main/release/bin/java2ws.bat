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

rem 
rem  invoke the CXF java2ws tool
rem 
@setlocal

if not defined CXF_HOME goto set_cxf_home
                                                                                                                                                             
:cont
if not defined JAVA_HOME goto no_java_home

set TOOLS_JAR=%JAVA_HOME%\lib\tools.jar;

if not exist "%CXF_HOME%\lib\cxf-manifest.jar" goto no_cxf_jar

set CXF_JAR=%CXF_HOME%\lib\cxf-manifest.jar

"%JAVA_HOME%\bin\java" -Djava.endorsed.dirs="%CXF_HOME%\lib\endorsed" -cp "%CXF_JAR%;%TOOLS_JAR%;%CLASSPATH%" -Djava.util.logging.config.file="%CXF_HOME%\etc\logging.properties" org.apache.cxf.tools.java2ws.JavaToWS %*

@endlocal

goto end

:no_cxf_jar
echo ERROR: Unable to find cxf-manifest.jar in %cxf_home/lib
goto end

:no_java_home
echo ERROR: Set JAVA_HOME to the path where the JDK (6.0 or higher) is installed
goto end 

:set_cxf_home
set CXF_HOME=%~dp0..
goto cont

:end



