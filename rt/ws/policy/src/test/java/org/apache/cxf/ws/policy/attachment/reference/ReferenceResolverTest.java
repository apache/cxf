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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.service.model.DescriptionInfo;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class ReferenceResolverTest {
    @Test
    public void testLocalServiceModelReferenceResolver() {
        DescriptionInfo di = mock(DescriptionInfo.class);
        PolicyBuilder builder = mock(PolicyBuilder.class);
        LocalServiceModelReferenceResolver resolver =
            new LocalServiceModelReferenceResolver(di, builder);

        List<UnknownExtensibilityElement> extensions = new ArrayList<>();
        when(di.getExtensors(UnknownExtensibilityElement.class)).thenReturn(extensions);

        assertNull(resolver.resolveReference("A"));

        UnknownExtensibilityElement extension = mock(UnknownExtensibilityElement.class);
        extensions.add(extension);
        when(di.getExtensors(UnknownExtensibilityElement.class)).thenReturn(extensions);
        Element e = mock(Element.class);
        QName qn = new QName(Constants.URI_POLICY_NS,
                             Constants.ELEM_POLICY);
        when(extension.getElementType()).thenReturn(qn);
        when(extension.getElement()).thenReturn(e);
        Document ownerDocument = mock(Document.class);
        when(e.getOwnerDocument()).thenReturn(ownerDocument);
        when(e.getAttributeNS(PolicyConstants.WSU_NAMESPACE_URI,
                                         PolicyConstants.WSU_ID_ATTR_NAME))
                        .thenReturn("A");
        Policy p = mock(Policy.class);
        when(builder.getPolicy(e)).thenReturn(p);

        assertSame(p, resolver.resolveReference("A"));
        verify(extension, times(1)).getElement();
    }

    @Test
    public void testRemoteReferenceResolverWithOlderNs() {

        doTestRemoteResolver(Constants.URI_POLICY_15_DEPRECATED_NS);
    }

    @Test
    public void testRemoteReferenceResolverWithDefaultNs() {
        doTestRemoteResolver(Constants.URI_POLICY_NS);
    }

    private void doTestRemoteResolver(String policyNs) {

        URL url = ReferenceResolverTest.class.getResource("referring.wsdl");
        String baseURI = url.toString();
        PolicyBuilder builder = mock(PolicyBuilder.class);
        RemoteReferenceResolver resolver = new RemoteReferenceResolver(baseURI, builder);

        assertNull(resolver.resolveReference("referred.wsdl#PolicyB"));

        Policy p = mock(Policy.class);
        when(builder.getPolicy(isA(Element.class))).thenReturn(p);

        assertSame(p, resolver.resolveReference("referred.wsdl#PolicyA"));
    }
}