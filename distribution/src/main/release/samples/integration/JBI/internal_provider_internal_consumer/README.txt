/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


Overview 
========

Prerequisite:  This README assumes some familiarity with the Java
Business Integration specification.  See the following URL for more
information: http://java.sun.com/integration/

This demo shows how CXF can be used to implement service
implementations for a Java Business Integration (JBI) container,. The
demo consists of a CXF Service Engine and a test service assembly.
The service assembly contains two service units: a service provider (server)
and a service consumer (client).

In each case the service units are written to the standard JAXWS 2.0
API.  The assembly is deployed to the CXF Service Engine (CSE).
The CSE connects the service implementation (and client) to the JBI
Normalized Message Router (NMR) using a customized CXF transport.

The JBI/NMR transport in this demo support InOut and InOnly message exchange pattern.



Deploy CXF Service Engine into ServiceMix
============================================
Build Instructions
------------------

. Download & Install ServiceMix 
  http://servicemix.apache.org/servicemix-321.html
. export SERVICEMIX_HOME for your shell envirnoment



. build everything using ant: 'ant build'

Installation & Deployment
-------------------------
Ensure that the $SERVICEMIX_HOME/bin is on the path.

Start ServiceMix
 >servicemix
And then you can see logs from the shell which you start servicemix, including
ServiceEngine install log, Service Assembly deploy log, cxf service
consumer and provider communication log. To remove noisy CXF log from the
console, just edit servicemix starup script, add
-Djava.util.logging.config.file="$CXF_HOME/etc/logging.properties" to
java launch commandline

Install and start the CXF Service Engine:

For UNIX:
 > ant -f $SERVICEMIX_HOME/ant/servicemix-ant-task.xml install-component -Dsm.install.file=../service-engine/build/lib/cxf-service-engine.jar -Dsm.username=smx -Dsm.password=smx
 > ant -f $SERVICEMIX_HOME/ant/servicemix-ant-task.xml start-component -Dsm.component.name=CXFServiceEngine -Dsm.username=smx -Dsm.password=smx
For Windows:
 > ant -f "%SERVICEMIX_HOME%"\ant\servicemix-ant-task.xml install-component -Dsm.install.file=..\service-engine\build\lib\cxf-service-engine.jar -Dsm.username=smx -Dsm.password=smx
 > ant -f "%SERVICEMIX_HOME%"\ant\servicemix-ant-task.xml start-component -Dsm.component.name=CXFServiceEngine -Dsm.username=smx -Dsm.password=smx

Deploy and start the CXF demo service assembly

For UNIX:
 > ant -f $SERVICEMIX_HOME/ant/servicemix-ant-task.xml  deploy-service-assembly -Dsm.deploy.file=./service-assembly/build/lib/cxf-service-assembly.zip -Dsm.username=smx -Dsm.password=smx
 > ant -f $SERVICEMIX_HOME/ant/servicemix-ant-task.xml start-service-assembly -Dsm.service.assembly.name=cxf-demo-service-assembly -Dsm.username=smx -Dsm.password=smx
For Windows:
 > ant -f "%SERVICEMIX_HOME%"\ant\servicemix-ant-task.xml  deploy-service-assembly -Dsm.deploy.file=.\service-assembly\build\lib\cxf-service-assembly.zip -Dsm.username=smx -Dsm.password=smx
 > ant -f "%SERVICEMIX_HOME%"\ant\servicemix-ant-task.xml start-service-assembly -Dsm.service.assembly.name=cxf-demo-service-assembly -Dsm.username=smx -Dsm.password=smx

More lifecycle management task

For UNIX:
 > ant -f $SERVICEMIX_HOME/ant/servicemix-ant-task.xml -projecthelp
For Windows:
 > ant -f "%SERVICEMIX_HOME%"\ant\servicemix-ant-task.xml -projecthelp

What happened
=============
The SE will start both Serivce Units in the assembly.  The consumer is
coded to wait for the providers endpoint to activate.  Once the
provider endpoint has activated, the consumer sends messages to the
provider.  These messages are taken by the CXF JBI transport,
wrapped in a NormalizedMessage and sent via the NMR to the service
provider.  The responses are sent back in a similar fashion. greetMe/sayHi
use InOut MEP, greetMeOneWay use InOnly MEP.
