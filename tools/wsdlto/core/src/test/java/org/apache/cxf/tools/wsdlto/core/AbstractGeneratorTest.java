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

package org.apache.cxf.tools.wsdlto.core;

import java.io.Writer;

import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.util.FileWriterUtil;
import org.junit.Before;
import org.junit.Test;

public class AbstractGeneratorTest extends ProcessorTestBase {

    DummyGenerator gen;
    ToolContext context;
    FileWriterUtil util;

    String packageName = "org.apache";
    String className = "Hello";

    @Before
    public void setUp() throws Exception {
        super.setUp();

        gen = new DummyGenerator();
        util = new FileWriterUtil(output.toString());

        context = new ToolContext();
        context.put(ToolConstants.CFG_OUTPUTDIR, output.toString());
        gen.setEnvironment(context);

        Writer writer = util.getWriter(packageName, className + ".java");
        writer.write("hello world");
        writer.flush();
        writer.close();
    }

    @Test
    public void testKeep() throws Exception {
        context.put(ToolConstants.CFG_GEN_NEW_ONLY, "keep");
        assertNull(gen.parseOutputName(packageName, className));
    }

    @Test
    public void testOverwrite() throws Exception {
        assertNotNull(gen.parseOutputName(packageName, className));
    }
}

