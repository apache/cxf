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

import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;
import org.apache.neethi.Assertion;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 */
public class HTTPClientAssertionBuilderTest extends Assert {

    @Test
    public void testBuildCompatible() throws Exception {
        HTTPClientAssertionBuilder ab = new HTTPClientAssertionBuilder();
        JaxbAssertion<HTTPClientPolicy>  a = ab.buildAssertion();
        HTTPClientPolicy pa = new HTTPClientPolicy();
        a.setData(pa);
        JaxbAssertion<HTTPClientPolicy> b = ab.buildAssertion();
        HTTPClientPolicy pb = new HTTPClientPolicy();
        b.setData(pb);
        JaxbAssertion<HTTPClientPolicy> c = 
            JaxbAssertion.cast(ab.buildCompatible(a, b), HTTPClientPolicy.class);
        assertNotNull(c);        
    }
    
    @Test
    public void testBuildAssertion() throws Exception {
        HTTPClientAssertionBuilder ab = new HTTPClientAssertionBuilder();
        Assertion a = ab.buildAssertion();
        assertTrue(a instanceof JaxbAssertion);
        assertTrue(a instanceof HTTPClientAssertionBuilder.HTTPClientPolicyAssertion);
        assertEquals(PolicyUtils.HTTPCLIENTPOLICY_ASSERTION_QNAME, a.getName());
        assertTrue(!a.isOptional());
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
        assertTrue(!a.equal(b));  
    }    
}
