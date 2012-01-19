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
package org.apache.cxf.systest.handlers;

import java.net.MalformedURLException;
import java.net.URL;

import javax.jws.HandlerChain;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

import org.apache.handler_test.HandlerTest;
import org.apache.handler_test.HandlerTest1;

/**
 * 
 */

@WebServiceClient(name = "HandlerTestService", 
                  targetNamespace = "http://apache.org/handler_test", 
                  wsdlLocation = "file:/D:/svn/cxf/trunk/testutils/src/main/resources/wsdl/handler_test.wsdl")
@HandlerChain(file = "./handlers_invocation_testunused.xml", name = "TestHandlerChain")
public class HandlerTestServiceWithAnnotation extends Service {
    
    static {
        URL url = null;
        try {
            url = new URL("file:/D:/svn/cxf/trunk/testutils/src/main/resources/wsdl/handler_test.wsdl");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        WSDL_LOCATION = url;
    }

    public static final QName SERVICE = new QName("http://apache.org/handler_test", "HandlerTestService");
    public static final QName SOAPPORT = new QName("http://apache.org/handler_test", "SoapPort");
    public static final QName SOAPPORT1 = new QName("http://apache.org/handler_test", "SoapPort1");
    public static final URL WSDL_LOCATION;   
    
    public HandlerTestServiceWithAnnotation(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public HandlerTestServiceWithAnnotation(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public HandlerTestServiceWithAnnotation() {
        super(WSDL_LOCATION, SERVICE);
    }

    /**
     * @return returns HandlerTest
     */
    @WebEndpoint(name = "SoapPort")
    public HandlerTest getSoapPort() {
        return (HandlerTest)super.getPort(SOAPPORT, HandlerTest.class);
    }

    /**
     * @param features A list of {@link javax.xml.ws.WebServiceFeature} to
     *            configure on the proxy. Supported features not in the
     *            <code>features</code> parameter will have their default
     *            values.
     * @return returns HandlerTest
     */
    @WebEndpoint(name = "SoapPort")
    public HandlerTest getSoapPort(WebServiceFeature... features) {
        return (HandlerTest)super.getPort(SOAPPORT, HandlerTest.class, features);
    }

    /**
     * @return returns HandlerTest1
     */
    @WebEndpoint(name = "SoapPort1")
    public HandlerTest1 getSoapPort1() {
        return (HandlerTest1)super.getPort(SOAPPORT1, HandlerTest1.class);
    }

    /**
     * @param features A list of {@link javax.xml.ws.WebServiceFeature} to
     *            configure on the proxy. Supported features not in the
     *            <code>features</code> parameter will have their default
     *            values.
     * @return returns HandlerTest1
     */
    @WebEndpoint(name = "SoapPort1")
    public HandlerTest1 getSoapPort1(WebServiceFeature... features) {
        return (HandlerTest1)super.getPort(SOAPPORT1, HandlerTest1.class, features);
    }

}
