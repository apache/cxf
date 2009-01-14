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

This demo illustrate how external cxf client using soap/http  can
communicate with external cxf server using soap/jms  through JBI
soap and jms binding component(as a transformer). 

The demo consists of a ServiceMix soap binding component  and a ServiceMix JMS
binding component.  A servicemix JMS binding service unit is deployed into ServiceMix JMS binding
component. A servicemix soap binding service unit is deployed into
servicemix soap binding component. ServiceMix soap binding service unit and ServiceMix JMS binding service
unit are wrapped in cxf demo service assembly.

A cxf client (as consumer) invoke servicemix soap binding
service unit using soap/http, the servicemix soap binding service unit
route this request to servicemix jms binding service unit using NMR,
then servicemix jms binding service unit invoke cxf server(as
provider) using soap/jms. Here servicemix JMS binding service unit and
soap binding service unit play the role as a transformer, connecting cxf service consumer and provider with different transport.




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
consumer and provider communication log.To remove noisy CXF log from the
console, just edit servicemix starup script, add
-Djava.util.logging.config.file="$CXF_HOME/etc/logging.properties" to
java launch commandline

Start cxf server
For UNIX:
 > ant server -Dthirdparty.classpath=$SERVICEMIX_HOME/lib/activemq-core-5.0.0.jar:$SERVICEMIX_HOME/lib/backport-util-concurrent-2.2.jar:$SERVICEMIX_HOME/lib/activeio-core-3.0.0-incubator.jar:$SERVICEMIX_HOME/lib/geronimo-j2ee-management_1.0_spec-1.1.jar 
For Windows:
 > ant server -Dthirdparty.classpath="%SERVICEMIX_HOME%\lib\activemq-core-5.0.0.jar;%SERVICEMIX_HOME%\lib\geronimo-j2ee-management_1.0_spec-1.1.jar"

Deploy and start the CXF demo service assembly

For UNIX:
 > ant -f $SERVICEMIX_HOME/ant/servicemix-ant-task.xml  deploy-service-assembly -Dsm.deploy.file=./service-assembly/build/lib/cxf-service-assembly.zip -Dsm.username=smx -Dsm.password=smx
 > ant -f $SERVICEMIX_HOME/ant/servicemix-ant-task.xml start-service-assembly -Dsm.service.assembly.name=cxf-demo-service-assembly -Dsm.username=smx -Dsm.password=smx
For Windows:
 > ant -f "%SERVICEMIX_HOME%"\ant\servicemix-ant-task.xml deploy-service-assembly -Dsm.deploy.file=.\service-assembly\build\lib\cxf-service-assembly.zip -Dsm.username=smx -Dsm.password=smx
 > ant -f "%SERVICEMIX_HOME%"\ant\servicemix-ant-task.xml start-service-assembly -Dsm.service.assembly.name=cxf-demo-service-assembly -Dsm.username=smx -Dsm.password=smx

Start cxf client
 > ant client



What happened
=============
A cxf client (as consumer) invoke servicemix soap binding
service unit using soap/http, the servicemix soap binding service unit
route this request to servicemix jms binding service unit using NMR,
then servicemix jms binding service unit invoke cxf server(as
provider) using soap/jms. Here servicemix JMS binding service unit and
soap binding service unit play the role as a transformer, connecting cxf service consumer and provider with different transport.
