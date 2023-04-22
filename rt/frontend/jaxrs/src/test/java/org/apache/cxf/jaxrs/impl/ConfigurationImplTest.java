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

package org.apache.cxf.jaxrs.impl;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Configurable;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.MessageBodyReader;
import org.apache.cxf.common.logging.LogUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConfigurationImplTest {

    @Test
    public void testIsRegistered() throws Exception {
//        ConfigurationImpl c = new ConfigurationImpl(RuntimeType.SERVER);
//        ContainerResponseFilter filter = new ContainerResponseFilterImpl();
//        assertTrue(c.register(filter,
//                              Collections.<Class<?>, Integer>singletonMap(ContainerResponseFilter.class, 1000)));
//        assertTrue(c.isRegistered(filter));
//        assertFalse(c.isRegistered(new ContainerResponseFilterImpl()));
//        assertTrue(c.isRegistered(ContainerResponseFilterImpl.class));
//        assertFalse(c.isRegistered(ContainerResponseFilter.class));
//        assertFalse(c.register(filter,
//                               Collections.<Class<?>, Integer>singletonMap(ContainerResponseFilter.class, 1000)));
//        assertFalse(c.register(ContainerResponseFilterImpl.class,
//                               Collections.<Class<?>, Integer>singletonMap(ContainerResponseFilter.class, 1000)));
        doTestIsFilterRegistered(new ContainerResponseFilterImpl(), ContainerResponseFilterImpl.class);
    }
    
    @Test
    public void testIsRegisteredSubClass() throws Exception {
        doTestIsFilterRegistered(new ContainerResponseFilterSubClassImpl(), ContainerResponseFilterSubClassImpl.class);
    }

    private void doTestIsFilterRegistered(Object provider, Class<?> providerClass) throws Exception {
        ConfigurationImpl c = new ConfigurationImpl(RuntimeType.SERVER);
        assertTrue(c.register(provider,
                              Collections.<Class<?>, Integer>singletonMap(ContainerResponseFilter.class, 1000)));
        assertTrue(c.isRegistered(provider));
        assertFalse(c.isRegistered(providerClass.getDeclaredConstructor().newInstance()));
        assertTrue(c.isRegistered(providerClass));
        assertFalse(c.isRegistered(ContainerResponseFilter.class));
        assertFalse(c.register(provider,
                               Collections.<Class<?>, Integer>singletonMap(ContainerResponseFilter.class, 1000)));
        assertFalse(c.register(providerClass,
                               Collections.<Class<?>, Integer>singletonMap(ContainerResponseFilter.class, 1000)));
    }

    @ConstrainedTo(RuntimeType.SERVER)
    public static class ContainerResponseFilterImpl implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        }

    }

    public static class ContainerResponseFilterSubClassImpl extends ContainerResponseFilterImpl { }

    @ConstrainedTo(RuntimeType.CLIENT)
    public static class ClientResponseFilterImpl implements ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
            throws IOException {
        }

    }
    static class TestHandler extends Handler {

        List<String> messages = new ArrayList<>();

        /** {@inheritDoc}*/
        @Override
        public void publish(LogRecord record) {
            messages.add(record.getLevel().toString() + ": " + record.getMessage());
        }

        /** {@inheritDoc}*/
        @Override
        public void flush() {
            // no-op
        }

        /** {@inheritDoc}*/
        @Override
        public void close() throws SecurityException {
            // no-op
        }
    }

    @Test
    public void testInvalidContract() {
        TestHandler handler = new TestHandler();
        LogUtils.getL7dLogger(ConfigurationImpl.class).addHandler(handler);

        ConfigurationImpl c = new ConfigurationImpl(RuntimeType.SERVER);
        ContainerResponseFilter filter = new ContainerResponseFilterImpl();
        assertFalse(c.register(filter,
                               Collections.<Class<?>, Integer>singletonMap(MessageBodyReader.class, 1000)));

        for (String message : handler.messages) {
            if (message.startsWith("WARN") && message.contains("does not implement specified contract")) {
                return; // success
            }
        }
        fail("did not log expected message");
    }

    public static class TestFilter implements ContainerRequestFilter, ContainerResponseFilter, 
    ClientRequestFilter, ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext paramClientRequestContext,
                           ClientResponseContext paramClientResponseContext)
                               throws IOException {
            // no-op
        }

        @Override
        public void filter(ClientRequestContext paramClientRequestContext) throws IOException {
            // no-op
        }

        @Override
        public void filter(ContainerRequestContext paramContainerRequestContext,
                           ContainerResponseContext paramContainerResponseContext)
                               throws IOException {
            // no-op
        }

        @Override
        public void filter(ContainerRequestContext paramContainerRequestContext) throws IOException {
            // no-op
        }
    }

    public interface MyClientFilter extends ClientRequestFilter, ClientResponseFilter {
        // reduced to just the intermediate layer. Could contain user code
    }

    public static class NestedInterfaceTestFilter implements MyClientFilter {

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            // no-op
        }

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
                throws IOException {
            // no-op
        }
    }

    private Client createClientProxy() {
        return (Client) Proxy.newProxyInstance(this.getClass().getClassLoader(), 
            new Class<?>[]{Client.class},
            new InvocationHandler() {

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    return null; //no-op
                } });
    }

    @Test
    public void testSubClassIsRegisteredOnConfigurable() {
        FeatureContextImpl featureContext = new FeatureContextImpl();
        Configurable<FeatureContext> configurable = new ConfigurableImpl<>(featureContext, RuntimeType.SERVER);
        featureContext.setConfigurable(configurable);
        featureContext.register(ContainerResponseFilterSubClassImpl.class);
        Configuration config = configurable.getConfiguration();
        Map<Class<?>, Integer> contracts = config.getContracts(ContainerResponseFilter.class);
        assertEquals(1, contracts.size());
        assertTrue(contracts.containsKey(ContainerResponseFilter.class));
    }
    
    @Test
    public void testServerFilterContractsOnClientIsRejected() {
        try (ConfigurableImpl<Client> configurable 
                = new ConfigurableImpl<>(createClientProxy(), RuntimeType.CLIENT)) {
            Configuration config = configurable.getConfiguration();
            configurable.register(TestFilter.class);
            Map<Class<?>, Integer> contracts = config.getContracts(TestFilter.class);
            assertTrue(contracts.containsKey(ClientRequestFilter.class));
            assertTrue(contracts.containsKey(ClientResponseFilter.class));
            assertFalse(contracts.containsKey(ContainerRequestFilter.class));
            assertFalse(contracts.containsKey(ContainerResponseFilter.class));
        }
    }

    @Test
    public void testClientFilterWithNestedInterfacesIsAccepted() {
        try (ConfigurableImpl<Client> configurable 
                = new ConfigurableImpl<>(createClientProxy(), RuntimeType.CLIENT)) {
            Configuration config = configurable.getConfiguration();
            configurable.register(NestedInterfaceTestFilter.class);
            Map<Class<?>, Integer> contracts = config.getContracts(NestedInterfaceTestFilter.class);
            assertTrue(contracts.containsKey(ClientRequestFilter.class));
            assertTrue(contracts.containsKey(ClientResponseFilter.class));
        }
    }

    @Test
    public void testClientFilterContractsOnServerFeatureIsRejected() {
        FeatureContextImpl featureContext = new FeatureContextImpl();
        Configurable<FeatureContext> configurable = new ConfigurableImpl<>(featureContext, RuntimeType.SERVER);
        featureContext.setConfigurable(configurable);
        featureContext.register(TestFilter.class);
        Configuration config = configurable.getConfiguration();
        Map<Class<?>, Integer> contracts = config.getContracts(TestFilter.class);
        assertFalse(contracts.containsKey(ClientRequestFilter.class));
        assertFalse(contracts.containsKey(ClientResponseFilter.class));
        assertTrue(contracts.containsKey(ContainerRequestFilter.class));
        assertTrue(contracts.containsKey(ContainerResponseFilter.class));
    }

    public static class DisablableFeature implements Feature {

        boolean enabled;

        /** {@inheritDoc}*/
        @Override
        public boolean configure(FeatureContext context) {
            return enabled;
        }

    }

    @Test
    public void testFeatureDisabledClass() {
        FeatureContextImpl featureContext = new FeatureContextImpl();
        Configurable<FeatureContext> configurable = new ConfigurableImpl<>(featureContext, RuntimeType.SERVER);
        featureContext.setConfigurable(configurable);
        featureContext.register(DisablableFeature.class);

        Configuration config = configurable.getConfiguration();
        assertFalse(config.isEnabled(DisablableFeature.class));
    }

    @Test
    public void testFeatureDisabledInstance() {
        FeatureContextImpl featureContext = new FeatureContextImpl();
        Configurable<FeatureContext> configurable = new ConfigurableImpl<>(featureContext, RuntimeType.SERVER);
        featureContext.setConfigurable(configurable);
        Feature feature = new DisablableFeature();
        featureContext.register(feature);

        Configuration config = configurable.getConfiguration();
        assertFalse(config.isEnabled(feature));
    }

    @Test 
    public void testIsEnabledWithMultipleFeaturesOfSameType() {
        FeatureContextImpl featureContext = new FeatureContextImpl();
        Configurable<FeatureContext> configurable = new ConfigurableImpl<>(featureContext, RuntimeType.SERVER);
        featureContext.setConfigurable(configurable);

        featureContext.register(new DisablableFeature());
        featureContext.register(new DisablableFeature());
        featureContext.register(new DisablableFeature());

        Configuration config = configurable.getConfiguration();
        assertEquals(3, config.getInstances().size());
        assertFalse(config.isEnabled(DisablableFeature.class));

        DisablableFeature enabledFeature = new DisablableFeature();
        enabledFeature.enabled = true;

        featureContext.register(enabledFeature);
        assertEquals(4, config.getInstances().size());
        assertTrue(config.isEnabled(DisablableFeature.class));

        featureContext.register(new DisablableFeature());
        assertEquals(5, config.getInstances().size());
        assertTrue(config.isEnabled(DisablableFeature.class));
    }

    @ConstrainedTo(RuntimeType.SERVER)
    public static class ClientFilterConstrainedToServer implements ClientRequestFilter {

        /** {@inheritDoc}*/
        @Override
        public void filter(ClientRequestContext paramClientRequestContext) throws IOException {
            // no-op
        }
        
    }

    @Test
    public void testInvalidConstraintOnProvider() {
        TestHandler handler = new TestHandler();
        LogUtils.getL7dLogger(ConfigurableImpl.class).addHandler(handler);

        try (ConfigurableImpl<Client> configurable 
                = new ConfigurableImpl<>(createClientProxy(), RuntimeType.CLIENT)) {
            Configuration config = configurable.getConfiguration();
    
            configurable.register(ClientFilterConstrainedToServer.class);
    
            assertEquals(0, config.getInstances().size());
    
            for (String message : handler.messages) {
                if (message.startsWith("WARN") && message.contains("cannot be registered in ")) {
                    return; // success
                }
            }
        }
        fail("did not log expected message");
    }

    
    @Test
    public void testChecksConstrainedToAnnotationDuringRegistration() {
        TestHandler handler = new TestHandler();
        LogUtils.getL7dLogger(ConfigurableImpl.class).addHandler(handler);

        try (ConfigurableImpl<Client> configurable 
                = new ConfigurableImpl<>(createClientProxy(), RuntimeType.CLIENT)) {
            Configuration config = configurable.getConfiguration();
    
            configurable.register(ContainerResponseFilterImpl.class);
    
            assertEquals(0, config.getInstances().size());
    
            for (String message : handler.messages) {
                if (message.startsWith("WARN") && message.contains("Null, empty or invalid contracts specified")) {
                    return; // success
                }
            }
        }
        fail("did not log expected message");
    }
}