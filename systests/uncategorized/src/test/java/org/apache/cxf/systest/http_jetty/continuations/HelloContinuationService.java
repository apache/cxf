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
package org.apache.cxf.systest.http_jetty.continuations;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;

/**
 * 
 */

@WebServiceClient(name = "HelloService", 
                  targetNamespace = "http://cxf.apache.org/systest/jaxws", 
                  wsdlLocation = "testutils/hello.wsdl")
public class HelloContinuationService extends Service {
    static final QName SERVICE = 
        new QName("http://cxf.apache.org/systest/jaxws", "HelloContinuationService");
    static final QName HELLO_PORT = 
        new QName("http://cxf.apache.org/systest/jaxws", "HelloContinuationPort");
    public HelloContinuationService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    @WebEndpoint(name = "HelloContinuationPort")
    public HelloContinuation getHelloContinuationPort() {
        return (HelloContinuation)super.getPort(HELLO_PORT, HelloContinuation.class);
    }

}
