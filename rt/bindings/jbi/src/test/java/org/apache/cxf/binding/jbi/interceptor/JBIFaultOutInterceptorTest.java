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
package org.apache.cxf.binding.jbi.interceptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ResourceBundle;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;

import org.apache.cxf.binding.jbi.JBIMessage;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.junit.Assert;
import org.junit.Test;

public class JBIFaultOutInterceptorTest extends Assert {
    
    @Test
    public void testPhase() throws Exception {
        PhaseInterceptor<JBIMessage> interceptor = new JBIFaultOutInterceptor();
        assertEquals(Phase.MARSHAL, interceptor.getPhase());
    }
    
    @Test
    public void testNoWriter() throws Exception {
        PhaseInterceptor<JBIMessage> interceptor = new JBIFaultOutInterceptor();
        try {
            JBIMessage msg = new JBIMessage(new MessageImpl());
            interceptor.handleMessage(msg);
            fail("Should have thrown an exception");
        } catch (IllegalStateException e) {
            // ok
        }
    }

    @Test
    public void testNoFault() throws Exception {
        PhaseInterceptor<JBIMessage> interceptor = new JBIFaultOutInterceptor();
        try {
            JBIMessage msg = new JBIMessage(new MessageImpl());
            msg.setContent(XMLStreamWriter.class, 
                    XMLOutputFactory.newInstance().createXMLStreamWriter(new ByteArrayOutputStream()));
            interceptor.handleMessage(msg);
            fail("Should have thrown an exception");
        } catch (IllegalStateException e) {
            // ok
        }
    }

    @Test
    public void testEmptyFault() throws Exception {
        PhaseInterceptor<JBIMessage> interceptor = new JBIFaultOutInterceptor();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(baos); 
        JBIMessage msg = new JBIMessage(new MessageImpl());
        msg.setContent(XMLStreamWriter.class, writer);
        msg.setContent(Exception.class, new Exception("My fault"));
        interceptor.handleMessage(msg);
        writer.close();
        Document doc = DOMUtils.readXml(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals("fault", doc.getDocumentElement().getFirstChild().getNodeName());
    }

    @Test
    public void testDetailedFault() throws Exception {
        PhaseInterceptor<JBIMessage> interceptor = new JBIFaultOutInterceptor();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(baos); 
        JBIMessage msg = new JBIMessage(new MessageImpl());
        Fault f = new Fault(new Message("FAULT", (ResourceBundle) null));
        f.getOrCreateDetail();
        f.getDetail().appendChild(f.getDetail().getOwnerDocument().createElementNS("urn:test", "myDetails"));
        msg.setContent(XMLStreamWriter.class, writer);
        msg.setContent(Exception.class, f);
        interceptor.handleMessage(msg);
        writer.close();
        Document doc = DOMUtils.readXml(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals("urn:test", doc.getDocumentElement().getFirstChild().getNamespaceURI());
        assertEquals("myDetails", doc.getDocumentElement().getFirstChild().getNodeName());
    }

}
