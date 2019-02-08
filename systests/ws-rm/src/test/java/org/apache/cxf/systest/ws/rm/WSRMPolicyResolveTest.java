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
package org.apache.cxf.systest.ws.rm;


import org.apache.cxf.test.AbstractCXFSpringTest;
import org.apache.cxf.testutil.common.TestUtil;
import org.springframework.context.support.GenericApplicationContext;

import org.junit.Test;

import static org.junit.Assert.assertEquals;




//CXF-4875
public class WSRMPolicyResolveTest extends AbstractCXFSpringTest {
    public static final String PORT = TestUtil.getPortNumber(WSRMPolicyResolveTest.class);
    /** {@inheritDoc}*/
    @Override
    protected void additionalSpringConfiguration(GenericApplicationContext context) throws Exception {
    }
    
    @Test
    public void testHello() throws Exception {
        BasicDocEndpoint port = getApplicationContext().getBean("TestClient",
                  BasicDocEndpoint.class);
        Object retObj = port.echo("Hello");
        assertEquals("Hello", retObj);
    }

    /** {@inheritDoc}*/
    @Override
    protected String[] getConfigLocations() {
        return new String[] {"classpath:/org/apache/cxf/systest/ws/rm/wsrm-policy-resolve.xml" };
    }
}