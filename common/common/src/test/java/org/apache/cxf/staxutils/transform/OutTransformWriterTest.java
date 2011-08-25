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
package org.apache.cxf.staxutils.transform;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.staxutils.StaxUtils;
import org.junit.Assert;
import org.junit.Test;


public class OutTransformWriterTest extends Assert {

    @Test
    public void testDefaultNamespace() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(os, "UTF-8");
        
        Map<String, String> outMap = new HashMap<String, String>();
        outMap.put("{http://testbeans.com}*", "{http://testbeans.com/v2}*");
        OutTransformWriter transformWriter = new OutTransformWriter(writer,
                                                                    outMap,
                                                                    Collections.<String, String>emptyMap(),
                                                                    Collections.<String>emptyList(),
                                                                    false,
                                                                    "http://testbeans.com/v2");
        JAXBContext context = JAXBContext.newInstance(TestBean.class);
        Marshaller m = context.createMarshaller();
        m.marshal(new TestBean(), transformWriter);
        
        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<testBean xmlns=\"http://testbeans.com/v2\"><bean/></testBean>";
        assertEquals(expected, os.toString());
    }
    
    @Test
    public void testNamespaceConversion() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(os, "UTF-8");
        
        Map<String, String> outMap = new HashMap<String, String>();
        outMap.put("{http://testbeans.com}testBean", "{http://testbeans.com/v2}testBean");
        outMap.put("{http://testbeans.com}bean", "{http://testbeans.com/v3}bean");
        OutTransformWriter transformWriter = new OutTransformWriter(writer,
                                                                    outMap,
                                                                    Collections.<String, String>emptyMap(),
                                                                    Collections.<String>emptyList(),
                                                                    false,
                                                                    null);
        JAXBContext context = JAXBContext.newInstance(TestBean.class);
        Marshaller m = context.createMarshaller();
        m.marshal(new TestBean(), transformWriter);
        
        String xmlPI = "<?xml version='1.0' encoding='UTF-8'?>";
        String start = "<ps1:testBean xmlns:ps1=\"http://testbeans.com/v2\"";
        String expected1 = xmlPI + start
            + " xmlns:ps2=\"http://testbeans.com/v3\"><ps2:bean/></ps1:testBean>";
        String expected2 = xmlPI + start
            + "><ps2:bean xmlns:ps2=\"http://testbeans.com/v3\"/></ps1:testBean>";
        String out = os.toString();
        assertTrue("Output \"" + out + "\" does not match expected values",
                expected1.equals(out) || expected2.equals(out));
        
    }
    
    @Test
    public void testNamespaceConversionAndDefaultNS() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(os, "UTF-8");
        
        Map<String, String> outMap = new HashMap<String, String>();
        outMap.put("{http://testbeans.com}testBean", "{http://testbeans.com/v2}testBean");
        outMap.put("{http://testbeans.com}bean", "{http://testbeans.com/v3}bean");
        OutTransformWriter transformWriter = new OutTransformWriter(writer,
                                                                    outMap,
                                                                    Collections.<String, String>emptyMap(),
                                                                    Collections.<String>emptyList(),
                                                                    false,
                                                                    "http://testbeans.com/v2");
        JAXBContext context = JAXBContext.newInstance(TestBean.class);
        Marshaller m = context.createMarshaller();
        m.marshal(new TestBean(), transformWriter);
        
        String xmlPI = "<?xml version='1.0' encoding='UTF-8'?>";
        String start = "<testBean xmlns=\"http://testbeans.com/v2\"";
        String expected1 = xmlPI + start
            + " xmlns:ps2=\"http://testbeans.com/v3\"><ps2:bean/></testBean>";
        String expected2 = xmlPI + start
            + "><ps2:bean xmlns:ps2=\"http://testbeans.com/v3\"/></testBean>";
        String out = os.toString();
        assertTrue("Output \"" + out + "\" does not match expected values",
                expected1.equals(out) || expected2.equals(out));
    }
 
    @XmlRootElement(name = "testBean", namespace = "http://testbeans.com")
    public static class TestBean {
        private TestBean2 bean = new TestBean2();

        @XmlElement(name = "bean", namespace = "http://testbeans.com")
        public void setBean(TestBean2 bean) {
            this.bean = bean;
        }

        public TestBean2 getBean() {
            return bean;
        }
    }
    
    private static class TestBean2 {
    }
}
