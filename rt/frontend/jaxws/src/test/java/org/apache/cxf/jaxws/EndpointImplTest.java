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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.xml.transform.Source;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.service.Hello;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.apache.hello_world_soap_http.HelloImpl;
import org.apache.hello_world_soap_http.HelloWrongAnnotation;
import org.junit.Test;

public class EndpointImplTest extends AbstractJaxWsTest {

    
    @Override
    protected Bus createBus() throws BusException {
        return BusFactory.getDefaultBus();
    }


    @Test
    public void testEndpoint() throws Exception {
        String address = "http://localhost:8080/test";
        
        GreeterImpl greeter = new GreeterImpl();
        EndpointImpl endpoint = new EndpointImpl(getBus(), greeter, (String)null);
 
        WebServiceContext ctx = greeter.getContext();
        assertNull(ctx);
        try {
            endpoint.publish(address);
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getCause() instanceof BusException);
            assertEquals("BINDING_INCOMPATIBLE_ADDRESS_EXC", ((BusException)ex.getCause()).getCode());
        }
        ctx = greeter.getContext();
        
        assertNotNull(ctx);
        
        // Test that we can't change settings through the JAX-WS API after publishing
        
        try {
            endpoint.publish(address);
            fail("republished an already published endpoint.");
        } catch (IllegalStateException e) {
            // expected
        }
        
