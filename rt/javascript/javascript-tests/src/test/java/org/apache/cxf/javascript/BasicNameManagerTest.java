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

package org.apache.cxf.javascript;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Simple tests of the name manager.
 */
public class BasicNameManagerTest {

    @Test
    public void testPrefixGeneration() {
        BasicNameManager manager = new BasicNameManager();
        String jsp = manager.transformURI("http://ThisIsA.Test");
        assertEquals("ThisIsA_Test", jsp);
        jsp = manager.transformURI("uri:george.bill:fred");
        assertEquals("george_bill_fred", jsp);
    }

}