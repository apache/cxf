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

package org.apache.cxf.tools.java2js.processor;

import java.io.File;

import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.java2js.JavaToJS;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.core.FrontEndProfile;
import org.apache.cxf.tools.wsdlto.core.PluginLoader;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.JAXWSContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JavaToJSProcessorTest extends ProcessorTestBase {
    JavaToJSProcessor processor = new JavaToJSProcessor();
    String classPath = "";
    
    @Before
    public void startUp() throws Exception {
        env = new ToolContext();
        classPath = System.getProperty("java.class.path");
        System.setProperty("java.class.path", getClassPath());
    }
    
    @After
    public void tearDown() {
        super.tearDown();
        System.setProperty("java.class.path", classPath);
    }


    @Test
    public void testSimpleClass() throws Exception {
        env.put(ToolConstants.CFG_OUTPUTFILE, output.getPath() + "/doc_wrapped_bare.js");
        env.put(ToolConstants.CFG_CLASSNAME, "org.apache.cxf.tools.fortest.simple.Hello");
        processor.setEnvironment(env);
        processor.process();

        File jsFile = new File(output, "doc_wrapped_bare.js");
        assertTrue("Fail to generate JS file: " + jsFile.toString(), jsFile.exists());
        // need some additional validation.
    }

    @Test
    public void testDocLitUseClassPathFlag() throws Exception {
        File classFile = new java.io.File(output.getCanonicalPath() + "/classes");
        classFile.mkdir();

        System.setProperty("java.class.path", getClassPath() + classFile.getCanonicalPath()
                           + File.separatorChar);

        env.put(ToolConstants.CFG_COMPILE, ToolConstants.CFG_COMPILE);
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        env.put(FrontEndProfile.class, PluginLoader.getInstance().getFrontEndProfile("jaxws"));
        env.put(DataBindingProfile.class, PluginLoader.getInstance().getDataBindingProfile("jaxb"));
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        env.put(ToolConstants.CFG_PACKAGENAME, "org.apache.cxf.classpath");
        env.put(ToolConstants.CFG_CLASSDIR, output.getCanonicalPath() + "/classes");
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl/hello_world_doc_lit.wsdl"));
        JAXWSContainer w2jProcessor = new JAXWSContainer(null);
        w2jProcessor.setContext(env);
        w2jProcessor.execute();

        System.setProperty("java.class.path", "");

        //      test flag
        String[] args = new String[] {"-o",
                                      "java2wsdl.js",
                                      "-jsutils",
                                      "-cp",
                                      classFile.getCanonicalPath(),
                                      "-d",
                                      output.getPath(),
                                      "org.apache.cxf.classpath.Greeter"};
        JavaToJS.main(args);
        File jsFile = new File(output, "java2wsdl.js");
        assertTrue("Generate JS Fail", jsFile.exists());
    }

}