        try {
            endpoint.setMetadata(new ArrayList<Source>(0));
            fail("set metadata on an already published endpoint.");
        } catch (IllegalStateException e) {
            // expected
        }
    }
    
    @Test
    public void testEndpointStop() throws Exception {   
        String address = "http://localhost:8080/test";
        
        GreeterImpl greeter = new GreeterImpl();
        EndpointImpl endpoint = new EndpointImpl(getBus(), greeter, (String)null);
 
        WebServiceContext ctx = greeter.getContext();
        assertNull(ctx);
        try {
            endpoint.publish(address);
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getCause() instanceof BusException);
            assertEquals("BINDING_INCOMPATIBLE_ADDRESS_EXC", ((BusException)ex.getCause()).getCode());
        }
        ctx = greeter.getContext();
        
        assertNotNull(ctx);
        
        // Test that calling stop on the Endpoint works
        assertTrue(endpoint.isPublished());
        endpoint.stop();
        assertFalse(endpoint.isPublished());
        
        // Test that the Endpoint cannot be restarted.
        try {
            endpoint.publish(address);
            fail("stopped endpoint restarted.");
        } catch (IllegalStateException e) {
            // expected.
        }
        
    }
    

    @Test
    public void testEndpointServiceConstructor() throws Exception {   
        GreeterImpl greeter = new GreeterImpl();
        JaxWsServiceFactoryBean serviceFactory = new JaxWsServiceFactoryBean();
        serviceFactory.setBus(getBus());
        serviceFactory.setInvoker(new BeanInvoker(greeter));
        serviceFactory.setServiceClass(GreeterImpl.class);
        
        EndpointImpl endpoint = new EndpointImpl(getBus(), greeter, 
                                                 new JaxWsServerFactoryBean(serviceFactory));
 
        WebServiceContext ctx = greeter.getContext();
        assertNull(ctx);
        try {
            String address = "http://localhost:8080/test";
            endpoint.publish(address);
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getCause() instanceof BusException);
            assertEquals("BINDING_INCOMPATIBLE_ADDRESS_EXC", ((BusException)ex.getCause()).getCode());
        }
        ctx = greeter.getContext();
        
        assertNotNull(ctx);
    }
    
    @Test
    public void testWSAnnoWithoutWSDLLocationInSEI() throws Exception {
        HelloImpl hello = new HelloImpl();
        JaxWsServiceFactoryBean serviceFactory = new JaxWsServiceFactoryBean();
        serviceFactory.setBus(getBus());
        serviceFactory.setInvoker(new BeanInvoker(hello));
        serviceFactory.setServiceClass(HelloImpl.class);
        
        EndpointImpl endpoint = new EndpointImpl(getBus(), hello, 
                                                 new JaxWsServerFactoryBean(serviceFactory));

        try {
            String address = "http://localhost:8080/test";
            endpoint.publish(address);
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getCause() instanceof BusException);
            assertEquals("BINDING_INCOMPATIBLE_ADDRESS_EXC", ((BusException)ex.getCause()).getCode());
        }
    }
    
    @Test
    public void testSOAPBindingOnMethodWithRPC() {
        HelloWrongAnnotation hello = new HelloWrongAnnotation();
        JaxWsServiceFactoryBean serviceFactory = new JaxWsServiceFactoryBean();
        serviceFactory.setBus(getBus());
        serviceFactory.setInvoker(new BeanInvoker(hello));
        serviceFactory.setServiceClass(HelloWrongAnnotation.class);
        
        try {
            new EndpointImpl(getBus(), hello, new JaxWsServerFactoryBean(serviceFactory));
        } catch (Exception e) {
            String expeced = "Method [sayHi] processing error: SOAPBinding can not on method with RPC style";
            assertEquals(expeced, e.getMessage());
        }
    }
    
    @Test
    public void testPublishEndpointPermission() throws Exception {
        Hello service = new Hello();
        EndpointImpl ep = new EndpointImpl(getBus(), service, (String) null);

        System.setProperty(EndpointImpl.CHECK_PUBLISH_ENDPOINT_PERMISSON_PROPERTY, "true");

        try {
            ep.publish("local://localhost:9090/hello");
            fail("Did not throw exception as expected");
        } catch (SecurityException e) {
            // that's expected
        } finally {
            System.setProperty(EndpointImpl.CHECK_PUBLISH_ENDPOINT_PERMISSON_PROPERTY, "false");
        }
        
        ep.publish("local://localhost:9090/hello");
    }

    @Test
    public void testAddWSAFeature() throws Exception {
        GreeterImpl greeter = new GreeterImpl();
        JaxWsServiceFactoryBean serviceFactory = new JaxWsServiceFactoryBean();
        serviceFactory.setBus(getBus());
        serviceFactory.setInvoker(new BeanInvoker(greeter));
        serviceFactory.setServiceClass(GreeterImpl.class);
        
        EndpointImpl endpoint = new EndpointImpl(getBus(), greeter, 
                                                 new JaxWsServerFactoryBean(serviceFactory));

        endpoint.getFeatures().add(new WSAddressingFeature());
        try {
            String address = "http://localhost:8080/test";
            endpoint.publish(address);
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getCause() instanceof BusException);
            assertEquals("BINDING_INCOMPATIBLE_ADDRESS_EXC", ((BusException)ex.getCause()).getCode());
        }
 
        assertTrue(serviceFactory.getFeatures().size() == 1);
        assertTrue(serviceFactory.getFeatures().get(0) instanceof WSAddressingFeature);
    }

    @Test
    public void testJaxWsaFeature() throws Exception {
        HelloWsa greeter = new HelloWsa();
        JaxWsServiceFactoryBean serviceFactory = new JaxWsServiceFactoryBean();
        serviceFactory.setBus(getBus());
        serviceFactory.setInvoker(new BeanInvoker(greeter));
        serviceFactory.setServiceClass(HelloWsa.class);

        EndpointImpl endpoint = new EndpointImpl(getBus(), greeter, 
                                                 new JaxWsServerFactoryBean(serviceFactory));
        try {
            String address = "http://localhost:8080/test";
            endpoint.publish(address);
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getCause() instanceof BusException);
            assertEquals("BINDING_INCOMPATIBLE_ADDRESS_EXC", ((BusException)ex.getCause()).getCode());
        }
 
        assertTrue(serviceFactory.getFeatures().size() == 1);
        assertTrue(serviceFactory.getFeatures().get(0) instanceof WSAddressingFeature);
    }

    static class EchoObserver implements MessageObserver {

        public void onMessage(Message message) {
            try {
                Conduit backChannel = message.getDestination().getBackChannel(message, null, null);

                backChannel.prepare(message);

                OutputStream out = message.getContent(OutputStream.class);
                assertNotNull(out);
                InputStream in = message.getContent(InputStream.class);
                assertNotNull(in);
                
                copy(in, out, 2045);

                out.close();
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void copy(final InputStream input, final OutputStream output, final int bufferSize)
        throws IOException {
        try {
            final byte[] buffer = new byte[bufferSize];

            int n = input.read(buffer);
            while (-1 != n) {
                output.write(buffer, 0, n);
                n = input.read(buffer);
            }
        } finally {
            input.close();
            output.close();
        }
    }
}
