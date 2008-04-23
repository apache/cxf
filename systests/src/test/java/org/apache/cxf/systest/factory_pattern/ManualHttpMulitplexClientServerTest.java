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


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.cxf.binding.soap.SoapBindingFactory;
import org.apache.cxf.factory_pattern.IsEvenResponse;
import org.apache.cxf.factory_pattern.Number;
import org.apache.cxf.factory_pattern.NumberFactory;
import org.apache.cxf.factory_pattern.NumberFactoryService;
import org.apache.cxf.factory_pattern.NumberService;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.jaxws.support.ServiceDelegateAccessor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.VersionTransformer;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.junit.BeforeClass;
import org.junit.Test;


public class ManualHttpMulitplexClientServerTest extends AbstractBusClientServerTestBase {
    
    public static class Server extends AbstractBusTestServerBase {        

        protected void run() {
            Object implementor = new ManualNumberFactoryImpl();
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
        assertTrue("server did not launch correctly",
                   launchServer(Server.class));
    }

    
    @Test
    public void testWithManualMultiplexEprCreation() throws Exception {
    
        NumberFactoryService service = new NumberFactoryService();
        NumberFactory nfact = service.getNumberFactoryPort();
        
        W3CEndpointReference w3cEpr = nfact.create("2");        
        assertNotNull("reference", w3cEpr);
        
        // use the epr info only
        // no wsdl so default generated soap/http binding will be used
        // address url must come from the calling context
        EndpointReferenceType epr = VersionTransformer.convertToInternal(w3cEpr); 
        QName serviceName = EndpointReferenceUtils.getServiceName(epr, bus);
        Service numService = Service.create(serviceName);
        
        String portString = EndpointReferenceUtils.getPortName(epr);
        QName portName = new QName(serviceName.getNamespaceURI(), portString);                
        numService.addPort(portName, SoapBindingFactory.SOAP_11_BINDING, "http://foo");
        Number num = (Number)numService.getPort(portName, Number.class);

        setupContextWithEprAddress(epr, num);
        
        IsEvenResponse numResp = num.isEven();
        assertTrue("2 is even", Boolean.TRUE.equals(numResp.isEven()));

        // try again with the address from another epr
        w3cEpr = nfact.create("3");
        epr = VersionTransformer.convertToInternal(w3cEpr);
        setupContextWithEprAddress(epr, num);
        numResp = num.isEven();
        assertTrue("3 is not even", Boolean.FALSE.equals(numResp.isEven()));
        
        // try again with the address from another epr
        w3cEpr = nfact.create("6");
        epr = VersionTransformer.convertToInternal(w3cEpr);
        setupContextWithEprAddress(epr, num);
        numResp = num.isEven();
        assertTrue("6 is even", Boolean.TRUE.equals(numResp.isEven()));
    }
    
    @Test
    public void testWithGetPortExtensionHttp() throws Exception {
        
        NumberFactoryService service = new NumberFactoryService();
        NumberFactory factory = service.getNumberFactoryPort();
         
        
        W3CEndpointReference w3cEpr = factory.create("20");
        EndpointReferenceType numberTwoRef = VersionTransformer.convertToInternal(w3cEpr); 
        assertNotNull("reference", numberTwoRef);
        
        // use getPort with epr api on service
        NumberService numService = new NumberService();
        ServiceImpl serviceImpl = ServiceDelegateAccessor.get(numService);
        
        Number num =  (Number)serviceImpl.getPort(numberTwoRef, Number.class);
        assertTrue("20 is even", num.isEven().isEven());
        w3cEpr = factory.create("23");
        EndpointReferenceType numberTwentyThreeRef = VersionTransformer.convertToInternal(w3cEpr); 
        num =  (Number)serviceImpl.getPort(numberTwentyThreeRef, Number.class);
        assertTrue("23 is not even", !num.isEven().isEven());
    }
    
    private void setupContextWithEprAddress(EndpointReferenceType epr, Number num) {
        
        String address = EndpointReferenceUtils.getAddress(epr);
        
        InvocationHandler handler  = Proxy.getInvocationHandler(num);
        BindingProvider  bp = null;        
        if (handler instanceof BindingProvider) {
            bp = (BindingProvider)handler;    
            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, address);
        }
    }
}
