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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.binding.xml.XMLFault;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.ClientFaultConverter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.hello_world_xml_http.bare.types.MyComplexStructType;
import org.junit.Test;

public class XMLFaultInterceptorsTest extends TestBase {

    @Test
    public void testRuntimeExceptionOfImpl() throws Exception {

        String ns = "http://apache.org/hello_world_xml_http/wrapped";
        common("/wsdl/hello_world_xml_wrapped.wsdl", new QName(ns, "XMLPort"), MyComplexStructType.class);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xmlMessage.setContent(OutputStream.class, baos);
        xmlMessage.setContent(XMLStreamWriter.class, StaxUtils.createXMLStreamWriter(baos));
        xmlMessage.setContent(Exception.class, new Fault(new RuntimeException("dummy exception")));
        XMLFaultOutInterceptor xfo = new XMLFaultOutInterceptor("phase1");
        chain.add(xfo);
        InHelpInterceptor ih = new InHelpInterceptor("phase2");
        ClientFaultConverter cfc = new ClientFaultConverter("phase3");
        XMLFaultInInterceptor xfi = new XMLFaultInInterceptor("phase3");
        chain.add(ih);
        chain.add(cfc);
        chain.add(xfi);
        chain.doIntercept(xmlMessage);
        assertNotNull(xmlMessage.getContent(Exception.class));
        assertTrue(xmlMessage.getContent(Exception.class) instanceof XMLFault);
        XMLFault xfault = (XMLFault) xmlMessage.getContent(Exception.class);
        assertTrue("check message expected - dummy exception",
                xfault.getMessage().indexOf("dummy exception") >= 0);
    }

    private class InHelpInterceptor extends AbstractInDatabindingInterceptor {
        InHelpInterceptor(String phase) {
            super(phase);
        }

        public void handleMessage(Message message) {

            try {
                ByteArrayOutputStream baos = (ByteArrayOutputStream) message.getContent(OutputStream.class);
                ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
                message.setContent(InputStream.class, bis);
                XMLStreamReader xsr = StaxUtils.createXMLStreamReader(bis);
                xsr.nextTag();
                message.setContent(XMLStreamReader.class, xsr);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

    }
}
