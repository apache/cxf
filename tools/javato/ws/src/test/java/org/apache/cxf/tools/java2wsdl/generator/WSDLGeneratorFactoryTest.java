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

package org.apache.cxf.tools.java2wsdl.generator;

import org.apache.cxf.tools.java2wsdl.generator.wsdl11.WSDL11Generator;
import org.apache.cxf.wsdl.WSDLConstants;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WSDLGeneratorFactoryTest {

    @Test
    public void testNewWSDL11Generator() {
        WSDLGeneratorFactory factory = new WSDLGeneratorFactory();
        factory.setWSDLVersion(WSDLConstants.WSDLVersion.WSDL11);
        AbstractGenerator<?> generator = factory.newGenerator();
        assertNotNull(generator);
        assertTrue(generator instanceof WSDL11Generator);
    }
}