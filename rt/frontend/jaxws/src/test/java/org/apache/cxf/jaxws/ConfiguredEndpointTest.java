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

package org.apache.cxf.jaxws;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.soap.SoapBindingFactory;
import org.apache.cxf.binding.soap.SoapTransportFactory;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.apache.hello_world_soap_http.SOAPService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ConfiguredEndpointTest extends Assert {
    private static final QName SERVICE_NAME = 
        new QName("http://apache.org/hello_world_soap_http", "SOAPService");    
    private static final QName PORT_NAME = 
        new QName("http://apache.org/hello_world_soap_http", "SoapPort");

    private BusFactory factory;
    
    @After
    public void tearDown() {
        Bus bus = BusFactory.getDefaultBus();
        if (null != bus) {
            bus.shutdown(true);
            BusFactory.setDefaultBus(null);
        }
        System.clearProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME);
    }
   
    @Test
    public void testCXFDefaultClientEndpoint() {
        factory = new CXFBusFactory();
        BusFactory.setDefaultBus(null);
        factory.createBus();
        System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, CXFBusFactory.class.getName());
        doTestDefaultClientEndpoint();
    }
     
    @Test
    public void testSpringDefaultClientEndpoint() {
        factory = new SpringBusFactory();
        BusFactory.setDefaultBus(null);
        factory.createBus();
        System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, SpringBusFactory.class.getName());
        doTestDefaultClientEndpoint();
    }
     
    private void doTestDefaultClientEndpoint() {        

        javax.xml.ws.Service service = new SOAPService();
        Greeter greeter = service.getPort(PORT_NAME, Greeter.class);
        
        JaxWsClientProxy eih = (JaxWsClientProxy)Proxy.getInvocationHandler(greeter);
        Client client = eih.getClient();
        JaxWsEndpointImpl endpoint = (JaxWsEndpointImpl)client.getEndpoint();
        assertEquals("Unexpected bean name", PORT_NAME.toString() + ".endpoint", endpoint.getBeanName());
        assertTrue("Unexpected value for property validating", 
                   !Boolean.TRUE.equals(endpoint.get(Message.SCHEMA_VALIDATION_ENABLED)));
   
        // System.out.println("endpoint interceptors");
        List<Interceptor> interceptors = endpoint.getInInterceptors();
        printInterceptors("in", interceptors);        
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
        interceptors = endpoint.getOutInterceptors();
        printInterceptors("out", interceptors);
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
        interceptors = endpoint.getInFaultInterceptors();
        printInterceptors("inFault", interceptors);
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
        interceptors = endpoint.getOutFaultInterceptors();
        printInterceptors("outFault", interceptors);
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
        
        // System.out.println("service interceptors");
        org.apache.cxf.service.ServiceImpl svc = (org.apache.cxf.service.ServiceImpl)endpoint.getService();
        assertEquals("Unexpected bean name", SERVICE_NAME.toString(), svc.getBeanName());
        interceptors = svc.getInInterceptors();
        printInterceptors("in", interceptors);
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
        interceptors = svc.getOutInterceptors();
        printInterceptors("out", interceptors);
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
        interceptors = svc.getInFaultInterceptors();
        printInterceptors("inFault", interceptors);
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
        interceptors = svc.getOutFaultInterceptors();
        printInterceptors("outFault", interceptors);
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
    }

    @Test
    @Ignore
    public void testCXFConfiguredClientEndpoint() {
        CXFBusFactory cf = new CXFBusFactory();
        factory = cf;
        BusFactory.setDefaultBus(null);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(Configurer.USER_CFG_FILE_PROPERTY_NAME,
            "org/apache/cxf/jaxws/configured-endpoints.xml");
        BusFactory.setDefaultBus(cf.createBus(null, properties));
        System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, CXFBusFactory.class.getName());
        doTestConfiguredClientEndpoint();
    }
    
    @Test
    public void testSpringConfiguredClientEndpoint() {
        SpringBusFactory sf = new SpringBusFactory();
        factory = sf;
        BusFactory.setDefaultBus(null);
        BusFactory.setDefaultBus(sf.createBus("org/apache/cxf/jaxws/configured-endpoints.xml"));
        System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, SpringBusFactory.class.getName());
        doTestConfiguredClientEndpoint();
    }

    private void doTestConfiguredClientEndpoint() {

        javax.xml.ws.Service service = new SOAPService();
        Greeter greeter = service.getPort(PORT_NAME, Greeter.class);

        JaxWsClientProxy eih = (JaxWsClientProxy)Proxy.getInvocationHandler(greeter);
        Client client = eih.getClient();
        JaxWsEndpointImpl endpoint = (JaxWsEndpointImpl)client.getEndpoint();
//      The service shouldn't pick up the <jaxws:endpoint>...
//        assertEquals("Unexpected bean name", PORT_NAME.toString() + ".endpoint", endpoint.getBeanName());
//        // assertTrue("Unexpected value for property validating", endpoint.getValidating());
//        List<Interceptor> interceptors = endpoint.getInInterceptors();
//        assertEquals("Unexpected number of interceptors.", 1, interceptors.size());
//        assertEquals("Unexpected interceptor id.", "endpoint-in", 
//                     findTestInterceptor(interceptors).getId());
//        interceptors = endpoint.getOutInterceptors();
//        assertEquals("Unexpected number of interceptors.", 1, interceptors.size());
//        assertEquals("Unexpected interceptor id.", "endpoint-out", 
//                     findTestInterceptor(interceptors).getId());
//        interceptors = endpoint.getInFaultInterceptors();
//        assertEquals("Unexpected number of interceptors.", 1, interceptors.size());
//        assertEquals("Unexpected interceptor id.", "endpoint-in-fault", 
//                     findTestInterceptor(interceptors).getId());
//        interceptors = endpoint.getOutFaultInterceptors();
//        assertEquals("Unexpected number of interceptors.", 1, interceptors.size());
//        assertEquals("Unexpected interceptor id.", "endpoint-out-fault", 
//                     findTestInterceptor(interceptors).getId());
        
        org.apache.cxf.service.ServiceImpl svc = (org.apache.cxf.service.ServiceImpl)endpoint.getService();
        assertEquals("Unexpected bean name.", SERVICE_NAME.toString(), svc.getBeanName());
        List<Interceptor> interceptors = svc.getInInterceptors();
        assertEquals("Unexpected number of interceptors.", 1, interceptors.size());
        assertEquals("Unexpected interceptor id.", "service-in", 
                     findTestInterceptor(interceptors).getId());
        interceptors = svc.getOutInterceptors();
        assertEquals("Unexpected number of interceptors.", 1, interceptors.size());
        assertEquals("Unexpected interceptor id.", "service-out", 
                     findTestInterceptor(interceptors).getId());
        interceptors = svc.getInFaultInterceptors();
        assertEquals("Unexpected number of interceptors.", 1, interceptors.size());
        assertEquals("Unexpected interceptor id.", "service-in-fault", 
                     findTestInterceptor(interceptors).getId());
        interceptors = svc.getOutFaultInterceptors();
        assertEquals("Unexpected number of interceptors.", 1, interceptors.size());
        assertEquals("Unexpected interceptor id.", "service-out-fault", 
                     findTestInterceptor(interceptors).getId());
    }
    
    @Test
    public void testCXFDefaultServerEndpoint() {
        factory = new CXFBusFactory();
        BusFactory.setDefaultBus(null);
        factory.createBus();
        System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, CXFBusFactory.class.getName());
        initializeBus();
        doTestDefaultServerEndpoint();
    }
     
    @Test
    public void testSpringDefaultServerEndpoint() {
        factory = new SpringBusFactory();
        BusFactory.setDefaultBus(null);
        factory.createBus();
        System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, SpringBusFactory.class.getName());
        initializeBus();
        doTestDefaultServerEndpoint();
    }
     
    private void doTestDefaultServerEndpoint() {
        
        Object implementor = new GreeterImpl(); 
        EndpointImpl ei = (EndpointImpl)(javax.xml.ws.Endpoint.create(implementor));
        ei.publish("http://localhost/greeter");
        
        JaxWsEndpointImpl endpoint = (JaxWsEndpointImpl)ei.getEndpoint();
        assertEquals("Unexpected bean name", PORT_NAME.toString() + ".endpoint", endpoint.getBeanName());
        assertTrue("Unexpected value for property validating", 
                   !Boolean.TRUE.equals(endpoint.get(Message.SCHEMA_VALIDATION_ENABLED)));
   
        List<Interceptor> interceptors = endpoint.getInInterceptors();
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
        interceptors = endpoint.getOutInterceptors();
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
        interceptors = endpoint.getInFaultInterceptors();
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
        interceptors = endpoint.getOutFaultInterceptors();
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
        
        org.apache.cxf.service.ServiceImpl svc = (org.apache.cxf.service.ServiceImpl)endpoint.getService();
        assertEquals("Unexpected bean name", SERVICE_NAME.toString(), svc.getBeanName());
        interceptors = svc.getInInterceptors();
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
        interceptors = svc.getOutInterceptors();
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
        interceptors = svc.getInFaultInterceptors();
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
        interceptors = svc.getOutFaultInterceptors();
        assertNull("Unexpected test interceptor", findTestInterceptor(interceptors));
    }

    @Test
    @Ignore
    public void xtestCXFConfiguredServerEndpoint() {
        CXFBusFactory cf = new CXFBusFactory();
        factory = cf;
        BusFactory.setDefaultBus(null);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(Configurer.USER_CFG_FILE_PROPERTY_NAME,
            "org/apache/cxf/jaxws/configured-endpoints.xml");
        BusFactory.setDefaultBus(cf.createBus(null, properties));
        initializeBus();
        System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, CXFBusFactory.class.getName());
        doTestConfiguredServerEndpoint();
    }
    
    @Test
    public void testSpringConfiguredServerEndpoint() {
        SpringBusFactory sf = new SpringBusFactory();
        factory = sf;
        BusFactory.setDefaultBus(null);
        BusFactory.setDefaultBus(sf.createBus("org/apache/cxf/jaxws/configured-endpoints.xml"));
        initializeBus();
        System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, SpringBusFactory.class.getName());
        doTestConfiguredServerEndpoint();
    }
    
    private void doTestConfiguredServerEndpoint() {
        
        Object implementor = new GreeterImpl(); 
        EndpointImpl ei = (EndpointImpl)(javax.xml.ws.Endpoint.create(implementor));
        ei.publish("http://localhost/greeter");
        
        JaxWsEndpointImpl endpoint = (JaxWsEndpointImpl)ei.getEndpoint();
        assertEquals("Unexpected bean name", PORT_NAME.toString() + ".endpoint", endpoint.getBeanName());
        assertTrue("Unexpected value for property validating", 
                   Boolean.valueOf((String) ei.getProperties().get(Message.SCHEMA_VALIDATION_ENABLED)));
        List<Interceptor> interceptors = endpoint.getInInterceptors();
        assertEquals("Unexpected number of interceptors.", 4, interceptors.size());
        assertEquals("Unexpected interceptor id.", "endpoint-in", 
                     findTestInterceptor(interceptors).getId());
        interceptors = endpoint.getOutInterceptors();
        assertEquals("Unexpected number of interceptors.", 5, interceptors.size());
        assertEquals("Unexpected interceptor id.", "endpoint-out", 
                     findTestInterceptor(interceptors).getId());
        interceptors = endpoint.getInFaultInterceptors();
        assertEquals("Unexpected number of interceptors.", 2, interceptors.size());
        assertEquals("Unexpected interceptor id.", "endpoint-in-fault", 
                     findTestInterceptor(interceptors).getId());
        interceptors = endpoint.getOutFaultInterceptors();
        assertEquals("Unexpected number of interceptors.", 2, interceptors.size());
        assertEquals("Unexpected interceptor id.", "endpoint-out-fault", 
                     findTestInterceptor(interceptors).getId());
        
        org.apache.cxf.service.ServiceImpl svc = (org.apache.cxf.service.ServiceImpl)endpoint.getService();
        assertEquals("Unexpected bean name.", SERVICE_NAME.toString(), svc.getBeanName());
        interceptors = svc.getInInterceptors();
        assertEquals("Unexpected number of interceptors.", 1, interceptors.size());
        assertEquals("Unexpected interceptor id.", "service-in", 
                     findTestInterceptor(interceptors).getId());
        interceptors = svc.getOutInterceptors();
        assertEquals("Unexpected number of interceptors.", 1, interceptors.size());
        assertEquals("Unexpected interceptor id.", "service-out", 
                     findTestInterceptor(interceptors).getId());
        interceptors = svc.getInFaultInterceptors();
        assertEquals("Unexpected number of interceptors.", 1, interceptors.size());
        assertEquals("Unexpected interceptor id.", "service-in-fault", 
                     findTestInterceptor(interceptors).getId());
        interceptors = svc.getOutFaultInterceptors();
        assertEquals("Unexpected number of interceptors.", 1, interceptors.size());
        assertEquals("Unexpected interceptor id.", "service-out-fault", 
                     findTestInterceptor(interceptors).getId());
    }
      
    private void initializeBus() {
        Bus bus = BusFactory.getDefaultBus();
        
        SoapBindingFactory bindingFactory = new SoapBindingFactory();

        bus.getExtension(BindingFactoryManager.class)
            .registerBindingFactory("http://schemas.xmlsoap.org/wsdl/soap/", bindingFactory);

        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        SoapTransportFactory soapDF = new SoapTransportFactory();
        soapDF.setBus(bus);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/wsdl/soap/", soapDF);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/soap/", soapDF);

        LocalTransportFactory localTransport = new LocalTransportFactory();
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/soap/http", localTransport);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/wsdl/soap/http", localTransport);
    }
      
    
    private TestInterceptor findTestInterceptor(List<Interceptor> interceptors) {
        for (Interceptor i : interceptors) {
            if (i instanceof TestInterceptor) {
                return (TestInterceptor)i;
            }
        }
        return null;
    }
    
    
    private void printInterceptors(String type, List<Interceptor> interceptors) {
        //for (Interceptor i : interceptors) {
            //System.out.println("    " + type + ": " + i.getClass().getName());
        //}
    }
    
    
    static final class TestInterceptor extends AbstractPhaseInterceptor {
        public TestInterceptor(String name) {
            super(name, "");
        }
    
        public void handleMessage(Message message) throws Fault {
            // TODO Auto-generated method stub
        }
    }
}
