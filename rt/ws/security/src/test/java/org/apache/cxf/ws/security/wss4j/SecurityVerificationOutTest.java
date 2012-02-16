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
package org.apache.cxf.ws.security.wss4j;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.security.policy.interceptors.SecurityVerificationOutInterceptor;
import org.apache.neethi.Policy;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

public class SecurityVerificationOutTest extends AbstractPolicySecurityTest {
    private IMocksControl control;
    

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    } 
    
    @Test(expected = PolicyException.class)
    public void testEncryptedPartsNoBinding() throws Exception {
        SoapMessage message = coachMessage("encrypted_parts_missing_binding.xml");
        control.replay();
        SecurityVerificationOutInterceptor.INSTANCE.handleMessage(message);
        control.verify();    
    }

    @Test(expected = PolicyException.class)
    public void testSignedPartsNoBinding() throws Exception {
        SoapMessage message = coachMessage("signed_parts_missing_binding.xml");
        control.replay();
        SecurityVerificationOutInterceptor.INSTANCE.handleMessage(message);
        control.verify();    
    }

    @Test
    public void testEncryptedPartsOK() throws Exception {
        SoapMessage message = coachMessage("encrypted_parts_policy_body.xml");
        control.replay();
        SecurityVerificationOutInterceptor.INSTANCE.handleMessage(message);
        control.verify();    
    }

    @Test
    public void testSignedPartsOK() throws Exception {
        SoapMessage message = coachMessage("signed_parts_policy_body.xml");
        control.replay();
        SecurityVerificationOutInterceptor.INSTANCE.handleMessage(message);
        control.verify();    
    }

    private SoapMessage coachMessage(String policyName) 
        throws IOException, ParserConfigurationException, SAXException {
        Policy policy = policyBuilder.getPolicy(this.getResourceAsStream(policyName)); 
        AssertionInfoMap aim = new AssertionInfoMap(policy);
        SoapMessage message = control.createMock(SoapMessage.class);        
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.TRUE);
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(aim);
        return message;
    }
}
