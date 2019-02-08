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
package org.apache.cxf.microprofile.client.cdi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Priorities;
import javax.ws.rs.Produces;

import org.apache.cxf.microprofile.client.mock.HighPriorityClientReqFilter;
import org.apache.cxf.microprofile.client.mock.InvokedMethodClientRequestFilter;
import org.apache.cxf.microprofile.client.mock.LowPriorityClientReqFilter;
import org.apache.cxf.microprofile.client.mock.MockConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.rest.client.tck.interfaces.InterfaceWithoutProvidersDefined;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RestClientCdiTest {

    @Test
    public void testProvidersRegisteredViaMPConfigProperty() throws Exception {
        Map<String, String> configValues = new HashMap<>();
        configValues.put(InterfaceWithoutProvidersDefined.class.getName() + "/mp-rest/providers",
                         HighPriorityClientReqFilter.class.getName() + ","
                         + LowPriorityClientReqFilter.class.getName() + ","
                         + InvokedMethodClientRequestFilter.class.getName());
        configValues.put(InterfaceWithoutProvidersDefined.class.getName() + "/mp-rest/providers/" 
                         + LowPriorityClientReqFilter.class.getName() + "/priority", "3");
        ((MockConfigProviderResolver)ConfigProviderResolver.instance()).setConfigValues(configValues);

        IMocksControl control = EasyMock.createNiceControl();
        BeanManager mockedBeanMgr = control.createMock(BeanManager.class);
        mockedBeanMgr.isScope(Path.class);
        EasyMock.expectLastCall().andReturn(false);
        mockedBeanMgr.isScope(Produces.class);
        EasyMock.expectLastCall().andReturn(false);
        mockedBeanMgr.isScope(Consumes.class);
        EasyMock.expectLastCall().andReturn(false);
        control.replay();

        RestClientBean bean = new RestClientBean(InterfaceWithoutProvidersDefined.class, mockedBeanMgr);
        List<Class<?>> registeredProviders = bean.getConfiguredProviders();
        assertEquals(3, registeredProviders.size());
        assertTrue(registeredProviders.contains(HighPriorityClientReqFilter.class));
        assertTrue(registeredProviders.contains(LowPriorityClientReqFilter.class));
        assertTrue(registeredProviders.contains(InvokedMethodClientRequestFilter.class));

        Map<Class<?>, Integer> priorities = bean.getConfiguredProviderPriorities(registeredProviders);
        assertEquals(3, priorities.size());
        assertEquals(3, (int) priorities.get(LowPriorityClientReqFilter.class));
        assertEquals(10, (int) priorities.get(HighPriorityClientReqFilter.class));
        assertEquals(Priorities.USER, (int) priorities.get(InvokedMethodClientRequestFilter.class));
    }
}