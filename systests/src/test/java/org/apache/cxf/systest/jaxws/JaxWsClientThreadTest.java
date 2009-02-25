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
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.jaxws.JaxWsClientProxy;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.test.AbstractCXFTest;
import org.apache.hello_world_soap_http.Greeter;
import org.junit.Test;

public class JaxWsClientThreadTest extends AbstractCXFTest {

    private final QName serviceName = new QName("http://apache.org/hello_world_soap_http", "SOAPService");
    private final QName portName = new QName("http://apache.org/hello_world_soap_http", "SoapPort");
    
    @Test
    public void testRequestContextThreadSafety() throws Throwable {

        URL url = getClass().getResource("/wsdl/hello_world.wsdl");
        javax.xml.ws.Service s = javax.xml.ws.Service.create(url, serviceName);
        final Greeter greeter = s.getPort(portName, Greeter.class);
        final InvocationHandler handler = Proxy.getInvocationHandler(greeter);

        ((BindingProvider)handler).getRequestContext().put(JaxWsClientProxy.THREAD_LOCAL_REQUEST_CONTEXT,
                                                           Boolean.TRUE);
        
        Map<String, Object> requestContext = ((BindingProvider)handler).getRequestContext();
        
        String address = (String)requestContext.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);

        final Throwable errorHolder[] = new Throwable[1];

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
                        } catch (SOAPFaultException expected) {
                            //expected.getCause().printStackTrace();
                            MalformedURLException mue = (MalformedURLException)expected
                                .getCause().getCause();
                            if (mue == null || mue.getMessage() == null) {
                                throw expected;
                            }
                            assertTrue("protocol contains thread id from context", mue.getMessage()
                                .indexOf(protocol) != 0);
                        }
                        
                        requestContext.remove(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
                        assertTrue("property is null", requestContext
                                     .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY) == null);
                        
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
        assertTrue("address from existing context has not changed", address.equals(requestContext
            .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY)));

        // get the latest values
        
        ((ClientImpl.EchoContext)((WrappedMessageContext)requestContext).getWrappedMap()).reload();
        assertTrue("address is different", !address.equals(requestContext
            .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY)));
        // verify value reflects what other threads were doing
        assertTrue("property is null from last thread execution", requestContext
                   .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY) == null);
    }
}

