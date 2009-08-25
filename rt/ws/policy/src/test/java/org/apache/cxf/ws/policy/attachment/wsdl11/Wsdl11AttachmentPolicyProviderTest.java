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

package org.apache.cxf.ws.policy.attachment.wsdl11;

import java.net.URL;
import java.util.List;

import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.ws.policy.AssertionBuilderRegistry;
import org.apache.cxf.ws.policy.AssertionBuilderRegistryImpl;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyBuilderImpl;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.policy.PolicyRegistryImpl;
import org.apache.cxf.ws.policy.builder.xml.XMLPrimitiveAssertionBuilder;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLManagerImpl;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.util.PolicyComparator;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class Wsdl11AttachmentPolicyProviderTest extends Assert {

    private static final String NAMESPACE_URI = "http://apache.org/cxf/calculator";
    private static final QName OPERATION_NAME = new QName(NAMESPACE_URI, "add");
    private static ServiceInfo[] services;
    private static EndpointInfo[] endpoints;
    private Wsdl11AttachmentPolicyProvider app;
    private Bus bus;
    private IMocksControl control = EasyMock.createNiceControl();
    
   
    
    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        
        IMocksControl control = EasyMock.createNiceControl();
        Bus bus = control.createMock(Bus.class);
        WSDLManager manager = new WSDLManagerImpl();       
        WSDLServiceBuilder builder = new WSDLServiceBuilder(bus);
        DestinationFactoryManager dfm = control.createMock(DestinationFactoryManager.class);
        EasyMock.expect(bus.getExtension(DestinationFactoryManager.class)).andReturn(dfm).anyTimes();
        EasyMock.expect(dfm.getDestinationFactory(EasyMock.isA(String.class))).andReturn(null).anyTimes();
        BindingFactoryManager bfm = control.createMock(BindingFactoryManager.class);
        EasyMock.expect(bus.getExtension(BindingFactoryManager.class)).andReturn(bfm).anyTimes();
        EasyMock.expect(bfm.getBindingFactory(EasyMock.isA(String.class))).andReturn(null).anyTimes();
        control.replay();
        
        int n = 19;
        services = new ServiceInfo[n];
        endpoints = new EndpointInfo[n];
        for (int i = 0; i < n; i++) {
            String resourceName = "/attachment/wsdl11/test" + i + ".wsdl";
            URL url = Wsdl11AttachmentPolicyProviderTest.class.getResource(resourceName);       
            try {
                services[i] = builder.buildServices(manager.getDefinition(url)).get(0);
            } catch (WSDLException ex) {
                ex.printStackTrace();
                fail("Failed to build service from resource " + resourceName);
            }
            assertNotNull(services[i]);
            endpoints[i] = services[i].getEndpoints().iterator().next();
            assertNotNull(endpoints[i]);
        }
        
        control.verify();

    }
    
    @AfterClass
    public static void oneTimeTearDown() {
        endpoints = null;
        services = null;
        
    }
    
    @Before
    public void setUp() {   
        control = EasyMock.createNiceControl();
        bus = control.createMock(Bus.class);
        bus.getExtension(ConfiguredBeanLocator.class);
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        AssertionBuilderRegistry abr = new AssertionBuilderRegistryImpl();
        abr.setIgnoreUnknownAssertions(false);
        XMLPrimitiveAssertionBuilder ab = new XMLPrimitiveAssertionBuilder();
        ab.setBus(bus);
        abr.register(new QName("http://cxf.apache.org/test/assertions", "A"), ab);
        abr.register(new QName("http://cxf.apache.org/test/assertions", "B"), ab);
        abr.register(new QName("http://cxf.apache.org/test/assertions", "C"), ab);
        
        PolicyBuilderImpl pb = new PolicyBuilderImpl();
        bus.getExtension(PolicyBuilder.class);
        EasyMock.expectLastCall().andReturn(pb).anyTimes();
        bus.getExtension(PolicyEngine.class);
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        
        pb.setAssertionBuilderRegistry(abr);
        app = new Wsdl11AttachmentPolicyProvider();
        app.setBuilder(pb);
        app.setRegistry(new PolicyRegistryImpl());
        control.replay();
        
    }
    
    
    @After
    public void tearDown() {
        control.verify();
    }
    
    @Test
    public void testElementPolicies() throws WSDLException {
    
        Policy p;
        
        // no extensions       
        p = app.getElementPolicy(services[0]);
        assertTrue(p == null || p.isEmpty());
        
        // extensions not of type Policy or PolicyReference
        p = app.getElementPolicy(services[1]);
        assertTrue(p == null || p.isEmpty());
        
        // one extension of type Policy, without assertion builder
        try {
            p = app.getElementPolicy(services[2]);
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            // expected
        }
        
        // one extension of type Policy
        p = app.getElementPolicy(services[3]);
        assertNotNull(p);
        assertTrue(!p.isEmpty());
        verifyAssertionsOnly(p, 2);
        
        // two extensions of type Policy
        p = app.getElementPolicy(services[4]);
        assertNotNull(p);
        assertTrue(!p.isEmpty());
        verifyAssertionsOnly(p, 3);
        
        EndpointInfo ei = new EndpointInfo();
        assertTrue(app.getElementPolicy(ei) == null);
    }
    
    @Test
    public void testEffectiveServicePolicies() throws WSDLException {
        
        Policy p;
        Policy ep;
        
        // no extensions        
        ep = app.getEffectivePolicy(services[0]);
        assertTrue(ep == null || ep.isEmpty());
        p = app.getElementPolicy(services[0]);
        assertTrue(p == null || p.isEmpty());
        
        // extensions not of type Policy or PolicyReference
        ep = app.getEffectivePolicy(services[1]);
        assertTrue(ep == null || ep.isEmpty());
        
        // one extension of type Policy, without assertion builder
        try {
            ep = app.getEffectivePolicy(services[2]);
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            // expected
        }
        
        // one extension of type Policy
        ep = app.getEffectivePolicy(services[3]);
        assertNotNull(ep);
        assertTrue(!ep.isEmpty());
        verifyAssertionsOnly(ep, 2);
        p = app.getElementPolicy(services[3]);
        assertTrue(PolicyComparator.compare(p, ep));
        
        // two extensions of type Policy
        ep = app.getEffectivePolicy(services[4]);
        assertNotNull(ep);
        assertTrue(!ep.isEmpty());
        verifyAssertionsOnly(ep, 3);
        p = app.getElementPolicy(services[4]);
        assertTrue(PolicyComparator.compare(p, ep));
    }

    @Test
    public void testEffectiveEndpointPolicies() {
        Policy ep;
        Policy p;
        
        // port has no extensions
        // porttype has no extensions
        // binding has no extensions
        ep = app.getEffectivePolicy(endpoints[0]);
        assertTrue(ep == null || ep.isEmpty());
        
        // port has one extension of type Policy        
        // binding has no extensions
        // porttype has no extensions
        ep = app.getEffectivePolicy(endpoints[5]);
        assertNotNull(ep);
        assertTrue(!ep.isEmpty());
        verifyAssertionsOnly(ep, 1);
        p = app.getElementPolicy(endpoints[5]);
        assertTrue(PolicyComparator.compare(p, ep));
        
        // port has no extensions
        // binding has one extension of type Policy
        // porttype has no extensions
        ep = app.getEffectivePolicy(endpoints[6]);
        assertNotNull(ep);
        assertTrue(!ep.isEmpty());
        verifyAssertionsOnly(ep, 1);
        p = app.getElementPolicy(endpoints[6].getBinding());
        assertTrue(PolicyComparator.compare(p, ep));
        
        // port has no extensions
        // binding has no extensions
        // porttype has one extension of type Policy
        ep = app.getEffectivePolicy(endpoints[7]);
        assertNotNull(ep);
        assertTrue(!ep.isEmpty());
        verifyAssertionsOnly(ep, 1);
        p = app.getElementPolicy(endpoints[7].getInterface());
        assertTrue(PolicyComparator.compare(p, ep));        
        
        // port has one extension of type Policy
        // porttype has one extension of type Policy
        // binding has one extension of type Policy
        ep = app.getEffectivePolicy(endpoints[8]);
        assertNotNull(ep);
        assertTrue(!ep.isEmpty());
        verifyAssertionsOnly(ep, 3);
        
        // port has no extensions
        // binding has no extensions
        // porttype has no extension elements but one extension attribute of type PolicyURIs
        // consisting of two references (one local, one external)
        
        ep = app.getEffectivePolicy(endpoints[18]);
        assertNotNull(ep);
        assertTrue(!ep.isEmpty());
        verifyAssertionsOnly(ep, 2);       
    }
    
    @Test
    public void testEffectiveBindingOperationPolicies() {
        Policy ep;
        
        // operation has no extensions
        // binding operation has no extensions
        ep = app.getEffectivePolicy(getBindingOperationInfo(endpoints[0]));
        assertTrue(ep == null || ep.isEmpty());
        
        // operation has no extensions
        // binding operation has one extension of type Policy
        ep = app.getEffectivePolicy(getBindingOperationInfo(endpoints[9]));
        assertNotNull(ep);
        assertTrue(!ep.isEmpty());
        verifyAssertionsOnly(ep, 1);
        
        // operation has one extension of type Policy
        // binding operation has no extensions
        ep = app.getEffectivePolicy(getBindingOperationInfo(endpoints[10]));
        assertNotNull(ep);
        assertTrue(!ep.isEmpty());
        verifyAssertionsOnly(ep, 2);
        
        // operation has one extension of type Policy
        // binding operation one extension of type Policy
        ep = app.getEffectivePolicy(getBindingOperationInfo(endpoints[11]));
        assertNotNull(ep);
        assertTrue(!ep.isEmpty());
        verifyAssertionsOnly(ep, 3);
    }
    
    @Test
    public void testEffectiveMessagePolicies() {
        Policy ep;
        
        // binding operation message has no extensions
        // operation message has no extensions
        // message has no extensions
        ep = app.getEffectivePolicy(getBindingMessageInfo(endpoints[0], true));
        assertTrue(ep == null || ep.isEmpty());
        
        // binding operation message has one extension of type Policy
        // operation message has no extensions
        // message has no extensions
        ep = app.getEffectivePolicy(getBindingMessageInfo(endpoints[12], true));
        assertTrue(!ep.isEmpty());
        verifyAssertionsOnly(ep, 1);
        
        // binding operation message has no extensions
        // operation message has one extension of type Policy
        // message has no extensions  
        ep = app.getEffectivePolicy(getBindingMessageInfo(endpoints[13], true));
        assertTrue(!ep.isEmpty());
        verifyAssertionsOnly(ep, 1);
        
        // binding operation message has no extensions
        // operation message has no extensions
        // message has one extension of type Policy
        ep = app.getEffectivePolicy(getBindingMessageInfo(endpoints[14], true));
        assertTrue(!ep.isEmpty());
        verifyAssertionsOnly(ep, 1);
        
        // binding operation message has one extension of type Policy
        // operation message has one extension of type Policy
        // message has one extension of type Policy
        ep = app.getEffectivePolicy(getBindingMessageInfo(endpoints[15], true));
        assertTrue(!ep.isEmpty());
        verifyAssertionsOnly(ep, 3);      
    }
    
    @Test
    public void testResolveLocal() {
        
        Policy ep;
        
        // service has one extension of type PolicyReference, reference can be resolved locally
        ep = app.getElementPolicy(services[16]);
        assertNotNull(ep);
        verifyAssertionsOnly(ep, 2);
        
        // port has one extension of type PolicyReference, reference cannot be resolved locally
        try {
            app.getElementPolicy(endpoints[16]);
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            // expected
        }        
    }
    
    @Test
    public void testResolveExternal() {
        // service has one extension of type PolicyReference, reference is external
        Policy p = app.getElementPolicy(services[17]);
        verifyAssertionsOnly(p, 2);
        
        // port has one extension of type PolicyReference, reference cannot be resolved because
        // referenced document does not contain policy with the required if
        try {
            app.getElementPolicy(endpoints[17]);
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            // expected
        } 
        
        // binding has one extension of type PolicyReference, reference cannot be resolved because
        // referenced document cannot be found
        try {
            app.getElementPolicy(endpoints[17].getBinding());
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            // expected
        } 
    }
    
    
    private void verifyAssertionsOnly(Policy p, int expectedAssertions) {
        List<PolicyComponent> pcs;
        pcs = CastUtils.cast(p.getAssertions(), PolicyComponent.class);
        assertEquals(expectedAssertions, pcs.size());
        for (int i = 0; i < expectedAssertions; i++) {
            assertEquals(Constants.TYPE_ASSERTION, pcs.get(i).getType());
        }
    }
    
    private BindingOperationInfo getBindingOperationInfo(EndpointInfo ei) {
        return ei.getBinding().getOperation(OPERATION_NAME);        
    }
    
    private BindingMessageInfo getBindingMessageInfo(EndpointInfo ei, boolean in) {
        return in ? ei.getBinding().getOperation(OPERATION_NAME).getInput()
            : ei.getBinding().getOperation(OPERATION_NAME).getOutput();
    }
    
    
}
