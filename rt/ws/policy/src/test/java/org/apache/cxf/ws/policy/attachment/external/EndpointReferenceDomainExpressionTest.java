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
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class EndpointReferenceDomainExpressionTest extends Assert {

    private IMocksControl control;
   
    // Avoid spurious failures on EasyMock detecting finalize calls
    // by using data members rather than local variables for these.
    private ServiceInfo si;
    private BindingOperationInfo boi;
    private BindingMessageInfo bmi;
    private BindingFaultInfo bfi;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();        
    } 
    
    @Test
    public void testEndpointReferenceDomainExpression() {
        EndpointReferenceType epr = control.createMock(EndpointReferenceType.class);
        
        EndpointReferenceDomainExpression eprde = new EndpointReferenceDomainExpression();
        assertNull(eprde.getEndpointReference());
        eprde.setEndpointReference(epr);
        assertSame(epr, eprde.getEndpointReference());
        
        si = control.createMock(ServiceInfo.class);
        boi = control.createMock(BindingOperationInfo.class);
        bmi = control.createMock(BindingMessageInfo.class);
        bfi = control.createMock(BindingFaultInfo.class);
        
        assertTrue(!eprde.appliesTo(si));
        assertTrue(!eprde.appliesTo(boi));
        assertTrue(!eprde.appliesTo(bmi));
        assertTrue(!eprde.appliesTo(bfi));
        
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        EasyMock.expect(ei.getAddress()).andReturn("http://localhost:8080/GreeterPort");
        AttributedURIType auri = control.createMock(AttributedURIType.class);
        EasyMock.expect(epr.getAddress()).andReturn(auri);
        EasyMock.expect(auri.getValue()).andReturn("http://localhost:8080/Greeter");
        control.replay();
        assertTrue(!eprde.appliesTo(ei));
        control.verify();
        
        control.reset();
        EasyMock.expect(ei.getAddress()).andReturn("http://localhost:8080/GreeterPort");
        EasyMock.expect(epr.getAddress()).andReturn(auri);
        EasyMock.expect(auri.getValue()).andReturn("http://localhost:8080/GreeterPort");
        control.replay();
        assertTrue(eprde.appliesTo(ei));
        control.verify();
        
        bfi = null;
        bmi = null;
        boi = null;
        si = null;
    }
    
}
