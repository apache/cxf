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

import java.util.concurrent.ThreadLocalRandom;

import org.apache.cxf.transport.http.policy.impl.ClientPolicyCalculator;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClientPolicyCalculatorTest {
    @Test
    public void testCompatibleClientPolicies() {
        ClientPolicyCalculator calc = new ClientPolicyCalculator();
        HTTPClientPolicy p1 = new HTTPClientPolicy();
        assertTrue("Policy is not compatible with itself.", calc.compatible(p1, p1));
        HTTPClientPolicy p2 = new HTTPClientPolicy();
        assertTrue("Policies are not compatible.", calc.compatible(p1, p2));
        p1.setBrowserType("browser");
        assertTrue("Policies are not compatible.", calc.compatible(p1, p2));
        p1.setBrowserType(null);
        p1.setConnectionTimeout(10000);
        assertTrue("Policies are not compatible.", calc.compatible(p1, p2));
        p1.setAllowChunking(false);
        p2.setAllowChunking(true);
        assertFalse("Policies are compatible.", calc.compatible(p1, p2));
        p2.setAllowChunking(false);
        assertTrue("Policies are compatible.", calc.compatible(p1, p2));
    }

    @Test
    public void testIntersectClientPolicies() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ClientPolicyCalculator calc = new ClientPolicyCalculator();
        HTTPClientPolicy p1 = new HTTPClientPolicy();
        HTTPClientPolicy p2 = new HTTPClientPolicy();

        p1.setBrowserType("browser");
        HTTPClientPolicy p = calc.intersect(p1, p2);
        assertEquals("browser", p.getBrowserType());
        p1.setBrowserType(null);

        long connectionRequestTimeout = random.nextLong(0, 10000);
        p1.setConnectionRequestTimeout(connectionRequestTimeout);
        p = calc.intersect(p1, p2);
        assertEquals(connectionRequestTimeout, p.getConnectionRequestTimeout());

        long receiveTimeout = random.nextLong(0, 10000);
        p1.setReceiveTimeout(receiveTimeout);
        p = calc.intersect(p1, p2);
        assertEquals(receiveTimeout, p.getReceiveTimeout());

        long connectionTimeout = random.nextLong(0, 10000);
        p1.setConnectionTimeout(connectionTimeout);
        p = calc.intersect(p1, p2);
        assertEquals(connectionTimeout, p.getConnectionTimeout());

        p1.setAllowChunking(false);
        p2.setAllowChunking(false);
        p = calc.intersect(p1, p2);
        assertFalse(p.isAllowChunking());
    }

    @Test
    public void testEqualClientPolicies() {
        ClientPolicyCalculator calc = new ClientPolicyCalculator();
        HTTPClientPolicy p1 = new HTTPClientPolicy();
        assertTrue(calc.equals(p1, p1));
        HTTPClientPolicy p2 = new HTTPClientPolicy();
        assertTrue(calc.equals(p1, p2));
        p1.setDecoupledEndpoint("http://localhost:8080/decoupled");
        assertFalse(calc.equals(p1, p2));
        p2.setDecoupledEndpoint("http://localhost:8080/decoupled");
        assertTrue(calc.equals(p1, p2));
        p1.setReceiveTimeout(10000L);
        assertFalse(calc.equals(p1, p2));
    }

    @Test
    public void testLongTimeouts() {
        ClientPolicyCalculator calc = new ClientPolicyCalculator();
        HTTPClientPolicy p1 = new HTTPClientPolicy();
        HTTPClientPolicy p2 = new HTTPClientPolicy();
        p2.setReceiveTimeout(120000);
        p2.setConnectionTimeout(60000);
        HTTPClientPolicy p = calc.intersect(p1, p2);
        assertEquals(120000, p.getReceiveTimeout());
        assertEquals(60000, p.getConnectionTimeout());

        p1 = new HTTPClientPolicy();
        p2 = new HTTPClientPolicy();
        p1.setReceiveTimeout(120000);
        p1.setConnectionTimeout(60000);
        p = calc.intersect(p1, p2);
        assertEquals(120000, p.getReceiveTimeout());
        assertEquals(60000, p.getConnectionTimeout());

        p2.setReceiveTimeout(50000);
        p2.setConnectionTimeout(20000);
        p = calc.intersect(p1, p2);
        //p1 should have priority
        assertEquals(120000, p.getReceiveTimeout());
        assertEquals(60000, p.getConnectionTimeout());

        //reverse intersect
        p = calc.intersect(p2, p1);
        //p2 should have priority
        assertEquals(50000, p.getReceiveTimeout());
        assertEquals(20000, p.getConnectionTimeout());
    }

}
