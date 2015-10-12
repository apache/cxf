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

package org.apache.cxf.systest.jibx;

import java.math.BigDecimal;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jibx.doc_lit_bare.PutLastTradedPricePortType;
import org.apache.cxf.jibx.doclitbare.types.In;
import org.apache.cxf.jibx.doclitbare.types.InDecimal;
import org.apache.cxf.jibx.doclitbare.types.Inout;
import org.apache.cxf.jibx.doclitbare.types.OutString;
import org.apache.cxf.jibx.doclitbare.types.StringRespType;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.hello_world_soap_http_jibx.jibx.GreetMeFault;
import org.apache.hello_world_soap_http_jibx.jibx.Greeter;
import org.apache.hello_world_soap_http_jibx.jibx.PingMeFault;
import org.apache.hello_world_soap_http_jibx.jibx.SOAPService;
import org.apache.helloworldsoaphttpjibx.jibx.types.FaultDetail;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class ClientServerJibxTest extends AbstractBusClientServerTestBase {
    static final String WSDL_PORT = TestUtil.getPortNumber(Server.class);

    private static final QName SERVICE_NAME 
        = new QName("http://apache.org/hello_world_soap_http_jibx/jibx", "SOAPService");
    
    private static final QName DOC_LIT_BARE_SERVICE =
        new QName("http://cxf.apache.org/jibx/doc_lit_bare", "SOAPService");
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }
    
    @Test
    public void testCallFromDocLitBareClient() throws Exception {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/systest/jibx/cxf.xml");
        BusFactory.setDefaultBus(bus);
        URL wsdl = this.getClass().getResource("/wsdl_systest_databinding/jibx/doc_lit_bare.wsdl");
        assertNotNull("We should have found the WSDL here. ", wsdl);      
        
        org.apache.cxf.jibx.doc_lit_bare.SOAPService ss = 
            new org.apache.cxf.jibx.doc_lit_bare.SOAPService(wsdl, DOC_LIT_BARE_SERVICE);
        PutLastTradedPricePortType port = ss.getSoapPort();
        updateAddressPort(port, WSDL_PORT);
        
         
        ClientProxy.getClient(port).getInInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(port).getOutInterceptors().add(new LoggingOutInterceptor());
        StringRespType resp = port.bareNoParam();
        assertEquals("Get a wrong response", "Get the request!", resp.getStringRespType());
        
        InDecimal xd = new InDecimal();
        xd.setInDecimal(new BigDecimal(123));
        OutString response = port.nillableParameter(xd);
        assertEquals("Get a wrong response", "Get the request 123", response.getOutString());
        
        In data = new In();
        data.setTickerPrice(12.33F);
        data.setTickerSymbol("CXF");
        port.putLastTradedPrice(data);
        
        Inout dataio = new Inout();
        dataio.setTickerPrice(12.33F);
        dataio.setTickerSymbol("CXF");
        Holder<Inout> holder = new Holder<Inout>(dataio);
        port.sayHi(holder);
        assertEquals("Get a wrong response", "BAK", holder.value.getTickerSymbol());
    }
    
    @Test
    public void testCallFromClient() throws Exception {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/systest/jibx/cxf.xml");
        BusFactory.setDefaultBus(bus);
        URL wsdl = this.getClass().getResource("/wsdl_systest_databinding/jibx/hello_world.wsdl");
        assertNotNull("We should have found the WSDL here. ", wsdl);      
        
        SOAPService ss = new SOAPService(wsdl, SERVICE_NAME);
        Greeter port = ss.getSoapPort();
        updateAddressPort(port, WSDL_PORT);
        
        String resp; 
        ClientProxy.getClient(port).getInInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(port).getOutInterceptors().add(new LoggingOutInterceptor());
        resp = port.sayHi();
        assertEquals("We should get the right response", "Bonjour", resp);        
        
        resp = port.greetMe("Willem");
        assertEquals("We should get the right response", "Hello Willem", resp);

        try {
            port.greetMe("fault");
            fail("Should have been a fault");
        } catch (GreetMeFault ex) {
            assertEquals("Some fault detail", ex.getFaultInfo().getGreetMeFaultDetail());
        }
        
        try {
            port.pingMe();
            fail("We expect exception here");
        } catch (PingMeFault ex) {            
            FaultDetail detail = ex.getFaultInfo();
            assertEquals("Wrong faultDetail major", detail.getMajor(), 2);
            assertEquals("Wrong faultDetail minor", detail.getMinor(), 1);             
        }
        
    }
    
}
