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

package org.apache.cxf.ws.rm.blueprint;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class RMBPHandlerTest {
    @Test
    public void testGetSchemaLocation() {
        RMBPHandler handler = new RMBPHandler();

        assertNotNull(handler.getSchemaLocation("http://cxf.apache.org/ws/rm/manager"));
        assertNotNull(handler.getSchemaLocation("http://schemas.xmlsoap.org/ws/2005/02/rm/policy"));
        assertNotNull(handler.getSchemaLocation("http://docs.oasis-open.org/ws-rx/wsrmp/200702"));
    }

}