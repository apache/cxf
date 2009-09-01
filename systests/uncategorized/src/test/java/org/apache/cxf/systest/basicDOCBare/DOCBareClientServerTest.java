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



package org.apache.cxf.systest.basicDOCBare;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_doc_lit_bare.PutLastTradedPricePortType;
import org.apache.hello_world_doc_lit_bare.SOAPService;
import org.apache.hello_world_doc_lit_bare.types.TradePriceData;
import org.junit.BeforeClass;
import org.junit.Test;

public class DOCBareClientServerTest extends AbstractBusClientServerTestBase {    

    private final QName serviceName = new QName("http://apache.org/hello_world_doc_lit_bare",
                                                "SOAPService");
    private final QName portName = new QName("http://apache.org/hello_world_doc_lit_bare", "SoapPort");
    
    
    @BeforeClass
    public static void startServers() throws Exception {
        System.setProperty("org.apache.cxf.bus.factory", "org.apache.cxf.bus.CXFBusFactory");
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }

    @Test
    public void testBasicConnection() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/doc_lit_bare.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        PutLastTradedPricePortType putLastTradedPrice = service.getPort(portName,
                                                                        PutLastTradedPricePortType.class);
        String response = putLastTradedPrice.bareNoParam();
        assertEquals("testResponse", response);
        
        TradePriceData priceData = new TradePriceData();
        priceData.setTickerPrice(1.0f);
        priceData.setTickerSymbol("CELTIX");

        Holder<TradePriceData> holder = new Holder<TradePriceData>(priceData);

        for (int i = 0; i < 5; i++) {
            putLastTradedPrice.sayHi(holder);
            assertEquals(4.5f, holder.value.getTickerPrice(), 0.01);
            assertEquals("APACHE", holder.value.getTickerSymbol());
            putLastTradedPrice.putLastTradedPrice(priceData);
        }

    }

    @Test
    public void testAnnotation() throws Exception {
        Class<PutLastTradedPricePortType> claz = PutLastTradedPricePortType.class;
        TradePriceData priceData = new TradePriceData();
        Holder<TradePriceData> holder = new Holder<TradePriceData>(priceData);
        Method method = claz.getMethod("sayHi", holder.getClass());
        assertNotNull("Can not find SayHi method in generated class ", method);
        Annotation ann = method.getAnnotation(WebMethod.class);
        WebMethod webMethod = (WebMethod)ann;
        assertEquals(webMethod.operationName(), "SayHi");
        Annotation[][] paraAnns = method.getParameterAnnotations();
        for (Annotation[] paraType : paraAnns) {
            for (Annotation an : paraType) {
                if (an.annotationType() == WebParam.class) {
                    WebParam webParam = (WebParam)an;
                    assertNotSame("", webParam.targetNamespace());
                }
            }
        }
    }
    
    @Test
    public void testNillableParameter() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/doc_lit_bare.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        PutLastTradedPricePortType port = service.getPort(portName,
                                                          PutLastTradedPricePortType.class);
        String result = port.nillableParameter(null);
        assertNull(result);
    } 


}

