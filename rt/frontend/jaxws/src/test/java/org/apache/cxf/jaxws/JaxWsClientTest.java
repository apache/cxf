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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.transport.Destination;
import org.apache.hello_world_soap_http.BadRecordLitFault;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.junit.Before;
import org.junit.Test;

public class JaxWsClientTest extends AbstractJaxWsTest {

    private final QName serviceName = new QName("http://apache.org/hello_world_soap_http",
                    "SOAPService");    
    private final QName portName = new QName("http://apache.org/hello_world_soap_http",
                    "SoapPort");
    private final String address = "http://localhost:9000/SoapContext/SoapPort";
    private Destination d;
    
    @Before
    public void setUp() throws Exception {
        super.setUpBus();

        EndpointInfo ei = new EndpointInfo(null, "http://schemas.xmlsoap.org/soap/http");
        ei.setAddress(address);

        d = localTransport.getDestination(ei);
    }

    @Test
    public void testCreate() throws Exception {
        javax.xml.ws.Service s = javax.xml.ws.Service
            .create(new QName("http://apache.org/hello_world_soap_http", "SoapPort"));
        assertNotNull(s);
        
        try {
            s = javax.xml.ws.Service.create(new URL("file:/does/not/exist.wsdl"),
                                            new QName("http://apache.org/hello_world_soap_http", "SoapPort"));
            fail("did not throw exception");
        } catch (WebServiceException sce) {
            // ignore, this is expected
        }
    }
    
    @Test
    public void testRequestContext() throws Exception {
        URL url = getClass().getResource("/wsdl/hello_world.wsdl");
        javax.xml.ws.Service s = javax.xml.ws.Service
            .create(url, serviceName);
        Greeter greeter = s.getPort(portName, Greeter.class);
        InvocationHandler handler  = Proxy.getInvocationHandler(greeter);
        BindingProvider  bp = null;
        
        if (handler instanceof BindingProvider) {
            bp = (BindingProvider)handler;
            //System.out.println(bp.toString());
            Map<String, Object> requestContext = bp.getRequestContext();
            String reqAddr = 
                (String)requestContext.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
            assertEquals("the address get from requestContext is not equal",
                         address, reqAddr);
        } else {
            fail("can't get the requset context");
        }
    }

    @Test
    public void testRequestContextPutAndRemoveEcho() throws Exception {
        URL url = getClass().getResource("/wsdl/hello_world.wsdl");
        javax.xml.ws.Service s = javax.xml.ws.Service
            .create(url, serviceName);
        Greeter greeter = s.getPort(portName, Greeter.class);
        final InvocationHandler handler  = Proxy.getInvocationHandler(greeter);
                
        Map<String, Object> requestContext = ((BindingProvider)handler).getRequestContext();
        requestContext.put(JaxWsClientProxy.THREAD_LOCAL_REQUEST_CONTEXT, Boolean.TRUE);
        
        //re-get the context so it's not a thread safe variant
        requestContext = ((BindingProvider)handler).getRequestContext();
        
        final String key = "Hi";
    
        requestContext.put(key, "ho");
        
        final Object[] result = new Object[2];
        Thread t = new Thread() {
            public void run() {
                Map<String, Object> requestContext = ((BindingProvider)handler).getRequestContext();
                result[0] = requestContext.get(key);
                requestContext.remove(key);
                result[1] = requestContext.get(key);
            }
        };
        t.start();
        t.join();
        
        assertEquals("thread sees the put", "ho", result[0]);
        assertNull("thread did not remove the put", result[1]);
        
        assertEquals("main thread does not see removal", 
                     "ho", requestContext.get(key));
    }

