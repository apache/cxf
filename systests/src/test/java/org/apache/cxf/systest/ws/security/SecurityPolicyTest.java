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

package org.apache.cxf.systest.ws.security;

import java.math.BigInteger;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.policytest.doubleit.DoubleItPortType;
import org.apache.cxf.policytest.doubleit.DoubleItService;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.PolicyException;
import org.junit.BeforeClass;
import org.junit.Test;


public class SecurityPolicyTest extends AbstractBusClientServerTestBase  {
    public static final String POLICY_ADDRESS = "http://localhost:9009/SecPolTest";
    @BeforeClass 
    public static void init() throws Exception {
        createStaticBus().getExtension(PolicyEngine.class).setEnabled(true);
        Endpoint.publish(POLICY_ADDRESS,
                         new DoubleItImpl());
    }
    
    @Test
    public void testPolicy() throws Exception {
        DoubleItService service = new DoubleItService();
        DoubleItPortType pt = service.getDoubleItPort();
        ((BindingProvider)pt).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                      POLICY_ADDRESS);
        try {
            pt.doubleIt(BigInteger.valueOf(25));
        } catch (SOAPFaultException ex) {
            assertTrue(ex.getCause().getCause() instanceof PolicyException);
            //expected - we don't support any of the policies yet
        }
    }
    
    
}
