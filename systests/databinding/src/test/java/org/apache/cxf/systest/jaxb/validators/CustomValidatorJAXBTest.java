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

package org.apache.cxf.systest.jaxb.validators;

import org.apache.cxf.testutil.common.TestUtil;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
@ContextConfiguration(locations = { "classpath:jaxbCustomValidators.xml" })
public class CustomValidatorJAXBTest extends AbstractJUnit4SpringContextTests {
    static final String PORT = TestUtil.getPortNumber(CustomValidatorJAXBTest.class);

    @Test
    public void cleanTest() {
        HelloWorld client = applicationContext
                .getBean("testClient", HelloWorld.class);

        PassedObject hi = client.sayHi(new PassedObject("John", "Doe"));
        Assert.assertTrue("Expected: 'Hello John Doe' Actual: " + hi.getName(),
                "Hello John Doe".equals(hi.getName()));

    }

    @Test
    public void sendNullTest() {
        HelloWorld client = applicationContext
                .getBean("testClient", HelloWorld.class);

        PassedObject hi = client.sayHi(new PassedObject());
        Assert.assertTrue("Expected: 'Hello null null' Actual: '" + hi.getName() + "'",
                "Hello null null".equals(hi.getName()));

    }

    @Test
    public void returnNullTest() {
        HelloWorld client = applicationContext
                .getBean("testClient", HelloWorld.class);

        PassedObject hi = client.returnNull(new PassedObject("John", "Doe"));
        Assert.assertTrue("Expected: 'Hello null' Actual: '" + hi.getName() + "'",
                "Hello null".equals(hi.getName()));

    }
}
