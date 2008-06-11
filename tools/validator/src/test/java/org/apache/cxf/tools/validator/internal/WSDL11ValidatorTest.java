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

package org.apache.cxf.tools.validator.internal;

import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.junit.Assert;
import org.junit.Test;

public class WSDL11ValidatorTest extends Assert {
    private ToolContext context = new ToolContext();
    
    @Test
    public void testWSDLImport() throws Exception {
        String wsdlSource = getClass().getResource("resources/a.wsdl").toURI().toString();
        context.put(ToolConstants.CFG_WSDLURL, wsdlSource);
        WSDL11Validator validator = new WSDL11Validator(null, context);
        try {
            assertFalse(validator.isValid());
        } catch (Exception e) {
            assertTrue(e.getMessage(), e.getMessage()
                           .indexOf("Caused by {http://apache.org/hello_world/messages}"
                                              + "[portType:GreeterA][operation:sayHi] not exist.") != -1);
        }
    }
}
