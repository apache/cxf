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

package org.apache.cxf.tools.java2wsdl.generator.wsdl11;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;

import javax.xml.bind.annotation.XmlList;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.java2wsdl.processor.JavaToWSDLProcessor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class WrapperBeanGeneratorTest extends ProcessorTestBase {
    JavaToWSDLProcessor processor = new JavaToWSDLProcessor();
    ClassLoader classLoader;

    //CHECKSTYLE:OFF
    @Rule 
    public ExternalResource envRule = new ExternalResource() {
        protected void before() throws Throwable {
            System.setProperty("java.class.path", getClassPath() + tmpDir.getRoot().getCanonicalPath()
                                                  + File.separatorChar);
            classLoader = new URLClassLoader(new URL[] {tmpDir.getRoot().toURI().toURL()},
                                             Thread.currentThread().getContextClassLoader());
        }
    };
    //CHECKSTYLE:ON
    
    
    @Before
    public void setUp() throws Exception {
        processor.setEnvironment(env);
    }

    private ServiceInfo getServiceInfo() {
        return processor.getServiceBuilder().createService();
    }
    
    @Test
    public void testGenInAnotherPackage() throws Exception {
        String testingClass = "org.apache.cxf.tools.fortest.withannotation.doc.GreeterNoWrapperBean";
        env.put(ToolConstants.CFG_CLASSNAME, testingClass);
        
        WrapperBeanGenerator generator = new WrapperBeanGenerator();
        generator.setServiceModel(getServiceInfo());
        
        generator.generate(output);

        String pkgBase = "org/apache/cxf";
        File requestWrapperClass = new File(output, pkgBase + "/EchoDataBean.java");
        assertTrue(requestWrapperClass.exists());
        String contents = IOUtils.toString(new FileInputStream(requestWrapperClass));
        assertTrue(contents.indexOf("org.apache.cxf.tools.fortest.withannotation.doc") != -1);
        
        File responseWrapperClass = new File(output, pkgBase + "/EchoDataBeanResponse.java");
        assertTrue(responseWrapperClass.exists());

        requestWrapperClass = new File(output, pkgBase + "/SayHi.java");
        assertTrue(requestWrapperClass.exists());
        responseWrapperClass = new File(output, pkgBase + "/SayHiResponse.java");
        assertTrue(responseWrapperClass.exists());
    }

    @Test
    public void testArray() throws Exception {
        String testingClass = "org.apache.cxf.tools.fortest.withannotation.doc.GreeterArray";
        env.put(ToolConstants.CFG_CLASSNAME, testingClass);
        
        WrapperBeanGenerator generator = new WrapperBeanGenerator();
        generator.setServiceModel(getServiceInfo());
        
        generator.generate(output);

        String pkgBase = "org/apache/cxf/tools/fortest/withannotation/doc/jaxws";
        File requestWrapperClass = new File(output, pkgBase + "/SayIntArray.java");
        assertTrue(requestWrapperClass.exists());
        String contents = IOUtils.toString(new FileInputStream(requestWrapperClass));
        assertTrue(contents.indexOf("int[]") != -1);
        
        File responseWrapperClass = new File(output, pkgBase + "/SayIntArrayResponse.java");
        assertTrue(responseWrapperClass.exists());
        contents = IOUtils.toString(new FileInputStream(responseWrapperClass));
        assertTrue(contents.indexOf("_return") != -1);
        
        requestWrapperClass = new File(output, pkgBase + "/SayStringArray.java");
        assertTrue(requestWrapperClass.exists());
        responseWrapperClass = new File(output, pkgBase + "/SayStringArrayResponse.java");
        assertTrue(responseWrapperClass.exists());

        requestWrapperClass = new File(output, pkgBase + "/SayTestDataBeanArray.java");
        assertTrue(requestWrapperClass.exists());
        responseWrapperClass = new File(output, pkgBase + "/SayTestDataBeanArrayResponse.java");
        assertTrue(responseWrapperClass.exists());
        contents = IOUtils.toString(new FileInputStream(requestWrapperClass));
        assertTrue(contents.indexOf("org.apache.cxf.tools.fortest.withannotation.doc.TestDataBean[]") != -1);
    }
    
    @Test
    public void testGenJaxbAnno() throws Exception {
        String testingClass = "org.apache.cxf.tools.fortest.withannotation.doc.SayHiNoWrapperBean";
        env.put(ToolConstants.CFG_CLASSNAME, testingClass);
        
        WrapperBeanGenerator generator = new WrapperBeanGenerator();
        generator.setServiceModel(getServiceInfo());
        
        generator.generate(output);
        Class clz = classLoader.loadClass("org.apache.cxf.SayHi");
        assertNotNull(clz);
        Field field = clz.getDeclaredField("arg0");
        assertNotNull(field.getAnnotation(XmlList.class));
    }
    
    @Test
    public void testGenGeneric() throws Exception {
        String testingClass = "org.apache.cxf.tools.fortest.withannotation.doc.EchoGenericNoWrapperBean";
        env.put(ToolConstants.CFG_CLASSNAME, testingClass);
        
        WrapperBeanGenerator generator = new WrapperBeanGenerator();
        generator.setServiceModel(getServiceInfo());
        
        generator.generate(output);

        String pkgBase = "org/apache/cxf";
        File requestWrapperClass = new File(output, pkgBase + "/EchoGeneric.java");
        assertTrue(requestWrapperClass.exists());
        String contents = IOUtils.toString(new FileInputStream(requestWrapperClass));
        assertTrue(contents.indexOf("public java.util.List<java.lang.String> get") != -1);
        
        File responseWrapperClass = new File(output, pkgBase + "/EchoGenericResponse.java");
        assertTrue(responseWrapperClass.exists());
        contents = IOUtils.toString(new FileInputStream(responseWrapperClass));
        assertTrue(contents.indexOf("public java.util.List<java.lang.String> getReturn()") != -1);
    }
    
}
