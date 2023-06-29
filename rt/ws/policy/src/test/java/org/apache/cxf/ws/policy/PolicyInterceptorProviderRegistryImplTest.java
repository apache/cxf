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

package org.apache.cxf.ws.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class PolicyInterceptorProviderRegistryImplTest {

    private static final QName ASSERTION = new QName("testns", "test");
    private static final QName WRONG_ASSERTION = new QName("testns", "wrong");


    @Test
    public void testConstructors() {
        PolicyInterceptorProviderRegistryImpl reg = new PolicyInterceptorProviderRegistryImpl();
        assertNotNull(reg);
        assertEquals(PolicyInterceptorProviderRegistry.class, reg.getRegistrationType());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegister() {
        PolicyInterceptorProviderRegistryImpl reg = new PolicyInterceptorProviderRegistryImpl();
        PolicyInterceptorProvider pp = mock(PolicyInterceptorProvider.class);
        Interceptor<Message> pi1 = mock(Interceptor.class);
        Interceptor<Message> pi2 = mock(Interceptor.class);
        Interceptor<Message> pif = mock(Interceptor.class);
        Interceptor<Message> po = mock(Interceptor.class);
        Interceptor<Message> pof = mock(Interceptor.class);
        List<Interceptor<? extends Message>> pil = new ArrayList<>();
        pil.add(pi1);
        pil.add(pi2);
        List<Interceptor<? extends Message>> pifl = new ArrayList<>();
        pifl.add(pif);
        List<Interceptor<? extends Message>> pol = new ArrayList<>();
        pol.add(po);
        List<Interceptor<? extends Message>> pofl = new ArrayList<>();
        pofl.add(pof);
        when(pp.getInInterceptors()).thenReturn(pil);
        when(pp.getInFaultInterceptors()).thenReturn(pifl);
        when(pp.getOutInterceptors()).thenReturn(pol);
        when(pp.getOutFaultInterceptors()).thenReturn(pofl);
        Collection<QName> assertionTypes = new ArrayList<>();
        assertionTypes.add(ASSERTION);
        when(pp.getAssertionTypes()).thenReturn(assertionTypes);

        reg.register(pp);
        assertEquals(pil, reg.getInInterceptorsForAssertion(ASSERTION));
        assertEquals(pifl, reg.getInFaultInterceptorsForAssertion(ASSERTION));
        assertEquals(pol, reg.getOutInterceptorsForAssertion(ASSERTION));
        assertEquals(pofl, reg.getOutFaultInterceptorsForAssertion(ASSERTION));
        assertTrue(reg.getInInterceptorsForAssertion(WRONG_ASSERTION).isEmpty());
    }

    @Test
    public void testGetNotNull() {
        PolicyInterceptorProviderRegistryImpl reg = new PolicyInterceptorProviderRegistryImpl();
        assertNotNull(reg.get(ASSERTION));
        assertTrue(reg.get(ASSERTION).isEmpty());
        assertNotNull(reg.getInInterceptorsForAssertion(ASSERTION));
        assertTrue(reg.getInInterceptorsForAssertion(ASSERTION).isEmpty());
        assertNotNull(reg.getOutInterceptorsForAssertion(ASSERTION));
        assertTrue(reg.getOutInterceptorsForAssertion(ASSERTION).isEmpty());
        assertNotNull(reg.getInFaultInterceptorsForAssertion(ASSERTION));
        assertTrue(reg.getInFaultInterceptorsForAssertion(ASSERTION).isEmpty());
        assertNotNull(reg.getOutFaultInterceptorsForAssertion(ASSERTION));
        assertTrue(reg.getOutFaultInterceptorsForAssertion(ASSERTION).isEmpty());
    }
}