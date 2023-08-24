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

package org.apache.cxf.systest.jaxws;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.jaxws.JaxWsClientProxy;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.test.AbstractCXFTest;
import org.apache.hello_world_soap_http.Greeter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JaxWsClientThreadTest extends AbstractCXFTest {

    private final QName serviceName = new QName("http://apache.org/hello_world_soap_http", "SOAPService");
    private final QName portName = new QName("http://apache.org/hello_world_soap_http", "SoapPort");

    @Test
    public void testRequestContextThreadSafety() throws Throwable {

        URL url = getClass().getResource("/wsdl/hello_world.wsdl");
        jakarta.xml.ws.Service s = jakarta.xml.ws.Service.create(url, serviceName);
        final Greeter greeter = s.getPort(portName, Greeter.class);
        final InvocationHandler handler = Proxy.getInvocationHandler(greeter);

        ((BindingProvider)handler).getRequestContext().put(JaxWsClientProxy.THREAD_LOCAL_REQUEST_CONTEXT,
                                                           Boolean.TRUE);

        Map<String, Object> requestContext = ((BindingProvider)handler).getRequestContext();

        String address = (String)requestContext.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);

        final Throwable[] errorHolder = new Throwable[1];

        Runnable r = new Runnable() {
            public void run() {
                try {
                    final String protocol = "http-" + Thread.currentThread().getId();
                    for (int i = 0; i < 10; i++) {
                        String threadSpecificaddress = protocol + "://localhost:80/" + i;
                        Map<String, Object> requestContext = ((BindingProvider)handler)
                                    .getRequestContext();
                        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                           threadSpecificaddress);
                        assertEquals("we get what we set", threadSpecificaddress, requestContext
                                     .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));
                        try {
                            greeter.greetMe("Hi");
                        } catch (WebServiceException expected) {
                            //expected.getCause().printStackTrace();
                            MalformedURLException mue = (MalformedURLException)expected
                                .getCause();
                            if (mue == null || mue.getMessage() == null) {
                                throw expected;
                            }
                            assertTrue("protocol contains thread id from context", mue.getMessage()
                                .indexOf(protocol) != 0);
                        }

                        requestContext.remove(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
                        assertNull("property is null", requestContext
                                     .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));

                    }
                } catch (Throwable t) {
                    // capture assert failures
                    errorHolder[0] = t;
                }
            }
        };

        final int numThreads = 5;
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(r);
        }
        for (int i = 0; i < numThreads; i++) {
            threads[i].start();
        }
        for (int i = 0; i < numThreads; i++) {
            threads[i].join();
        }
        if (errorHolder[0] != null) {
            throw errorHolder[0];
        }

        // main thread contextValues are un changed
        assertEquals("address from existing context has not changed", address, requestContext
            .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));

        // get the latest values

        ((ClientImpl.EchoContext)((WrappedMessageContext)requestContext).getWrappedMap()).reload();
        assertNotEquals("address is different", address, requestContext
            .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));
        // verify value reflects what other threads were doing
        assertNull("property is null from last thread execution", requestContext
                   .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));
    }



    @Test
    public void testRequestContextThreadSafetyDispatch() throws Throwable {

        URL url = getClass().getResource("/wsdl/hello_world.wsdl");
        jakarta.xml.ws.Service s = jakarta.xml.ws.Service.create(url, serviceName);
        JAXBContext c = JAXBContext.newInstance(org.apache.hello_world_soap_http.types.ObjectFactory.class);
        final Dispatch<Object> disp = s.createDispatch(portName, c, Service.Mode.PAYLOAD);

        disp.getRequestContext().put(JaxWsClientProxy.THREAD_LOCAL_REQUEST_CONTEXT,
                                     Boolean.TRUE);

        Map<String, Object> requestContext = disp.getRequestContext();

        String address = (String)requestContext.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);

        final Throwable[] errorHolder = new Throwable[1];

        Runnable r = new Runnable() {
            public void run() {
                try {
                    final String protocol = "http-" + Thread.currentThread().getId();
                    for (int i = 0; i < 10; i++) {
                        String threadSpecificaddress = protocol + "://localhost:80/" + i;
                        Map<String, Object> requestContext = disp.getRequestContext();
                        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                           threadSpecificaddress);
                        assertEquals("we get what we set", threadSpecificaddress, requestContext
                                     .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));
                        try {
                            org.apache.hello_world_soap_http.types.GreetMe gm
                                = new org.apache.hello_world_soap_http.types.GreetMe();
                            gm.setRequestType("Hi");
                            disp.invoke(gm);
                        } catch (WebServiceException expected) {
                            //expected.getCause().printStackTrace();
                            MalformedURLException mue = (MalformedURLException)expected
                                .getCause();
                            if (mue == null || mue.getMessage() == null) {
                                throw expected;
                            }
                            assertTrue("protocol contains thread id from context", mue.getMessage()
                                .indexOf(protocol) != 0);
                        }

                        requestContext.remove(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
                        assertNull("property is null", requestContext
                                     .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));

                    }
                } catch (Throwable t) {
                    // capture assert failures
                    errorHolder[0] = t;
                }
            }
        };

        final int numThreads = 5;
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(r);
        }
        for (int i = 0; i < numThreads; i++) {
            threads[i].start();
        }
        for (int i = 0; i < numThreads; i++) {
            threads[i].join();
        }
        if (errorHolder[0] != null) {
            throw errorHolder[0];
        }

        // main thread contextValues are un changed
        assertEquals("address from existing context has not changed", address, requestContext
            .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));

        // get the latest values

        ((ClientImpl.EchoContext)((WrappedMessageContext)requestContext).getWrappedMap()).reload();
        assertNotEquals("address is different", address, requestContext
            .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));
        // verify value reflects what other threads were doing
        assertNull("property is null from last thread execution", requestContext
                   .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));
    }

    @Test
    public void testMultiGreeterThreadSafety() throws Throwable {

        URL url = getClass().getResource("/wsdl/hello_world.wsdl");
        final jakarta.xml.ws.Service s = jakarta.xml.ws.Service.create(url, serviceName);

        final int numThreads = 50;
        final Throwable[] errorHolder = new Throwable[numThreads];
        
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parentGroup;
        while ((parentGroup = rootGroup.getParent()) != null) {
            rootGroup = parentGroup;
        }
        int start = rootGroup.activeCount();

        Thread[] threads = new Thread[numThreads];
        for (int t = 0; t < numThreads; t++) {
            final int tid = t;
            Runnable r = new Runnable() {
                public void run() {
                    final Greeter greeter = s.getPort(portName, Greeter.class);
                    try (AutoCloseable c = (AutoCloseable)greeter) {
                        final InvocationHandler handler = Proxy.getInvocationHandler(greeter);
                        Map<String, Object> requestContext = ((BindingProvider)handler).getRequestContext();

                        final String protocol = "http-" + Thread.currentThread().getId();
                        for (int i = 0; i < 10; i++) {
                            String threadSpecificaddress = protocol + "://localhost:80/" + i;
                            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                               threadSpecificaddress);
                            assertEquals("we get what we set", threadSpecificaddress, requestContext
                                         .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));
                            try {
                                greeter.greetMe("Hi");
                            } catch (WebServiceException expected) {
                                //expected.getCause().printStackTrace();
                                MalformedURLException mue = (MalformedURLException)expected
                                    .getCause();
                                if (mue == null || mue.getMessage() == null) {
                                    throw expected;
                                }
                                assertTrue("protocol contains thread id from context", mue.getMessage()
                                    .indexOf(protocol) != 0);
                            }

                            requestContext.remove(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
                            assertNull("property is null", requestContext
                                         .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));

                        }
                    } catch (Throwable t) {
                        // capture assert failures
                        errorHolder[tid] = t;
                    }
                }
            };
            threads[t] = new Thread(r);
        }
        for (int i = 0; i < numThreads; i++) {
            threads[i].start();
        }
        for (int i = 0; i < numThreads; i++) {
            threads[i].join();
        }
        for (int i = 0; i < numThreads; i++) {
            if (errorHolder[i] != null) {
                throw errorHolder[i];
            }
        }
        
        int end = rootGroup.activeCount();
        int count = 0;
        while (end > start && count < 30) {
            Thread.sleep(100);
            System.gc();
            end = rootGroup.activeCount();
        }
        
        
        System.out.println("Start: " + start + "     End: " + end);
        // we'll allow a few extra threads to be created for various things like GC, but we
        // definitely shouldn't be anywhere near numThreads of extra threads
        assertTrue("Too many extra trheads created  " + end + "/" + start, (end - start) < 5);

    }
}