    @Test
    public void testEndpoint() throws Exception {
        ReflectionServiceFactoryBean bean = new JaxWsServiceFactoryBean();
        URL resource = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(resource);
        bean.setWsdlURL(resource.toString());
        bean.setBus(getBus());
        bean.setServiceClass(GreeterImpl.class);
        GreeterImpl greeter = new GreeterImpl();
        BeanInvoker invoker = new BeanInvoker(greeter);
        bean.setInvoker(invoker);

        Service service = bean.create();

        String namespace = "http://apache.org/hello_world_soap_http";
        EndpointInfo ei = service.getServiceInfos().get(0).getEndpoint(new QName(namespace, "SoapPort"));
        JaxWsEndpointImpl endpoint = new JaxWsEndpointImpl(getBus(), service, ei);

        ClientImpl client = new ClientImpl(getBus(), endpoint);

        BindingOperationInfo bop = ei.getBinding().getOperation(new QName(namespace, "sayHi"));
        assertNotNull(bop);
        bop = bop.getUnwrappedOperation();
        assertNotNull(bop);

        MessagePartInfo part = bop.getOutput().getMessageParts().get(0);
        assertEquals(0, part.getIndex());
        
        d.setMessageObserver(new MessageReplayObserver("sayHiResponse.xml"));
        Object ret[] = client.invoke(bop, new Object[] {"hi"}, null);
        assertNotNull(ret);
        assertEquals("Wrong number of return objects", 1, ret.length);

        // test fault handling
        bop = ei.getBinding().getOperation(new QName(namespace, "testDocLitFault"));
        bop = bop.getUnwrappedOperation();
        d.setMessageObserver(new MessageReplayObserver("testDocLitFault.xml"));
        try {
            client.invoke(bop, new Object[] {"BadRecordLitFault"}, null);
            fail("Should have returned a fault!");
        } catch (BadRecordLitFault fault) {
            assertEquals("foo", fault.getFaultInfo().trim());
            assertEquals("Hadrian did it.", fault.getMessage());
        }

        try {
            client.getEndpoint().getOutInterceptors().add(new NestedFaultThrower());
            client.getEndpoint().getOutInterceptors().add(new FaultThrower());
            client.invoke(bop, new Object[] {"BadRecordLitFault"}, null);
            fail("Should have returned a fault!");
        } catch (Fault fault) {
            assertEquals(true, fault.getMessage().indexOf("Foo") >= 0);
        }         
        
    }

    
    public static class NestedFaultThrower extends AbstractPhaseInterceptor<Message> {
        
        public NestedFaultThrower() {
            super(Phase.PRE_LOGICAL);
            addBefore(FaultThrower.class.getName());
        }

        public void handleMessage(Message message) throws Fault {
            boolean result = message.getInterceptorChain().doIntercept(message);
            assertEquals("doIntercept not return false", result, false);
            assertNotNull(message.getContent(Exception.class));
            throw new Fault(message.getContent(Exception.class));
        }

    }

    @Test
    public void testClientProxyFactory() {
        JaxWsProxyFactoryBean cf = new JaxWsProxyFactoryBean(); 
        cf.setAddress("http://localhost:9000/test");
        cf.setServiceClass(Greeter.class);
        Greeter greeter = (Greeter) cf.create();
        Greeter greeter2 = (Greeter) cf.create();
        Greeter greeter3 = (Greeter) cf.create();
        
        Client c = ClientProxy.getClient(greeter);
        Client c2 = ClientProxy.getClient(greeter2);
        Client c3 = ClientProxy.getClient(greeter3);
        assertNotSame(c, c2);
        assertNotSame(c, c3);
        assertNotSame(c3, c2);
        assertNotSame(c.getEndpoint(), c2.getEndpoint());
        assertNotSame(c.getEndpoint(), c3.getEndpoint());
        assertNotSame(c3.getEndpoint(), c2.getEndpoint());
        
        ((BindingProvider)greeter).getRequestContext().put("test", "manny"); 
        ((BindingProvider)greeter2).getRequestContext().put("test", "moe"); 
        ((BindingProvider)greeter3).getRequestContext().put("test", "jack");
        
        assertEquals("manny", ((BindingProvider)greeter).getRequestContext().get("test"));
        assertEquals("moe", ((BindingProvider)greeter2).getRequestContext().get("test"));
        assertEquals("jack", ((BindingProvider)greeter3).getRequestContext().get("test"));
    }
    
    
    public static class FaultThrower extends AbstractPhaseInterceptor<Message> {
        
        public FaultThrower() {
            super(Phase.PRE_LOGICAL);
        }

        public void handleMessage(Message message) throws Fault {
            throw new Fault(new org.apache.cxf.common.i18n.Message("Foo", (ResourceBundle)null));
        }

    }

}
