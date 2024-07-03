@echo off
@setlocal

rem  Licensed to the Apache Software Foundation (ASF) under one
rem  or more contributor license agreements. See the NOTICE file
rem  distributed with this work for additional information
rem  regarding copyright ownership. The ASF licenses this file
rem  to you under the Apache License, Version 2.0 (the
rem  "License"); you may not use this file except in compliance
rem  with the License. You may obtain a copy of the License at
rem 
rem  http://www.apache.org/licenses/LICENSE-2.0
rem 
rem  Unless required by applicable law or agreed to in writing,
rem  software distributed under the License is distributed on an
rem  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
rem  KIND, either express or implied. See the License for the
rem  specific language governing permissions and limitations
rem  under the License.


cd ..

set BASEDON=Time
set AMOUNT=3
set OPERATION=echoString
set PACKETSIZE=1
set THREADS=1
set HOST=localhost
set PORT=20000

:Loop
IF "%1"=="" GOTO Continue
IF "%1"=="-BasedOn" (set BASEDON=%2)
IF "%1"=="-Amount" (set AMOUNT=%2)
IF "%1"=="-Operation" (set OPERATION=%2)
IF "%1"=="-Threads" (set THREADS=%2)
IF "%1"=="-HostName" (set HOST=%2)
IF "%1"=="-Port" (set PORT=%2)
IF "%1"=="-PacketSize" (set PACKETSIZE=%2)
SHIFT
GOTO Loop

:Continue

ant client -Dcxf.running.time=%AMOUNT% -Dcxf.operation=%OPERATION% -Dcxf.basedon=%BASEDON% -Dcxf.packet.size=%PACKETSIZE% -Dcxf.threads=%THREADS% -Dcxf.port.name=%PORT% -Dcxf.host.name=%HOST% 

@endlocal
