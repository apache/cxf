<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements. See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership. The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License. You may obtain a copy of the License at
 
  http://www.apache.org/licenses/LICENSE-2.0
 
  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied. See the License for the
  specific language governing permissions and limitations
  under the License.
-->


cxf-bundle-minimal bundle contains the modules needed for the
remote SOAP-based (SOAP-HTTP and SOAP-JMS) communications only.

The following is the list of modules which is not included in this bundle :

cxf-tools-common
cxf-tools-validator
cxf-tools-wsdlto-core
cxf-tools-misctools
cxf-tools-wsdlto-databinding-jaxb
cxf-tools-corba
cxf-tools-wsdlto-frontend-jaxws
cxf-tools-wsdlto-frontend-javascript
cxf-tools-java2ws
cxf-xjc-ts
cxf-xjc-dv
cxf-rt-management
cxf-rt-transports-local
cxf-rt-bindings-corba
cxf-rt-bindings-coloc
cxf-rt-bindings-object
cxf-rt-bindings-xml
cxf-rt-bindings-http
cxf-rt-frontend-js
cxf-rt-frontend-jaxrs
cxf-rt-javascript

If your code depends on any of the above modules then please use 
an all-inclusive cxf-bundle bundle or cxf-bundle-jaxrs bundle instead,
depending on your requirements.
