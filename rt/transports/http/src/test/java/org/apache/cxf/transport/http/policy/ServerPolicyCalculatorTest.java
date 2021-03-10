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
package org.apache.cxf.transport.http.policy;

import org.apache.cxf.transport.http.policy.impl.ServerPolicyCalculator;
import org.apache.cxf.transports.http.configuration.HTTPServerPolicy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServerPolicyCalculatorTest {
    @Test
    public void testCompatibleServerPolicies() {
        ServerPolicyCalculator spc = new ServerPolicyCalculator();
        HTTPServerPolicy p1 = new HTTPServerPolicy();
        assertTrue("Policy is not compatible with itself.", spc.compatible(p1, p1));
        HTTPServerPolicy p2 = new HTTPServerPolicy();
        assertTrue("Policies are not compatible.", spc.compatible(p1, p2));
        p1.setServerType("server");
        assertTrue("Policies are not compatible.", spc.compatible(p1, p2));
        p1.setServerType(null);
        p1.setReceiveTimeout(10000);
        assertTrue("Policies are not compatible.", spc.compatible(p1, p2));
        p1.setSuppressClientSendErrors(false);
        assertTrue("Policies are compatible.", spc.compatible(p1, p2));
        p1.setSuppressClientSendErrors(true);
        assertFalse("Policies are compatible.", spc.compatible(p1, p2));
        p2.setSuppressClientSendErrors(true);
        assertTrue("Policies are compatible.", spc.compatible(p1, p2));
    }

    @Test
    public void testIntersectServerPolicies() {
        ServerPolicyCalculator spc = new ServerPolicyCalculator();
        HTTPServerPolicy p1 = new HTTPServerPolicy();
        HTTPServerPolicy p2 = new HTTPServerPolicy();

        p1.setServerType("server");
        HTTPServerPolicy p = spc.intersect(p1, p2);
        assertEquals("server", p.getServerType());
        p1.setServerType(null);
        p1.setReceiveTimeout(10000L);
        p = spc.intersect(p1, p2);
        assertEquals(10000L, p.getReceiveTimeout());
        p1.setSuppressClientSendErrors(true);
        p2.setSuppressClientSendErrors(true);
        p = spc.intersect(p1, p2);
        assertTrue(p.isSuppressClientSendErrors());
    }


    @Test
    public void testEqualServerPolicies() {
        ServerPolicyCalculator spc = new ServerPolicyCalculator();
        HTTPServerPolicy p1 = new HTTPServerPolicy();
        assertTrue(spc.equals(p1, p1));
        HTTPServerPolicy p2 = new HTTPServerPolicy();
        assertTrue(spc.equals(p1, p2));
        p1.setContentEncoding("encoding");
        assertFalse(spc.equals(p1, p2));
        p2.setContentEncoding("encoding");
        assertTrue(spc.equals(p1, p2));
        p1.setSuppressClientSendErrors(true);
        assertFalse(spc.equals(p1, p2));
    }
}
