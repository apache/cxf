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

var WebServiceProvider = {
    'wsdlLocation': 'file:../testutils/target/classes/wsdl/hello_world.wsdl',
    'serviceName': 'SOAPService',
    'portName': 'SoapPort',
    'targetNamespace': 'http://apache.org/hello_world_soap_http',
    'ServiceMode': 'MESSAGE',
};

var SOAP_ENV = "http://schemas.xmlsoap.org/soap/envelope/";
var ns4 = "http://apache.org/hello_world_soap_http/types";

WebServiceProvider.invoke = function(req) {
    var resp = req.getImplementation().createDocument(SOAP_ENV, "SOAP-ENV:Envelope", null);
    var list = req.getElementsByTagNameNS(ns4, "greetMe");
    var txt, responseNode;
    if (list.length > 0) {
        txt = resp.createTextNode("TestGreetMeResponse");
        responseNode = 'ns4:greetMeResponse';
    } else {
        txt = resp.createTextNode("TestSayHiResponse");
        responseNode = 'ns4:sayHiResponse';
    }
    var respType = resp.createElementNS(ns4, "ns4:responseType");
    respType.insertBefore(txt, null);
    var gm = resp.createElementNS(ns4, responseNode);
    gm.insertBefore(respType, null);
    var sb = resp.createElementNS(SOAP_ENV, "SOAP-ENV:Body");
    sb.insertBefore(gm, null);
    resp.getDocumentElement().insertBefore(sb, null);
    return resp;
}
