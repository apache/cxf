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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.LogicalHandler;
import jakarta.xml.ws.handler.LogicalMessageContext;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;
import org.apache.hello_world_soap_http.BadRecordLitFault;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.GreeterImpl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JaxWsClientTest extends AbstractJaxWsTest {

    private static final QName SERVICE_NAME = new QName("http://apache.org/hello_world_soap_http",
                    "SOAPService");
    private static final QName PORT_NAME = new QName("http://apache.org/hello_world_soap_http",
                    "SoapPort");
    private static final String ADDRESS = "http://localhost:9000/SoapContext/SoapPort";

    private Destination d;
    private Map<String, List<String>> headers = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        super.setUpBus();

        EndpointInfo ei = new EndpointInfo(null, "http://schemas.xmlsoap.org/soap/http");
        ei.setAddress(ADDRESS);

        d = localTransport.getDestination(ei, bus);
    }

    @Test
    public void testCreate() throws Exception {
        jakarta.xml.ws.Service s = jakarta.xml.ws.Service
            .create(new QName("http://apache.org/hello_world_soap_http", "SoapPort"));
        assertNotNull(s);

        try {
            jakarta.xml.ws.Service.create(new URL("file:/does/not/exist.wsdl"),
                                            new QName("http://apache.org/hello_world_soap_http", "SoapPort"));
            fail("did not throw exception");
        } catch (WebServiceException sce) {
            // ignore, this is expected
        }
    }

    @Test
    public void testRequestContext() throws Exception {
        URL url = getClass().getResource("/wsdl/hello_world.wsdl");
        jakarta.xml.ws.Service s = jakarta.xml.ws.Service
            .create(url, SERVICE_NAME);
        Greeter greeter = s.getPort(PORT_NAME, Greeter.class);
        InvocationHandler handler = Proxy.getInvocationHandler(greeter);

        if (handler instanceof BindingProvider) {
            BindingProvider bp = (BindingProvider)handler;
            //System.out.println(bp.toString());
            Map<String, Object> requestContext = bp.getRequestContext();
            String reqAddr =
                (String)requestContext.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
            assertEquals("the address get from requestContext is not equal",
                         ADDRESS, reqAddr);
        } else {
            fail("can't get the requset context");
        }
    }

    @Test
    public void testRequestContextPutAndRemoveEcho() throws Exception {
        URL url = getClass().getResource("/wsdl/hello_world.wsdl");
        jakarta.xml.ws.Service s = jakarta.xml.ws.Service
            .create(url, SERVICE_NAME);
        final Greeter handler = s.getPort(PORT_NAME, Greeter.class);

        Map<String, Object> requestContext = ((BindingProvider)handler).getRequestContext();
        requestContext.put(JaxWsClientProxy.THREAD_LOCAL_REQUEST_CONTEXT, Boolean.TRUE);

        // future calls to getRequestContext() will use a thread local request context.
        // That allows the request context to be threadsafe.
        requestContext = ((BindingProvider)handler).getRequestContext();

        final String key = "Hi";

        requestContext.put(key, "ho");

        final Object[] result = new Object[2];
        Thread t = new Thread() {
            public void run() {
                //requestContext in main thread shouldn't affect the requestContext in this thread
                Map<String, Object> requestContext = ((BindingProvider)handler).getRequestContext();
                result[0] = requestContext.get(key);
                requestContext.remove(key);
                result[1] = requestContext.get(key);
            }
        };
        t.start();
        t.join();

        assertNull("thread shouldn't see the put", result[0]);
        assertNull("thread did not remove the put", result[1]);

        assertEquals("main thread does not see removal",
                     "ho", requestContext.get(key));
    }
    @Test
    public void testRequestContextPutAndRemoveEchoDispatch() throws Exception {
        URL url = getClass().getResource("/wsdl/hello_world.wsdl");
        jakarta.xml.ws.Service s = jakarta.xml.ws.Service
            .create(url, SERVICE_NAME);

        final Dispatch<DOMSource> disp = s.createDispatch(PORT_NAME, DOMSource.class,
                                                    jakarta.xml.ws.Service.Mode.PAYLOAD);


        Map<String, Object> requestContext = disp.getRequestContext();
        requestContext.put(JaxWsClientProxy.THREAD_LOCAL_REQUEST_CONTEXT, Boolean.TRUE);

        // future calls to getRequestContext() will use a thread local request context.
        // That allows the request context to be threadsafe.
        requestContext = disp.getRequestContext();

        final String key = "Hi";

        requestContext.put(key, "ho");

        final Object[] result = new Object[2];
        Thread t = new Thread() {
            public void run() {
                //requestContext in main thread shouldn't affect the requestContext in this thread
                Map<String, Object> requestContext = disp.getRequestContext();
                result[0] = requestContext.get(key);
                requestContext.remove(key);
                result[1] = requestContext.get(key);
            }
        };
        t.start();
        t.join();

        assertNull("thread shouldn't see the put", result[0]);
        assertNull("thread did not remove the put", result[1]);

        assertEquals("main thread does not see removal",
                     "ho", requestContext.get(key));
    }

    @Test
    public void testThreadLocalRequestContextIsIsolated() throws InterruptedException {
        URL url = getClass().getResource("/wsdl/hello_world.wsdl");
        jakarta.xml.ws.Service s = jakarta.xml.ws.Service.create(url, SERVICE_NAME);
        final Greeter handler = s.getPort(PORT_NAME, Greeter.class);
        final AtomicBoolean isPropertyAPresent = new AtomicBoolean(false);
        // Makes request context thread local
        ClientProxy.getClient(handler).setThreadLocalRequestContext(true);
        // PropertyA should be added to the request context of current thread only
        ClientProxy.getClient(handler).getRequestContext().put("PropertyA", "PropertyAVal");
        Runnable checkRequestContext = new Runnable() {
            @Override
            public void run() {
                if (ClientProxy.getClient(handler).getRequestContext().containsKey("PropertyA")) {
                    isPropertyAPresent.set(true);
                }
            }
        };
        Thread thread = new Thread(checkRequestContext);
        thread.start();
        thread.join(60000);

        assertFalse("If we have per thread isolation propertyA should be not present in the context of another thread.",
                    isPropertyAPresent.get());
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
        Object[] ret = client.invoke(bop, new Object[] {"hi"}, null);
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
            assertTrue(fault.getMessage().indexOf("Foo") >= 0);
        }
        client.close();

    }


    public static class NestedFaultThrower extends AbstractPhaseInterceptor<Message> {

        public NestedFaultThrower() {
            super(Phase.PRE_LOGICAL);
            addBefore(FaultThrower.class.getName());
        }

        public void handleMessage(Message message) throws Fault {
            boolean result = message.getInterceptorChain().doIntercept(message);
            assertFalse("doIntercept not return false", result);
            assertNotNull(message.getContent(Exception.class));
            throw new Fault(message.getContent(Exception.class));
        }

    }

    @Test
    public void testClientProxyFactory() {
        JaxWsProxyFactoryBean cf = new JaxWsProxyFactoryBean();
        cf.setAddress("http://localhost:9000/test");
        Greeter greeter = cf.create(Greeter.class);
        /*  .n.b. don't call call create with an argument and change the SEI. */
        Greeter greeter2 = (Greeter) cf.create();
        Greeter greeter3 = (Greeter) cf.create();

        Client c = (Client)greeter;
        Client c2 = (Client)greeter2;
        Client c3 = (Client)greeter3;
        assertNotSame(c, c2);
        assertNotSame(c, c3);
        assertNotSame(c3, c2);
        assertNotSame(c.getEndpoint(), c2.getEndpoint());
        assertNotSame(c.getEndpoint(), c3.getEndpoint());
        assertNotSame(c3.getEndpoint(), c2.getEndpoint());

        c3.getInInterceptors();

        ((BindingProvider)greeter).getRequestContext().put("test", "manny");
        ((BindingProvider)greeter2).getRequestContext().put("test", "moe");
        ((BindingProvider)greeter3).getRequestContext().put("test", "jack");

        assertEquals("manny", ((BindingProvider)greeter).getRequestContext().get("test"));
        assertEquals("moe", ((BindingProvider)greeter2).getRequestContext().get("test"));
        assertEquals("jack", ((BindingProvider)greeter3).getRequestContext().get("test"));
    }

    @Test
    public void testLogicalHandler() {
        URL url = getClass().getResource("/wsdl/hello_world.wsdl");
        jakarta.xml.ws.Service s = jakarta.xml.ws.Service
            .create(url, SERVICE_NAME);
        Greeter greeter = s.getPort(PORT_NAME, Greeter.class);
        d.setMessageObserver(new MessageReplayObserver("sayHiResponse.xml"));

        @SuppressWarnings("rawtypes") // JAX-WS api doesn't specify this as List<Handler<? extends MessageContext>>
        List<Handler> chain = ((BindingProvider)greeter).getBinding().getHandlerChain();
        chain.add(new LogicalHandler<LogicalMessageContext>() {
            public void close(MessageContext arg0) {
            }

            public boolean handleFault(LogicalMessageContext arg0) {
                return true;
            }

            public boolean handleMessage(LogicalMessageContext context) {

                Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
                if (outbound) {
                    headers = CastUtils.cast((Map<?, ?>) context.get(MessageContext.HTTP_REQUEST_HEADERS));
                    if (headers == null) {
                        headers = new HashMap<>();
                        context.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
                    }
                    headers.put("My-Custom-Header", Collections.singletonList("value"));
                }
                return true;
            }
        });
        ((BindingProvider)greeter).getBinding().setHandlerChain(chain);

        String response = greeter.sayHi();
        assertNotNull(response);
        assertTrue("custom header should be present", headers.containsKey("My-Custom-Header"));
        assertTrue("existing SOAPAction header should not be removed", headers.containsKey("SOAPAction"));
    }

    @Test
    public void testSoapHandler() {
        URL url = getClass().getResource("/wsdl/hello_world.wsdl");
        jakarta.xml.ws.Service s = jakarta.xml.ws.Service
            .create(url, SERVICE_NAME);
        Greeter greeter = s.getPort(PORT_NAME, Greeter.class);
        d.setMessageObserver(new MessageReplayObserver("sayHiResponse.xml"));

        @SuppressWarnings("rawtypes")
        List<Handler> chain = ((BindingProvider)greeter).getBinding().getHandlerChain();
        chain.add(new SOAPHandler<SOAPMessageContext>() {

                public boolean handleMessage(SOAPMessageContext context) {

                    Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
                    if (outbound) {
                        headers = CastUtils.cast((Map<?, ?>) context.get(MessageContext.HTTP_REQUEST_HEADERS));
                        if (headers == null) {
                            headers = new HashMap<>();
                            context.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
                        }
                        headers.put("My-Custom-Header", Collections.singletonList("value"));
                    }
                    return true;
                }

                public boolean handleFault(SOAPMessageContext smc) {
                    return true;
                }

                public Set<QName> getHeaders() {
                    return null;
                }

                public void close(MessageContext messageContext) {
                }
        });
        ((BindingProvider)greeter).getBinding().setHandlerChain(chain);

        String response = greeter.sayHi();
        assertNotNull(response);
        assertTrue("custom header should be present", headers.containsKey("My-Custom-Header"));
        assertTrue("existing SOAPAction header should not be removed", headers.containsKey("SOAPAction"));

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
