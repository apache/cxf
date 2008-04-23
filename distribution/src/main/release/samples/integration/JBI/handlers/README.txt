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

This demo shows how JAXWS handlers can be used in cxf service
engine.  The demo consists of a CXF Service Engine and a test service assembly.
The service assembly contains two service units: a service provider (server)
and a service consumer (client).

In each case the, service units are written to the standard JAXWS 2.0
API.  The assembly is deployed to the CXF Service Engine (CSE).
The CSE connects the service implementation (and client) to the JBI
Normalized Message Router (NMR) using a customized CXF transport.


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
console, just edit servicemix startup script, add
-Djava.util.logging.config.file="$CXF_HOME/etc/logging.properties" to
java launch commandline

Install the CXF Service Engine:

For UNIX:
 > ant -f $SERVICEMIX_HOME/ant/servicemix-ant-task.xml install-component -Dsm.install.file=../service-engine/build/lib/cxf-service-engine.jar  -Dsm.username=smx -Dsm.password=smx
 > ant -f $SERVICEMIX_HOME/ant/servicemix-ant-task.xml start-component -Dsm.component.name=CXFServiceEngine  -Dsm.username=smx -Dsm.password=smx
For Windows:
 > ant -f "%SERVICEMIX_HOME%"/ant/servicemix-ant-task.xml install-component -Dsm.install.file=../service-engine/build/lib/cxf-service-engine.jar -Dsm.username=smx -Dsm.password=smx
 > ant -f "%SERVICEMIX_HOME%"/ant/servicemix-ant-task.xml start-component> -Dsm.component.name=CXFServiceEngine  -Dsm.username=smx -Dsm.password=smx
Deploy the CXF demo service assembly

For UNIX:
 > ant -f $SERVICEMIX_HOME/ant/servicemix-ant-task.xml  deploy-service-assembly -Dsm.deploy.file=./service-assembly/build/lib/cxf-service-assembly.zip  -Dsm.username=smx -Dsm.password=smx
 > ant -f $SERVICEMIX_HOME/ant/servicemix-ant-task.xml start-service-assembly -Dsm.service.assembly.name=cxf-demo-service-assembly  -Dsm.username=smx -Dsm.password=smx
For Windows:
 > ant -f "%SERVICEMIX_HOME%"/ant/servicemix-ant-task.xml deploy-service-assembly -Dsm.deploy.file=./service-assembly/build/lib/cxf-service-assembly.zip -Dsm.username=smx -Dsm.password=smx
 > ant -f "%SERVICEMIX_HOME%"/ant/servicemix-ant-task.xml start-service-assembly -Dsm.service.assembly.name=cxf-demo-service-assembly  -Dsm.username=smx -Dsm.password=smx

More lifecycle management task
For UNIX:
 > ant -f $SERVICEMIX_HOME/ant/servicemix-ant-task.xml -projecthelp
For Windows:
 > ant -f "%SERVICEMIX_HOME%"/ant/servicemix-ant-task.xml -projecthelp

What happened
=============
The service provider uses a
SOAP protocol handler which simply logs incoming and outgoing messages
to the console.  

The service provider code registers a handler using the @HandlerChain annotation
within the service implementation class. For this demo, LoggingHandler
is SOAPHandler that logs the entire SOAP message content to stdout.

While the annotation in the service implementation class specifies
that the service provider should use the LoggingHandler, the demo shows how
this behaviour is superceded by information obtained from the
cxf-server.xml configuration file, thus allowing control over the
service provider's behaviour without changing the code.  When the
service provider uses the configuration file, LoggingHandler is replaced with
FileLoggingHandler, which logs simple informative messages, not the
entire message content, to the console and adds information to the
demo.log file.

The service consumer includes a logical handler that checks the parameters on
outbound requests and short-circuits the invocation in certain
circumstances. This handler is not specified programatically but
through configuration in the file cxf-client.xml.  
