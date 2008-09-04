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

package org.apache.cxf.ws.policy.attachment.reference;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.service.model.DescriptionInfo;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.neethi.Policy;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class ReferenceResolverTest extends Assert {

    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    } 
    
    @Test
    public void testLocalServiceModelReferenceResolver() {
        DescriptionInfo di = control.createMock(DescriptionInfo.class);
        PolicyBuilder builder = control.createMock(PolicyBuilder.class);
        LocalServiceModelReferenceResolver resolver = 
            new LocalServiceModelReferenceResolver(di, builder);
        
        List<UnknownExtensibilityElement> extensions = new ArrayList<UnknownExtensibilityElement>();
        EasyMock.expect(di.getExtensors(UnknownExtensibilityElement.class)).andReturn(extensions);
        
        control.replay();
        assertNull(resolver.resolveReference("A"));
        control.verify();
        
        control.reset();
        UnknownExtensibilityElement extension = control.createMock(UnknownExtensibilityElement.class);
        extensions.add(extension);
        EasyMock.expect(di.getExtensors(UnknownExtensibilityElement.class)).andReturn(extensions);
        Element e = control.createMock(Element.class);
        EasyMock.expect(extension.getElement()).andReturn(e).times(2);
        QName qn = new QName(PolicyConstants.NAMESPACE_WS_POLICY, 
                             PolicyConstants.POLICY_ELEM_NAME);
        EasyMock.expect(extension.getElementType()).andReturn(qn).anyTimes();
        EasyMock.expect(e.getAttributeNS(PolicyConstants.WSU_NAMESPACE_URI,
                                         PolicyConstants.WSU_ID_ATTR_NAME))
                        .andReturn("A");
        Policy p = control.createMock(Policy.class);
        EasyMock.expect(builder.getPolicy(e)).andReturn(p);        
        
        control.replay();
        assertSame(p, resolver.resolveReference("A"));
        control.verify();
        
    }
    
    @Test
    public void testRemoteReferenceResolverWithOlderNs() {
        
        doTestRemoteResolver(PolicyConstants.NAMESPACE_W3_200607);
    }
    
    @Test
    public void testRemoteReferenceResolverWithDefaultNs() {
        doTestRemoteResolver(PolicyConstants.NAMESPACE_WS_POLICY);
    }
    
    private void doTestRemoteResolver(String policyNs) {
        
        URL url = ReferenceResolverTest.class.getResource("referring.wsdl");
        String baseURI = url.toString();
        PolicyBuilder builder = control.createMock(PolicyBuilder.class);
        RemoteReferenceResolver resolver = new RemoteReferenceResolver(baseURI, builder);
    
        assertNull(resolver.resolveReference("referred.wsdl#PolicyB"));
        
        Policy p = control.createMock(Policy.class);
        EasyMock.expect(builder.getPolicy(EasyMock.isA(Element.class))).andReturn(p);
        
        control.replay();
        assertSame(p, resolver.resolveReference("referred.wsdl#PolicyA"));
        control.verify(); 
        control.reset();
    }
}
