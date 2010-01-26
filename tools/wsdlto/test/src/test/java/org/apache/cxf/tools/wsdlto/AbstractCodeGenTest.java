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
package org.apache.cxf.tools.wsdlto;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.core.FrontEndProfile;
import org.apache.cxf.tools.wsdlto.core.PluginLoader;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.JAXWSContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExternalResource;

public abstract class AbstractCodeGenTest extends ProcessorTestBase {

    //CHECKSTYLE:OFF
    @Rule 
    public ExternalResource envRule = new ExternalResource() {
        protected void before() throws Throwable {
            File classFile = tmpDir.newFolder("classes");
            classFile.mkdir();
            classLoader = new URLClassLoader(new URL[] {classFile.toURI().toURL()},
                                             Thread.currentThread().getContextClassLoader());
            env.put(ToolConstants.CFG_COMPILE, ToolConstants.CFG_COMPILE);
            env.put(ToolConstants.CFG_CLASSDIR, classFile.toString());
            env.put(FrontEndProfile.class, PluginLoader.getInstance().getFrontEndProfile("jaxws"));
            env.put(DataBindingProfile.class, PluginLoader.getInstance().getDataBindingProfile("jaxb"));
            env.put(ToolConstants.CFG_IMPL, "impl");
            env.put(ToolConstants.CFG_OUTPUTDIR, tmpDir.getRoot().toString());
            env.put(ToolConstants.CFG_SUPPRESS_WARNINGS, true);
        }
    };
    //CHECKSTYLE:ON
    
    
    protected JAXWSContainer processor;
    protected ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        processor = new JAXWSContainer(null);
    
    }

    @After
    public void tearDown() {
        processor = null;
        env = null;
        super.tearDown();
    }

}
