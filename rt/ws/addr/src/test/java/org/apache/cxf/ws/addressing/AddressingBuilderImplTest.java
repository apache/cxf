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

package org.apache.cxf.ws.addressing;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;




public class AddressingBuilderImplTest extends Assert {
    private AddressingBuilderImpl builder;

    @Before
    public void setUp() {
        builder = new AddressingBuilderImpl();
    }

    @Test
    public void testGetAddressingProperties() throws Exception {
        AddressingProperties properties = builder.newAddressingProperties();
        assertNotNull("expected AddressingProperties ", properties);
        assertNotSame("unexpected same properties",
                      builder.newAddressingProperties(),
                      properties);
    }

    @Test
    public void testGetAddressingConstants() throws Exception {
        AddressingConstants constants = builder.newAddressingConstants();
        assertNotNull("expected AddressingConstants ", constants);
        assertNotSame("unexpected same constants",
                      builder.newAddressingConstants(),
                      constants);
    }
}
