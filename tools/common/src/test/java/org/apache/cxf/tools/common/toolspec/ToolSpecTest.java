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

package org.apache.cxf.tools.common.toolspec;

import org.apache.cxf.tools.common.ToolException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ToolSpecTest {
    ToolSpec toolSpec;

    @Test
    public void testConstruct() {
        toolSpec = null;
        toolSpec = new ToolSpec();
        assertNotNull(toolSpec);
    }

    @Test
    public void testConstructFromInputStream() {
        String tsSource = "parser/resources/testtool.xml";
        try {
            toolSpec = new ToolSpec(getClass().getResourceAsStream(tsSource), false);
        } catch (ToolException e) {
            throw new RuntimeException(e);
        }
        assertNull(toolSpec.getAnnotation());
    }

    @Test
    public void testGetParameterDefault() throws Exception {
        String tsSource = "parser/resources/testtool.xml";

        toolSpec = new ToolSpec(getClass().getResourceAsStream(tsSource), false);

        assertNull(toolSpec.getAnnotation());
        assertNull(toolSpec.getParameterDefault("namespace"));
        assertNull(toolSpec.getParameterDefault("wsdlurl"));
    }

    @Test
    public void testGetStreamRefName1() throws Exception {
        String tsSource = "parser/resources/testtool1.xml";
        toolSpec = new ToolSpec(getClass().getResourceAsStream(tsSource), false);
        assertEquals("test getStreamRefName failed", toolSpec.getStreamRefName("streamref"), "namespace");
    }

    @Test
    public void testGetStreamRefName2() throws Exception {
        String tsSource = "parser/resources/testtool2.xml";
        toolSpec = new ToolSpec(getClass().getResourceAsStream(tsSource), false);
        assertEquals("test getStreamRefName2 failed", toolSpec.getStreamRefName("streamref"), "wsdlurl");
    }

    @Test
    public void testIsValidInputStream() throws Exception {
        String tsSource = "parser/resources/testtool1.xml";
        toolSpec = new ToolSpec(getClass().getResourceAsStream(tsSource), false);
        assertTrue(toolSpec.isValidInputStream("testID"));
        assertFalse(toolSpec.isValidInputStream("dummyID"));
        assertTrue(toolSpec.getInstreamIds().size() == 1);
    }

    @Test
    public void testGetHandler() throws Exception {
        String tsSource = "parser/resources/testtool1.xml";
        toolSpec = new ToolSpec(getClass().getResourceAsStream(tsSource), false);
        assertNotNull(toolSpec.getHandler());
        assertNotNull(toolSpec.getHandler(this.getClass().getClassLoader()));
    }

    @Test
    public void testGetOutstreamIds() throws Exception {
        String tsSource = "parser/resources/testtool2.xml";
        toolSpec = new ToolSpec(getClass().getResourceAsStream(tsSource), false);
        assertTrue(toolSpec.getOutstreamIds().size() == 1);
    }
}
