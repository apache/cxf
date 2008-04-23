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

import org.apache.cxf.transports.http.configuration.HTTPServerPolicy;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;
import org.apache.neethi.Assertion;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 */
public class HTTPServerAssertionBuilderTest extends Assert {

    @Test
    public void testBuildCompatible() throws Exception {
        HTTPServerAssertionBuilder ab = new HTTPServerAssertionBuilder();
        JaxbAssertion<HTTPServerPolicy>  a = ab.buildAssertion();
        HTTPServerPolicy pa = new HTTPServerPolicy();
        a.setData(pa);
        JaxbAssertion<HTTPServerPolicy> b = ab.buildAssertion();
        HTTPServerPolicy pb = new HTTPServerPolicy();
        b.setData(pb);
        JaxbAssertion<HTTPServerPolicy> c = 
            JaxbAssertion.cast(ab.buildCompatible(a, b), HTTPServerPolicy.class);
        assertNotNull(c);        
    }
    
    @Test
    public void testBuildAssertion() throws Exception {
        HTTPServerAssertionBuilder ab = new HTTPServerAssertionBuilder();
        Assertion a = ab.buildAssertion();
        assertTrue(a instanceof JaxbAssertion);
        assertTrue(a instanceof HTTPServerAssertionBuilder.HTTPServerPolicyAssertion);
        assertEquals(PolicyUtils.HTTPSERVERPOLICY_ASSERTION_QNAME, a.getName());
        assertTrue(!a.isOptional());
    }
    
    @Test
    public void testHTTPServerPolicyAssertionEqual() throws Exception {
        HTTPServerAssertionBuilder ab = new HTTPServerAssertionBuilder();
        JaxbAssertion<HTTPServerPolicy>  a = ab.buildAssertion(); 
        a.setData(new HTTPServerPolicy());
        assertTrue(a.equal(a));        
        JaxbAssertion<HTTPServerPolicy> b = ab.buildAssertion();
        b.setData(new HTTPServerPolicy());
        assertTrue(a.equal(b));
        HTTPServerPolicy pa = new HTTPServerPolicy();
        a.setData(pa);
        assertTrue(a.equal(a));
        HTTPServerPolicy pb = new HTTPServerPolicy();
        b.setData(pb);
        assertTrue(a.equal(b));
        pa.setSuppressClientSendErrors(true);
        assertTrue(!a.equal(b));       
    }
    
}
