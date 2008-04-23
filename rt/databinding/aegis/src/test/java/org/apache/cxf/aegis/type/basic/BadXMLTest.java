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
package org.apache.cxf.aegis.type.basic;

import junit.framework.TestCase;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.TypeMapping;
import org.junit.Test;

public class BadXMLTest extends TestCase {
    TypeMapping mapping;


    @Test
    public void testBadDescriptorNS() throws Exception {
        AegisContext context = new AegisContext();
        context.initialize();
        mapping = context.getTypeMapping();
        try {
            mapping.getTypeCreator().createType(BadBeanDescriptor.class);
            fail("No exception was thrown");
        } catch (DatabindingException e) {
            // this is supposed to happen
        }

    }
}
