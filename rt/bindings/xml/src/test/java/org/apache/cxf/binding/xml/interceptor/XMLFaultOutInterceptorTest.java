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

package org.apache.cxf.binding.xml.interceptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

import org.apache.cxf.binding.xml.XMLConstants;
import org.apache.cxf.binding.xml.XMLFault;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.hello_world_doc_lit.PingMeFault;
import org.apache.hello_world_doc_lit.types.FaultDetail;
import org.junit.Before;
import org.junit.Test;

public class XMLFaultOutInterceptorTest extends TestBase {

    XMLFaultOutInterceptor out = new XMLFaultOutInterceptor();

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testFault() throws Exception {
        FaultDetail detail = new FaultDetail();
        detail.setMajor((short)2);
        detail.setMinor((short)1);
        PingMeFault fault = new PingMeFault("TEST_FAULT", detail);

        XMLFault xmlFault = XMLFault.createFault(new Fault(fault));
        Element el = xmlFault.getOrCreateDetail();
        JAXBContext ctx = JAXBContext.newInstance(FaultDetail.class);
        Marshaller m = ctx.createMarshaller();
        m.marshal(detail, el);
        
        OutputStream outputStream = new ByteArrayOutputStream();
        xmlMessage.setContent(OutputStream.class, outputStream);
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(outputStream);
        xmlMessage.setContent(XMLStreamWriter.class, writer);
        xmlMessage.setContent(Exception.class, xmlFault);
        
        out.handleMessage(xmlMessage);
        outputStream.flush();

        XMLStreamReader reader = getXMLReader();
        DepthXMLStreamReader dxr = new DepthXMLStreamReader(reader);
        
        dxr.nextTag();
        StaxUtils.toNextElement(dxr);
        assertEquals(XMLConstants.NS_XML_FORMAT, dxr.getNamespaceURI());
        assertEquals(XMLFault.XML_FAULT_ROOT, dxr.getLocalName());
        
        dxr.nextTag();
        StaxUtils.toNextElement(dxr);
        assertEquals(XMLFault.XML_FAULT_STRING, dxr.getLocalName());
        assertEquals(fault.toString(), dxr.getElementText());
        
        dxr.nextTag();
        StaxUtils.toNextElement(dxr);
        assertEquals(XMLFault.XML_FAULT_DETAIL, dxr.getLocalName());
        
        dxr.nextTag();
        StaxUtils.toNextElement(dxr);
        assertEquals("faultDetail", dxr.getLocalName());
    }

    private XMLStreamReader getXMLReader() throws Exception {
        ByteArrayOutputStream o = (ByteArrayOutputStream) xmlMessage.getContent(OutputStream.class);
        InputStream in = new ByteArrayInputStream(o.toByteArray());
        return StaxUtils.createXMLStreamReader(in);
    }
}
