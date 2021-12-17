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
package org.apache.cxf.systest.mtom_schema_validation;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebEndpoint;
import jakarta.xml.ws.WebServiceClient;
import jakarta.xml.ws.WebServiceFeature;

@WebServiceClient(name = "HelloWS", targetNamespace = "http://cxf.apache.org/")
public class HelloWSClient extends Service {
    private QName portName = new QName("http://cxf.apache.org/", "hello");

    public HelloWSClient(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     * @return returns HelloWS
     */
    @WebEndpoint(name = "hello")
    public HelloWS getHello() {
        return super.getPort(portName, HelloWS.class);
    }

    /**
     * @param features A list of {@link jakarta.xml.ws.WebServiceFeature} to configure on the proxy. Supported
     *            features not in the <code>features</code> parameter will have their default values.
     * @return returns HelloWS
     */
    @WebEndpoint(name = "hello")
    public HelloWS getHello(WebServiceFeature... features) {
        return super.getPort(portName, HelloWS.class, features);
    }

}
