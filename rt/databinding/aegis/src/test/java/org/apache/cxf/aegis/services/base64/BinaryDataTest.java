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
package org.apache.cxf.aegis.services.base64;

import org.w3c.dom.Node;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.junit.Test;

public class BinaryDataTest extends AbstractAegisTest {
    @Test
    public void testBinary() throws Exception {
        createService(BinaryDataService.class);

        runTests();
    }    
      
    private void runTests() throws Exception {
        Node res = invoke("BinaryDataService",
                          "/org/apache/cxf/aegis/services/base64/binary.xml");

        addNamespace("b", "http://base64.services.aegis.cxf.apache.org");
        assertValid("//b:return[text()='OK']", res);

        res = invoke("BinaryDataService",                     
                     "/org/apache/cxf/aegis/services/base64/binaryEmpty.xml");
        assertValid("//b:return[text()='OK']", res);
    }

}
