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

package org.apache.cxf.systest.factory_pattern;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.cxf.factory_pattern.IsEvenResponse;
import org.apache.cxf.factory_pattern.Number;
import org.apache.cxf.factory_pattern.NumberFactory;
import org.apache.cxf.factory_pattern.NumberFactoryService;
import org.apache.cxf.factory_pattern.NumberService;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.jaxws.support.ServiceDelegateAccessor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Test;

/*
 * exercise with multiplexWithAddress config rather than ws-a headers
 */
public class MultiplexHttpAddressClientServerTest extends AbstractBusClientServerTestBase {
    
    public static class Server extends AbstractBusTestServerBase {        

        protected void run() {
            Object implementor = new HttpNumberFactoryImpl();
            Endpoint.publish(NumberFactoryImpl.FACTORY_ADDRESS, implementor);
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        // no need for ws-a, just enable multiplexWithAddress on server
        Map<String, String> props = new HashMap<String, String>();    
        props.put("cxf.config.file", "org/apache/cxf/systest/factory_pattern/cxf.xml");
        assertTrue("server did not launch correctly",
                   launchServer(Server.class, props, null, false));
    }

    
    @Test
    public void testWithGetPortExtensionHttp() throws Exception {
        
        NumberFactoryService service = new NumberFactoryService();
        NumberFactory factory = service.getNumberFactoryPort();
        
        NumberService numService = new NumberService();
        ServiceImpl serviceImpl = ServiceDelegateAccessor.get(numService);
        
        W3CEndpointReference numberTwoRef = factory.create("20");
        assertNotNull("reference", numberTwoRef);
           
        Number num =  (Number)serviceImpl.getPort(numberTwoRef, Number.class);
        assertTrue("20 is even", num.isEven().isEven());
        
        W3CEndpointReference numberTwentyThreeRef = factory.create("23");
        num =  (Number)serviceImpl.getPort(numberTwentyThreeRef, Number.class);
        assertTrue("23 is not even", !num.isEven().isEven());
    }
    
    @Test
    public void testWithManualMultiplexEprCreation() throws Exception {
    
        Service numService = Service.create(NumberFactoryImpl.NUMBER_SERVICE_QNAME);
        Number num =  (Number)numService.getPort(Number.class);
        
        InvocationHandler handler  = Proxy.getInvocationHandler(num);
        BindingProvider bp = (BindingProvider)handler;    
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
                                   NumberFactoryImpl.NUMBER_SERVANT_ADDRESS_ROOT + "103");
            
        IsEvenResponse numResp = num.isEven();
        assertTrue("103 is not even", Boolean.FALSE.equals(numResp.isEven()));
    }
    
    @Test
    public void testWithGetWsdlOnServant() throws Exception {
        
        int firstChar = new URL(NumberFactoryImpl.NUMBER_SERVANT_ADDRESS_ROOT 
                + "?wsdl").openStream().read();        
        assertTrue("firstChar :" + String.valueOf(firstChar), firstChar == '<');
        
        firstChar = new URL(NumberFactoryImpl.NUMBER_SERVANT_ADDRESS_ROOT 
                                + "103?wsdl").openStream().read();
        assertTrue("firstChar :" + String.valueOf(firstChar), firstChar == '<');
    }
    
    @Test
    public void testSoapAddressLocation() throws Exception {
        
        assertTrue("Should have received the soap:address location " 
                   + NumberFactoryImpl.NUMBER_SERVANT_ADDRESS_ROOT, 
                   checkSoapAddressLocation(NumberFactoryImpl.NUMBER_SERVANT_ADDRESS_ROOT));
        assertTrue("Should have received the soap:address location " 
                   + NumberFactoryImpl.NUMBER_SERVANT_ADDRESS_ROOT + "20", 
                   checkSoapAddressLocation(NumberFactoryImpl.NUMBER_SERVANT_ADDRESS_ROOT + "20"));
        assertTrue("Should have received the soap:address location " 
                   + NumberFactoryImpl.NUMBER_SERVANT_ADDRESS_ROOT + "22", 
                   checkSoapAddressLocation(NumberFactoryImpl.NUMBER_SERVANT_ADDRESS_ROOT + "22"));
        assertTrue("Should have received the soap:address location " 
                   + NumberFactoryImpl.NUMBER_SERVANT_ADDRESS_ROOT + "20", 
                   checkSoapAddressLocation(NumberFactoryImpl.NUMBER_SERVANT_ADDRESS_ROOT + "20"));
        assertTrue("Should have received the soap:address location " 
                   + NumberFactoryImpl.NUMBER_SERVANT_ADDRESS_ROOT, 
                   checkSoapAddressLocation(NumberFactoryImpl.NUMBER_SERVANT_ADDRESS_ROOT));
    }
    
    private boolean checkSoapAddressLocation(String address) 
        throws Exception {
        URL url = new URL(address + "?wsdl");
        
        URLConnection urlConn = url.openConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
        
        while (br.ready()) {
            String str = br.readLine();
            if (str.contains("soap:address") 
                && str.contains("location=" + "\"" + address + "\"")) {
                return  true;
            }
        }
        return false;
    }
}
