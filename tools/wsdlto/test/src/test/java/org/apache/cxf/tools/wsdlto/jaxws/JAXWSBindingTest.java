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
package org.apache.cxf.tools.wsdlto.jaxws;

import java.io.File;
import java.io.FileInputStream;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.core.FrontEndProfile;
import org.apache.cxf.tools.wsdlto.core.PluginLoader;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.JAXWSContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JAXWSBindingTest extends ProcessorTestBase {
    private JAXWSContainer processor;

    @Before
    public void setUp() throws Exception {

        super.setUp();
        env.put(FrontEndProfile.class, PluginLoader.getInstance().getFrontEndProfile("jaxws"));
        env.put(DataBindingProfile.class, PluginLoader.getInstance().getDataBindingProfile("jaxb"));
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());

        processor = new JAXWSContainer(null);
    }

    @After
    public void tearDown() {
        super.tearDown();
        processor = null;
        env = null;
    }

    @Test
    public void testDateTypeAdapter() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/echo_date.wsdl"));
        env.put(ToolConstants.CFG_BINDING, getLocation("/wsdl2java_wsdl/echo_date.xjb"));
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);

        String path = "/org/apache/cxf/tools/fortest/date";
        File sei = new File(output, path + "/EchoDate.java");
        assertTrue(sei.exists());
        assertTrue(IOUtils.toString(new FileInputStream(sei)).indexOf("java.util.Date") != -1);
    }
}