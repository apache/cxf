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
    'wsdlLocation': 'file:../../testutils/src/main/resources/wsdl/hello_world.wsdl',
    'serviceName': 'SOAPService_Test1',
    'portName': 'SoapPort_Test1',
    'targetNamespace': 'http://apache.org/hello_world_soap_http',
};

var ns = new Namespace('ns', "http://apache.org/hello_world_soap_http/types");

WebServiceProvider.invoke = function(req) {
    default xml namespace = ns;
    var resp;
    if (req.localName() == 'greetMe') {
        resp = <ns:greetMeResponse xmlns:ns={ns}/>;
        resp.ns::responseType = 'TestGreetMeResponse';
    } else {
        resp = <ns:sayHiResponse xmlns:ns={ns}/>;
        resp.ns::responseType = 'TestSayHiResponse';
    }
    return resp;
}
