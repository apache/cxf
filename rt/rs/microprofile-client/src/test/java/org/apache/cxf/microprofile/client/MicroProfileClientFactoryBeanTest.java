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

package org.apache.cxf.microprofile.client;

import java.util.List;

import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.apache.cxf.jaxrs.client.spec.TLSConfiguration;
import org.apache.cxf.microprofile.client.mock.MyClient;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.tck.providers.TestClientRequestFilter;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MicroProfileClientFactoryBeanTest {
    @Test
    @SuppressWarnings("unchecked")
    public void testCreateClientEnabledFeature() throws Exception {
        final MicroProfileClientConfigurableImpl<RestClientBuilder> configurable = 
                new MicroProfileClientConfigurableImpl<>(RestClientBuilder.newBuilder());

        final MicroProfileClientFactoryBean bean = new MicroProfileClientFactoryBean(configurable, 
                "http://bar", MyClient.class, null, new TLSConfiguration());
        
        final SomeFeature feature = new SomeFeature(true);
        bean.setProvider(feature);

        assertTrue(bean.create() instanceof MyClient);
        assertTrue(configurable.getConfiguration().isRegistered(SomeFeature.class));
        assertTrue(configurable.getConfiguration().isRegistered(TestClientRequestFilter.class));
        assertTrue(configurable.getConfiguration().isEnabled(SomeFeature.class));
        
        assertThat((List<Object>)bean.getProviders(), hasItem(instanceOf(TestClientRequestFilter.class)));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testCreateClientDisabledFeature() throws Exception {
        final MicroProfileClientConfigurableImpl<RestClientBuilder> configurable = 
                new MicroProfileClientConfigurableImpl<>(RestClientBuilder.newBuilder());

        final MicroProfileClientFactoryBean bean = new MicroProfileClientFactoryBean(configurable, 
                "http://bar", MyClient.class, null, new TLSConfiguration());
        
        final SomeFeature feature = new SomeFeature(false);
        bean.setProvider(feature);

        assertTrue(bean.create() instanceof MyClient);
        assertTrue(configurable.getConfiguration().isRegistered(SomeFeature.class));
        assertTrue(configurable.getConfiguration().isRegistered(TestClientRequestFilter.class));
        assertFalse(configurable.getConfiguration().isEnabled(SomeFeature.class));
        
        assertThat((List<Object>)bean.getProviders(), hasItem(instanceOf(TestClientRequestFilter.class)));
    }
    
    public static class SomeFeature implements Feature {
        private final boolean enabled;
        
        public SomeFeature(boolean enabled) {
            this.enabled = enabled;
        }
        
        @Override
        public boolean configure(FeatureContext context) {
            context.register(TestClientRequestFilter.class);
            return enabled;
        }
    }
}
