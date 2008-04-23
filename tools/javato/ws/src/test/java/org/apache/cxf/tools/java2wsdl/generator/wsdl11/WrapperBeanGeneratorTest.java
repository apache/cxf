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

import javax.xml.bind.annotation.XmlList;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.java2wsdl.processor.JavaToWSDLProcessor;
import org.apache.cxf.tools.util.AnnotationUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WrapperBeanGeneratorTest extends ProcessorTestBase {
    JavaToWSDLProcessor processor = new JavaToWSDLProcessor();
    String classPath = "";
    ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        super.setUp();        
        classPath = System.getProperty("java.class.path");
        String pathSeparator = System.getProperty("path.separator");
        System.setProperty("java.class.path", getClassPath() + pathSeparator + output.getPath());
        classLoader = AnnotationUtil.getClassLoader(Thread.currentThread().getContextClassLoader());
        processor.setEnvironment(env);
        
    }

    @After
    public void tearDown() {
        super.tearDown();
        System.setProperty("java.class.path", classPath);
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
    
}
