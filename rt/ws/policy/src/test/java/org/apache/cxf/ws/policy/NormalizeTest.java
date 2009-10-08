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

import java.io.InputStream;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.ws.policy.builder.xml.XMLPrimitiveAssertionBuilder;
import org.apache.neethi.Policy;
import org.apache.neethi.util.PolicyComparator;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NormalizeTest extends Assert {
    
    private IMocksControl control;
    
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }
    
    @After
    public void tearDown() {
        control.verify();
    }
    
    @Test
    public void testNormalize() throws Exception {
        Bus bus = createBus(PolicyConstants.NAMESPACE_XMLSOAP_200409);
        PolicyBuilderImpl builder = createBuilder(bus);
        
        int n = 26;
        for (int i = 1; i < n; i++) {
            String sample = "/samples/test" + i + ".xml";
            String normalized = "/normalized/test" + i + ".xml";
            doTestNormalize(builder, sample, normalized);
        }       
    }
    
    @Test
    public void testNormalizeDefaultNs() throws Exception {
        Bus bus = createBus(PolicyConstants.NAMESPACE_WS_POLICY);
        PolicyBuilderImpl builder = createBuilder(bus);
        
        String sample = "/samples/test1DefaultNs.xml";
        String normalized = "/normalized/test1DefaultNs.xml";
        doTestNormalize(builder, sample, normalized);
    }
    
    private PolicyBuilderImpl createBuilder(Bus bus) {
        PolicyBuilderImpl builder = new PolicyBuilderImpl();
        builder.setBus(bus);
        AssertionBuilderRegistry abr = new AssertionBuilderRegistryImpl();
        builder.setAssertionBuilderRegistry(abr);
        XMLPrimitiveAssertionBuilder ab = new XMLPrimitiveAssertionBuilder();
        ab.setBus(bus);
       
        abr.register(new QName("http://schemas.xmlsoap.org/ws/2002/12/secext", "SecurityToken"), ab);
        abr.register(new QName("http://schemas.xmlsoap.org/ws/2002/12/secext", "SecurityHeader"), ab);
        abr.register(new QName("http://schemas.xmlsoap.org/ws/2002/12/secext", "Integrity"), ab);
        abr.register(new QName("http://sample.org/Assertions", "A"), ab);
        abr.register(new QName("http://sample.org/Assertions", "B"), ab);
        abr.register(new QName("http://sample.org/Assertions", "C"), ab);
        return builder;
    }
    
    private Bus createBus(String policyNamespace) {
        Bus bus = control.createMock(Bus.class);
        PolicyConstants constants = new PolicyConstants();
        constants.setNamespace(policyNamespace);
        EasyMock.expect(bus.getExtension(PolicyConstants.class)).andReturn(constants).anyTimes();
        control.replay();
        return bus;
    }
    
    private void doTestNormalize(
        PolicyBuilderImpl builder, String sample, String normalized) throws Exception {
        
        InputStream sampleIn = NormalizeTest.class.getResourceAsStream(sample);
        assertNotNull("Could not get input stream for resource " + sample, sampleIn);
        InputStream normalisedIn = NormalizeTest.class.getResourceAsStream(normalized);
        assertNotNull("Could not get input stream for resource " + normalized, normalisedIn);
                    
        Policy samplePolicy = builder.getPolicy(sampleIn);
        Policy normalisedPolicy = builder.getPolicy(normalisedIn);
        assertNotNull(samplePolicy);
        assertNotNull(normalisedPolicy);
        
        Policy normalisedSamplePolicy = (Policy)samplePolicy.normalize(true);
        assertTrue(PolicyComparator.compare(normalisedPolicy, normalisedSamplePolicy));
    }
    
}
