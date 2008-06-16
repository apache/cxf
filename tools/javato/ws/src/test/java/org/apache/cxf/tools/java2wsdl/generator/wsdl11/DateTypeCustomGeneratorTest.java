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
import java.net.URI;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.fortest.date.EchoCalendar;
import org.apache.cxf.tools.fortest.date.EchoDate;
import org.apache.cxf.tools.java2wsdl.processor.JavaToWSDLProcessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DateTypeCustomGeneratorTest extends ProcessorTestBase {
    DateTypeCustomGenerator gen = new DateTypeCustomGenerator();
    JavaToWSDLProcessor processor = new JavaToWSDLProcessor();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        processor.setEnvironment(env);
    }

    @After
    public void tearDown() {
        //super.tearDown();
    }

    @Test
    public void testGetJAXBCustFile() {
        gen.setWSDLName("demo");
        assertTrue(gen.getJAXBCustFile(new File(".")).toString().endsWith("demo.xjb"));
    }

    private ServiceInfo getServiceInfo(Class clz) {
        env.put(ToolConstants.CFG_CLASSNAME, clz.getName());
        return processor.getServiceBuilder().createService();
    }

    @Test
    public void getGetDateType() {
        assertNull(gen.getDateType());
        gen.setServiceModel(getServiceInfo(org.apache.cxf.tools.fortest.jaxws.rpc.GreeterFault.class));
        assertNull(gen.getDateType());

        gen.setServiceModel(getServiceInfo(EchoDate.class));
        assertEquals(Date.class, gen.getDateType());

        gen.setServiceModel(getServiceInfo(EchoCalendar.class));
        assertEquals(Calendar.class, gen.getDateType());
    }

    @Test
    public void testGenerateEmbedStyle() throws Exception {
        gen.setWSDLName("date_embed");
        gen.setServiceModel(getServiceInfo(EchoDate.class));
        assertEquals(Date.class, gen.getDateType());

        URI expectedFile = getClass().getResource("expected/date_embed.xml").toURI();
        assertFileEquals(new File(expectedFile), gen.generate(output));

        gen.setWSDLName("calendar_embed");
        gen.setServiceModel(getServiceInfo(EchoCalendar.class));
        assertEquals(Calendar.class, gen.getDateType());

        expectedFile = getClass().getResource("expected/calendar_embed.xml").toURI();
        assertFileEquals(new File(expectedFile), gen.generate(output));
    }

    @Test
    public void testGenerateExternalStyle() throws Exception {
        gen.setAllowImports(true);
        gen.addSchemaFiles(Arrays.asList(new String[]{"hello_schema1.xsd", "hello_schema2.xsd"}));

        gen.setWSDLName("date_external");
        gen.setServiceModel(getServiceInfo(EchoDate.class));
        assertEquals(Date.class, gen.getDateType());

        URI expectedFile = getClass().getResource("expected/date.xjb").toURI();
        assertFileEquals(new File(expectedFile), gen.generate(output));

        gen.setWSDLName("calendar_external");
        gen.setServiceModel(getServiceInfo(EchoCalendar.class));
        assertEquals(Calendar.class, gen.getDateType());

        expectedFile = getClass().getResource("expected/calendar.xjb").toURI();
        assertFileEquals(new File(expectedFile), gen.generate(output));
    }
}