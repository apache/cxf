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

import java.util.Collection;
import java.util.Collections;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.neethi.Policy;

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
public class PolicyAttachmentTest {
    @Test
    public void testBasic() {
        PolicyAttachment pa = new PolicyAttachment();
        assertNull(pa.getDomainExpressions());
        assertNull(pa.getPolicy());

        Policy p = mock(Policy.class);
        Collection<DomainExpression> des = CastUtils.cast(Collections.emptyList(), DomainExpression.class);

        pa.setPolicy(p);
        pa.setDomainExpressions(des);
        assertSame(p, pa.getPolicy());
        assertSame(des, pa.getDomainExpressions());
    }

    @Test
    public void testAppliesToService() {
        ServiceInfo si1 = mock(ServiceInfo.class);
        ServiceInfo si2 = mock(ServiceInfo.class);
        DomainExpression de = mock(DomainExpression.class);
        Collection<DomainExpression> des = Collections.singletonList(de);
        PolicyAttachment pa = new PolicyAttachment();
        pa.setDomainExpressions(des);

        when(de.appliesTo(si1)).thenReturn(false);
        when(de.appliesTo(si2)).thenReturn(true);

        assertFalse(pa.appliesTo(si1));
        assertTrue(pa.appliesTo(si2));
    }

    @Test
    public void testAppliesToEndpoint() {
        EndpointInfo ei1 = mock(EndpointInfo.class);
        EndpointInfo ei2 = mock(EndpointInfo.class);
        DomainExpression de = mock(DomainExpression.class);
        Collection<DomainExpression> des = Collections.singletonList(de);
        PolicyAttachment pa = new PolicyAttachment();
        pa.setDomainExpressions(des);

        when(de.appliesTo(ei1)).thenReturn(false);
        when(de.appliesTo(ei2)).thenReturn(true);

        assertFalse(pa.appliesTo(ei1));
        assertTrue(pa.appliesTo(ei2));

    }

    @Test
    public void testAppliesToOperation() {
        BindingOperationInfo boi1 = mock(BindingOperationInfo.class);
        BindingOperationInfo boi2 = mock(BindingOperationInfo.class);
        DomainExpression de = mock(DomainExpression.class);
        Collection<DomainExpression> des = Collections.singletonList(de);
        PolicyAttachment pa = new PolicyAttachment();
        pa.setDomainExpressions(des);

        when(de.appliesTo(boi1)).thenReturn(false);
        when(de.appliesTo(boi2)).thenReturn(true);

        assertFalse(pa.appliesTo(boi1));
        assertTrue(pa.appliesTo(boi2));
    }

    @Test
    public void testAppliesToMessage() {
        BindingMessageInfo bmi1 = mock(BindingMessageInfo.class);
        BindingMessageInfo bmi2 = mock(BindingMessageInfo.class);
        DomainExpression de = mock(DomainExpression.class);
        Collection<DomainExpression> des = Collections.singletonList(de);
        PolicyAttachment pa = new PolicyAttachment();
        pa.setDomainExpressions(des);

        when(de.appliesTo(bmi1)).thenReturn(false);
        when(de.appliesTo(bmi2)).thenReturn(true);

        assertFalse(pa.appliesTo(bmi1));
        assertTrue(pa.appliesTo(bmi2));
    }

    @Test
    public void testAppliesToFault() {
        BindingFaultInfo bfi1 = mock(BindingFaultInfo.class);
        BindingFaultInfo bfi2 = mock(BindingFaultInfo.class);
        DomainExpression de = mock(DomainExpression.class);
        Collection<DomainExpression> des = Collections.singletonList(de);
        PolicyAttachment pa = new PolicyAttachment();
        pa.setDomainExpressions(des);

        when(de.appliesTo(bfi1)).thenReturn(false);
        when(de.appliesTo(bfi2)).thenReturn(true);

        assertFalse(pa.appliesTo(bfi1));
        assertTrue(pa.appliesTo(bfi2));
    }


}
