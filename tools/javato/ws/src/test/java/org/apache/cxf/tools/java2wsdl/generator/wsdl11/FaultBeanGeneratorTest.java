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
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.java2wsdl.processor.JavaToWSDLProcessor;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FaultBeanGeneratorTest extends ProcessorTestBase {
    JavaToWSDLProcessor processor = new JavaToWSDLProcessor();

    @Before
    public void setUp() throws Exception {
        processor.setEnvironment(env);
    }

    private ServiceInfo getServiceInfo() {
        return processor.getServiceBuilder().createService();
    }

    @Test
    public void testGenFaultBean() throws Exception {
        String testingClass = "org.apache.cxf.tools.fortest.cxf523.Database";
        env.put(ToolConstants.CFG_CLASSNAME, testingClass);

        FaultBeanGenerator generator = new FaultBeanGenerator();
        generator.setToolContext(env);
        generator.setServiceModel(getServiceInfo());

        generator.generate(output);

        String pkgBase = "org/apache/cxf/tools/fortest/cxf523/jaxws";
        
        assertEquals(2, new File(output, pkgBase).listFiles().length);
        File faultBeanClass = new File(output, pkgBase + "/DBServiceFaultBean.java");
        assertTrue(faultBeanClass.exists());

        assertFileEquals("expected/DBServiceFaultBean.java.source", faultBeanClass);
    }

    @Test
    public void testGenFaultBeanWithCustomization() throws Exception {
        String testingClass = "org.apache.cxf.tools.fortest.jaxws.rpc.GreeterFault";
        env.put(ToolConstants.CFG_CLASSNAME, testingClass);
        env.put(ToolConstants.CFG_VERBOSE, Boolean.TRUE);

        FaultBeanGenerator generator = new FaultBeanGenerator();
        generator.setServiceModel(getServiceInfo());
        generator.setToolContext(env);
        generator.generate(output);

        String pkgBase = "org/apache/cxf/tools/fortest/jaxws/rpc/types";
        assertEquals(2, new File(output, pkgBase).listFiles().length);
        File faultBeanClass = new File(output, pkgBase + "/FaultDetail.java");
        assertTrue(faultBeanClass.exists());

        assertFileEquals("expected/FaultDetail.java.source", faultBeanClass);
    }

    @Test
    public void testGetExceptionClasses() throws Exception {
        Class<?> seiClass = Class.forName("org.apache.hello_world.Greeter");
        FaultBeanGenerator generator = new FaultBeanGenerator();
        Set<Class<?>> classes = new HashSet<>();
        for (Method method : seiClass.getMethods()) {
            classes.addAll(generator.getExceptionClasses(method));
        }
        assertEquals(0, classes.size());

        classes.clear();

        seiClass = Class.forName("org.apache.cxf.tools.fortest.cxf523.Database");
        for (Method method : seiClass.getMethods()) {
            classes.addAll(generator.getExceptionClasses(method));
        }
        assertEquals(1, classes.size());
        assertEquals("org.apache.cxf.tools.fortest.cxf523.DBServiceFault",
                     classes.iterator().next().getName());
    }
}
