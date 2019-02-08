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

import org.apache.cxf.transport.http.policy.impl.ClientPolicyCalculator;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;
import org.apache.neethi.Assertion;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class HTTPClientAssertionBuilderTest {


    @Test
    public void testBuildAssertion() throws Exception {
        HTTPClientAssertionBuilder ab = new HTTPClientAssertionBuilder();
        Assertion a = ab.buildAssertion();
        assertTrue(a instanceof JaxbAssertion);
        assertTrue(a instanceof HTTPClientAssertionBuilder.HTTPClientPolicyAssertion);
        assertEquals(new ClientPolicyCalculator().getDataClassName(), a.getName());
        assertFalse(a.isOptional());
    }

    @Test
    public void testHTTPCLientPolicyAssertionEqual() throws Exception {
        HTTPClientAssertionBuilder ab = new HTTPClientAssertionBuilder();
        JaxbAssertion<HTTPClientPolicy>  a = ab.buildAssertion();
        a.setData(new HTTPClientPolicy());
        assertTrue(a.equal(a));
        JaxbAssertion<HTTPClientPolicy> b = ab.buildAssertion();
        b.setData(new HTTPClientPolicy());
        assertTrue(a.equal(b));
        HTTPClientPolicy pa = new HTTPClientPolicy();
        a.setData(pa);
        assertTrue(a.equal(a));
        HTTPClientPolicy pb = new HTTPClientPolicy();
        b.setData(pb);
        assertTrue(a.equal(b));
        pa.setDecoupledEndpoint("http://localhost:9999/decoupled_endpoint");
        assertFalse(a.equal(b));
    }
}
