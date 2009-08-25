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

package org.apache.cxf.ws.policy.attachment.external;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.neethi.Policy;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;


/**
 * 
 */
public class ExternalAttachmentProviderTest extends Assert {

    private static final QName TEST_ASSERTION_TYPE = new QName("http://a.b.c", "x");
    
    
    private IMocksControl control;
    private Policy policy;
    private PolicyAssertion assertion;
    private PolicyAttachment attachment;
    private Collection<PolicyAttachment> attachments = new ArrayList<PolicyAttachment>();
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();  
    } 
    
    @Test
    public void testBasic() {
        ExternalAttachmentProvider eap = new ExternalAttachmentProvider();
        assertNull(eap.getLocation());
        Resource uri = control.createMock(Resource.class);
        eap.setLocation(uri);
        assertSame(uri, eap.getLocation());
        
    }
    
    @Test
    public void testGetEffectiveFaultPolicy() {
        ExternalAttachmentProvider eap = new ExternalAttachmentProvider();
        BindingFaultInfo bfi = control.createMock(BindingFaultInfo.class);
        setUpAttachment(bfi, false, eap);
        control.replay();
        assertTrue(eap.getEffectivePolicy(bfi).isEmpty());
        control.verify();
        
        control.reset();
        setUpAttachment(bfi, true, eap);
        control.replay();
        assertSame(assertion, eap.getEffectivePolicy(bfi).getAssertions().get(0));
        control.verify();
    }
    
    @Test
    public void testGetEffectiveMessagePolicy() {
        ExternalAttachmentProvider eap = new ExternalAttachmentProvider();
        BindingMessageInfo bmi = control.createMock(BindingMessageInfo.class);
        setUpAttachment(bmi, false, eap);
        control.replay();
        assertTrue(eap.getEffectivePolicy(bmi).isEmpty());
        control.verify();
        
        control.reset();
        setUpAttachment(bmi, true, eap);
        control.replay();
        assertSame(assertion, eap.getEffectivePolicy(bmi).getAssertions().get(0));
        control.verify();
    }
    
    @Test
    public void testGetEffectiveOperationPolicy() {
        BindingOperationInfo boi = control.createMock(BindingOperationInfo.class);
        ExternalAttachmentProvider eap = new ExternalAttachmentProvider();
        setUpAttachment(boi, false, eap);
        control.replay();
        assertTrue(eap.getEffectivePolicy(boi).isEmpty());
        control.verify();
        
        control.reset();
        setUpAttachment(boi, true, eap);
        control.replay();
        assertSame(assertion, eap.getEffectivePolicy(boi).getAssertions().get(0));
        control.verify();
    }

    @Test
    public void testGetEffectiveEndpointPolicy() {
        ExternalAttachmentProvider eap = new ExternalAttachmentProvider();
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        setUpAttachment(ei, false, eap);
        control.replay();
        assertTrue(eap.getEffectivePolicy(ei).isEmpty());
        control.verify();
        
        control.reset();
        setUpAttachment(ei, true, eap);
        control.replay();
        assertSame(assertion, eap.getEffectivePolicy(ei).getAssertions().get(0));
        control.verify();
    }

    @Test
    public void testGetEffectiveServicePolicy() {
        ExternalAttachmentProvider eap = new ExternalAttachmentProvider();
        ServiceInfo si = control.createMock(ServiceInfo.class);
        setUpAttachment(si, false, eap);
        control.replay();
        assertTrue(eap.getEffectivePolicy(si).isEmpty());
        control.verify();
        
        control.reset();
        setUpAttachment(si, true, eap);
        control.replay();
        assertSame(assertion, eap.getEffectivePolicy(si).getAssertions().get(0));
        control.verify();
    }
    
    @Test
    public void testReadDocumentNotExisting() throws MalformedURLException {
        ExternalAttachmentProvider eap = new ExternalAttachmentProvider();
        URL url = ExternalAttachmentProviderTest.class.getResource("resources/attachments1.xml");
        String uri = url.toExternalForm();
        uri = uri.replaceAll("attachments1.xml", "attachments0.xml");
        eap.setLocation(new UrlResource(uri));
        try {
            eap.readDocument();
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            assertTrue(ex.getCause() instanceof FileNotFoundException);
        }
    }
    
    @Test
    public void testReadDocumentWithoutAttachmentElements() throws MalformedURLException {
        ExternalAttachmentProvider eap = new ExternalAttachmentProvider();
        URL url = ExternalAttachmentProviderTest.class.getResource("resources/attachments1.xml");
        String uri = url.toExternalForm();
        eap.setLocation(new UrlResource(uri));
        eap.readDocument(); 
        assertTrue(eap.getAttachments().isEmpty());
    }
    
    @Test
    public void testReadDocumentAttachmentElementWithoutAppliesTo() throws MalformedURLException {
        ExternalAttachmentProvider eap = new ExternalAttachmentProvider();
        URL url = ExternalAttachmentProviderTest.class.getResource("resources/attachments2.xml");
        String uri = url.toExternalForm();
        eap.setLocation(new UrlResource(uri));
        eap.readDocument(); 
        assertTrue(eap.getAttachments().isEmpty());
    }
    
    @Test
    public void testReadDocumentUnknownDomainExpression() throws MalformedURLException {

        Bus bus = control.createMock(Bus.class);
        
        DomainExpressionBuilderRegistry debr = control.createMock(DomainExpressionBuilderRegistry.class);
        EasyMock.expect(bus.getExtension(DomainExpressionBuilderRegistry.class)).andReturn(debr);
        EasyMock.expect(debr.build(EasyMock.isA(Element.class)))
            .andThrow(new PolicyException(new Exception()));
        URL url = ExternalAttachmentProviderTest.class.getResource("resources/attachments3.xml");
        String uri = url.toExternalForm();
        
        control.replay();
        ExternalAttachmentProvider eap = new ExternalAttachmentProvider(bus);
        eap.setLocation(new UrlResource(uri));
        try {
            eap.readDocument();
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            // expected
        }
        control.verify();
    }
    
    @Test
    public void testReadDocumentEPRDomainExpression() throws MalformedURLException {
        
        Bus bus = control.createMock(Bus.class);
        
        DomainExpressionBuilderRegistry debr = control.createMock(DomainExpressionBuilderRegistry.class);
        EasyMock.expect(bus.getExtension(DomainExpressionBuilderRegistry.class)).andReturn(debr);
        DomainExpression de = control.createMock(DomainExpression.class);
        EasyMock.expect(debr.build(EasyMock.isA(Element.class))).andReturn(de);
        PolicyBuilder pb = control.createMock(PolicyBuilder.class);
        EasyMock.expect(bus.getExtension(PolicyBuilder.class)).andReturn(pb).anyTimes();
        Policy p = control.createMock(Policy.class);
        EasyMock.expect(pb.getPolicy(EasyMock.isA(Element.class))).andReturn(p);
                
        
        control.replay();
        ExternalAttachmentProvider eap = new ExternalAttachmentProvider(bus);
        URL url = ExternalAttachmentProviderTest.class.getResource("resources/attachments4.xml");
        String uri = url.toExternalForm();
        eap.setLocation(new UrlResource(uri));
        eap.readDocument();
        assertEquals(1, eap.getAttachments().size());
        PolicyAttachment pa = eap.getAttachments().iterator().next();
        assertSame(p, pa.getPolicy());
        assertEquals(1, pa.getDomainExpressions().size());
        assertSame(de, pa.getDomainExpressions().iterator().next());
        control.verify();
    }
    
    void setUpAttachment(Object subject, boolean applies, ExternalAttachmentProvider eap) {
        attachments.clear();
        attachment = control.createMock(PolicyAttachment.class);
        attachments.add(attachment);
        policy = new Policy();
        assertion = new PrimitiveAssertion(TEST_ASSERTION_TYPE);
        policy.addAssertion(assertion);
        eap.setAttachments(attachments);
        if (subject instanceof ServiceInfo) {
            EasyMock.expect(attachment.appliesTo((ServiceInfo)subject)).andReturn(applies);
        } else if (subject instanceof EndpointInfo) {
            EasyMock.expect(attachment.appliesTo((EndpointInfo)subject)).andReturn(applies);
        } else if (subject instanceof BindingOperationInfo) {
            EasyMock.expect(attachment.appliesTo((BindingOperationInfo)subject)).andReturn(applies);
        } else if (subject instanceof BindingMessageInfo) {
            EasyMock.expect(attachment.appliesTo((BindingMessageInfo)subject)).andReturn(applies);
        } else if (subject instanceof BindingFaultInfo) {
            EasyMock.expect(attachment.appliesTo((BindingFaultInfo)subject)).andReturn(applies);
        } else {
            System.err.println("subject class: " + subject.getClass());
        }
        if (applies) {
            EasyMock.expect(attachment.getPolicy()).andReturn(policy);
        } 
    }
    
    
    
}
