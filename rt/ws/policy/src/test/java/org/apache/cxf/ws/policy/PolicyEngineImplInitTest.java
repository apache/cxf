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

package org.apache.cxf.ws.policy;

import jakarta.annotation.Resource;
import org.apache.cxf.Bus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;



import static org.junit.Assert.assertNotNull;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class PolicyEngineImplInitTest {


    @Resource
    Bus bus;

    @Before
    public void setUp() throws Exception {
        assertNotNull(bus);
    }

    @After
    public void tearDown() throws Exception {
        bus.shutdown(true);
    }

    @Test
    public void testPolicyInterceptors() throws Exception {
        Assert.assertEquals("should only have one PolicyOutInterceptor", bus.getOutInterceptors().size(), 1);
        Assert.assertEquals("should only have one PolicyInInterceptor", bus.getInInterceptors().size(), 1);
    }
}
