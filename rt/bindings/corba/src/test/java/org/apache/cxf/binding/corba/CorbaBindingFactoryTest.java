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
package org.apache.cxf.binding.corba;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;



import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class CorbaBindingFactoryTest extends Assert {
    
    protected Bus bus;
    protected EndpointInfo endpointInfo;
    protected EndpointReferenceType target;    
    protected MessageObserver observer;
    protected Message inMessage;
    CorbaBindingFactory factory;
    
    @Before
    public void setUp() throws Exception {
        bus = BusFactory.getDefaultBus();       
        BindingFactoryManager bfm = bus.getExtension(BindingFactoryManager.class);        
        factory = (CorbaBindingFactory)bfm.getBindingFactory("http://cxf.apache.org/bindings/corba");
        bfm.registerBindingFactory(CorbaConstants.NU_WSDL_CORBA, factory);               
    }
    
    @After
    public void tearDown() {
        bus.shutdown(true);     
    }
    
    protected void setupServiceInfo(String ns, String wsdl, String serviceName, String portName) {        
        URL wsdlUrl = getClass().getResource(wsdl);
        assertNotNull(wsdlUrl);
        WSDLServiceFactory f = new WSDLServiceFactory(bus, wsdlUrl, new QName(ns, serviceName));

        Service service = f.create();        
        endpointInfo = service.getEndpointInfo(new QName(ns, portName));
   
    }
    
    @Test
    public void testCreateBinding() throws Exception {
        IMocksControl control = EasyMock.createNiceControl();
        BindingInfo bindingInfo = control.createMock(BindingInfo.class);

        CorbaBinding binding = (CorbaBinding)factory.createBinding(bindingInfo);
        assertNotNull(binding);
        assertTrue(CorbaBinding.class.isInstance(binding));        
        List<Interceptor> inInterceptors = binding.getInInterceptors();
        assertNotNull(inInterceptors);
        List<Interceptor> outInterceptors = binding.getOutInterceptors();
        assertNotNull(outInterceptors);
        assertEquals(2, inInterceptors.size());
        assertEquals(2, outInterceptors.size());        
    }

    @Test
    public void testGetCorbaConduit() throws Exception {
        setupServiceInfo("http://cxf.apache.org/bindings/corba/simple", 
                         "/wsdl_corbabinding/simpleIdl.wsdl", 
                         "SimpleCORBAService", 
                         "SimpleCORBAPort");
        
        Conduit conduit = factory.getConduit(endpointInfo);
        assertNotNull(conduit);   
        conduit = factory.getConduit(endpointInfo, null);
        assertNotNull(conduit);
        target = EasyMock.createMock(EndpointReferenceType.class);
        conduit = factory.getConduit(endpointInfo, target);
        assertNotNull(conduit);
    }
            
    @Test
    public void testGetDestination() throws Exception {                
        setupServiceInfo("http://cxf.apache.org/bindings/corba/simple", 
                         "/wsdl_corbabinding/simpleIdl.wsdl", 
                         "SimpleCORBAService", 
                         "SimpleCORBAPort");
        
        Destination destination = factory.getDestination(endpointInfo);
        assertNotNull(destination);
        target = destination.getAddress();
        assertNotNull(target);
    }
    
    @Test
    public void testTransportIds() throws Exception {
        setupServiceInfo("http://cxf.apache.org/bindings/corba/simple", 
                         "/wsdl_corbabinding/simpleIdl.wsdl", 
                         "SimpleCORBAService", 
                         "SimpleCORBAPort");
        
        List<String> strs = new ArrayList<String>();
        strs.add("one");
        strs.add("two");
        factory.setTransportIds(strs);        
        List<String> retStrs = factory.getTransportIds();
        assertNotNull(retStrs);
        String str = retStrs.get(0);
        assertEquals("one", str.toString());
        str = retStrs.get(1);
        assertEquals("two", str.toString());
    }
    
    @Test
    public void testGetUriPrefixes() throws Exception {
        Set<String> prefixes = factory.getUriPrefixes();
        assertNotNull("Prefixes should not be null", prefixes != null);
    }
    
    // check this
    @Test
    public void testSetBus() throws Exception {
        factory.setBus(bus);    
    }

}
