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

import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class EndpointReferenceDomainExpressionTest {
    // Avoid spurious failures on EasyMock detecting finalize calls
    // by using data members rather than local variables for these.
    private ServiceInfo si;
    private BindingOperationInfo boi;
    private BindingMessageInfo bmi;
    private BindingFaultInfo bfi;

    @Test
    public void testEndpointReferenceDomainExpression() {
        EndpointReferenceType epr = mock(EndpointReferenceType.class);

        EndpointReferenceDomainExpression eprde = new EndpointReferenceDomainExpression();
        assertNull(eprde.getEndpointReference());
        eprde.setEndpointReference(epr);
        assertSame(epr, eprde.getEndpointReference());

        si = mock(ServiceInfo.class);
        boi = mock(BindingOperationInfo.class);
        bmi = mock(BindingMessageInfo.class);
        bfi = mock(BindingFaultInfo.class);

        assertFalse(eprde.appliesTo(si));
        assertFalse(eprde.appliesTo(boi));
        assertFalse(eprde.appliesTo(bmi));
        assertFalse(eprde.appliesTo(bfi));

        EndpointInfo ei = mock(EndpointInfo.class);
        when(ei.getAddress()).thenReturn("http://localhost:8080/GreeterPort");
        AttributedURIType auri = mock(AttributedURIType.class);
        when(epr.getAddress()).thenReturn(auri);
        when(auri.getValue()).thenReturn("http://localhost:8080/Greeter");
        assertFalse(eprde.appliesTo(ei));

        when(ei.getAddress()).thenReturn("http://localhost:8080/GreeterPort");
        when(epr.getAddress()).thenReturn(auri);
        when(auri.getValue()).thenReturn("http://localhost:8080/GreeterPort");
        assertTrue(eprde.appliesTo(ei));

        bfi = null;
        bmi = null;
        boi = null;
        si = null;
    }

}
