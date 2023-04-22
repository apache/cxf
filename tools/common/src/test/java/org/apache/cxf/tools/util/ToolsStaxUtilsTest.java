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

package org.apache.cxf.tools.util;

import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.Tag;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ToolsStaxUtilsTest extends ProcessorTestBase {

    @Test
    public void testGetTags() throws Exception {
        Tag tag1 = ToolsStaxUtils.getTagTree(getClass().getResourceAsStream("resources/test2.wsdl"));
        assertEquals(1, tag1.getTags().size());
        Tag def1 = tag1.getTags().get(0);
        assertEquals(6, def1.getTags().size());
        Tag types1 = def1.getTags().get(0);
        Tag schema1 = types1.getTags().get(0);
        assertEquals(4, schema1.getTags().size());

        Tag tag2 = ToolsStaxUtils.getTagTree(getClass().getResourceAsStream("resources/test3.wsdl"));
        assertEquals(1, tag2.getTags().size());
        Tag def2 = tag2.getTags().get(0);
        assertEquals(6, def2.getTags().size());
        Tag types2 = def2.getTags().get(0);
        Tag schema2 = types2.getTags().get(0);
        assertEquals(4, schema2.getTags().size());

        assertTagEquals(schema1, schema2);
    }
}