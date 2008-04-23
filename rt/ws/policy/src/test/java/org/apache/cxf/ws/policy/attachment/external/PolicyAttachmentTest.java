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
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class PolicyAttachmentTest extends Assert {

    private IMocksControl control;
    
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();        
    } 
    
    @Test
    public void testBasic() {
        PolicyAttachment pa = new PolicyAttachment();
        assertNull(pa.getDomainExpressions());
        assertNull(pa.getPolicy());
        
        Policy p = control.createMock(Policy.class);
        Collection<DomainExpression> des = CastUtils.cast(Collections.emptyList(), DomainExpression.class);
        
        pa.setPolicy(p);
        pa.setDomainExpressions(des);
        assertSame(p, pa.getPolicy());
        assertSame(des, pa.getDomainExpressions());
    }
    
    @Test
    public void testAppliesToService() {
        ServiceInfo si1 = control.createMock(ServiceInfo.class);
        ServiceInfo si2 = control.createMock(ServiceInfo.class);
        DomainExpression de = control.createMock(DomainExpression.class);
        Collection<DomainExpression> des = Collections.singletonList(de);
        PolicyAttachment pa = new PolicyAttachment();
        pa.setDomainExpressions(des);
       
        EasyMock.expect(de.appliesTo(si1)).andReturn(false);
        EasyMock.expect(de.appliesTo(si2)).andReturn(true);
        control.replay();
        assertTrue(!pa.appliesTo(si1));
        assertTrue(pa.appliesTo(si2));
        control.verify();  
    }
    
    @Test
    public void testAppliesToEndpoint() {
        EndpointInfo ei1 = control.createMock(EndpointInfo.class);
        EndpointInfo ei2 = control.createMock(EndpointInfo.class);
        DomainExpression de = control.createMock(DomainExpression.class);
        Collection<DomainExpression> des = Collections.singletonList(de);
        PolicyAttachment pa = new PolicyAttachment();
        pa.setDomainExpressions(des);
       
        EasyMock.expect(de.appliesTo(ei1)).andReturn(false);
        EasyMock.expect(de.appliesTo(ei2)).andReturn(true);
        control.replay();
        assertTrue(!pa.appliesTo(ei1));
        assertTrue(pa.appliesTo(ei2));
        control.verify();  
    }
    
    @Test
    public void testAppliesToOperation() {
        BindingOperationInfo boi1 = control.createMock(BindingOperationInfo.class);
        BindingOperationInfo boi2 = control.createMock(BindingOperationInfo.class);
        DomainExpression de = control.createMock(DomainExpression.class);
        Collection<DomainExpression> des = Collections.singletonList(de);
        PolicyAttachment pa = new PolicyAttachment();
        pa.setDomainExpressions(des);
       
        EasyMock.expect(de.appliesTo(boi1)).andReturn(false);
        EasyMock.expect(de.appliesTo(boi2)).andReturn(true);
        control.replay();
        assertTrue(!pa.appliesTo(boi1));
        assertTrue(pa.appliesTo(boi2));
        control.verify();  
    }
    
    @Test
    public void testAppliesToMessage() {
        BindingMessageInfo bmi1 = control.createMock(BindingMessageInfo.class);
        BindingMessageInfo bmi2 = control.createMock(BindingMessageInfo.class);
        DomainExpression de = control.createMock(DomainExpression.class);
        Collection<DomainExpression> des = Collections.singletonList(de);
        PolicyAttachment pa = new PolicyAttachment();
        pa.setDomainExpressions(des);
       
        EasyMock.expect(de.appliesTo(bmi1)).andReturn(false);
        EasyMock.expect(de.appliesTo(bmi2)).andReturn(true);
        control.replay();
        assertTrue(!pa.appliesTo(bmi1));
        assertTrue(pa.appliesTo(bmi2));
        control.verify();  
    }
    
    @Test
    public void testAppliesToFault() {
        BindingFaultInfo bfi1 = control.createMock(BindingFaultInfo.class);
        BindingFaultInfo bfi2 = control.createMock(BindingFaultInfo.class);
        DomainExpression de = control.createMock(DomainExpression.class);
        Collection<DomainExpression> des = Collections.singletonList(de);
        PolicyAttachment pa = new PolicyAttachment();
        pa.setDomainExpressions(des);
       
        EasyMock.expect(de.appliesTo(bfi1)).andReturn(false);
        EasyMock.expect(de.appliesTo(bfi2)).andReturn(true);
        control.replay();
        assertTrue(!pa.appliesTo(bfi1));
        assertTrue(pa.appliesTo(bfi2));
        control.verify();  
    }
    
    
}
