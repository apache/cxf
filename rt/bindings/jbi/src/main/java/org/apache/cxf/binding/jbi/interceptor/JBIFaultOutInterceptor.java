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

import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

import org.apache.cxf.binding.jbi.JBIConstants;
import org.apache.cxf.binding.jbi.JBIFault;
import org.apache.cxf.binding.jbi.JBIMessage;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.NSStack;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

public class JBIFaultOutInterceptor extends AbstractPhaseInterceptor<JBIMessage> {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JBIFaultOutInterceptor.class);

    public JBIFaultOutInterceptor() {
        super(Phase.MARSHAL);
    }

    public void handleMessage(JBIMessage message) throws Fault {
        message.put(org.apache.cxf.message.Message.RESPONSE_CODE, new Integer(500));
        NSStack nsStack = new NSStack();
        nsStack.push();

        
        
        try {
            XMLStreamWriter writer = getWriter(message);
            Fault fault = getFault(message);
            JBIFault jbiFault = JBIFault.createFault(fault);
            nsStack.add(JBIConstants.NS_JBI_BINDING);
            String prefix = nsStack.getPrefix(JBIConstants.NS_JBI_BINDING);
            StaxUtils.writeStartElement(writer, prefix, JBIFault.JBI_FAULT_ROOT, 
                                        JBIConstants.NS_JBI_BINDING);
            if (!jbiFault.hasDetails()) {
                Element faultString = DOMUtils.createDocument().createElement("fault");
                faultString.setTextContent(jbiFault.getCause().getMessage());
                StaxUtils.writeNode(faultString, writer, true);   
            } else {
                Element detail = jbiFault.getDetail();
                Element elem = DOMUtils.getFirstElement(detail);
                if (elem != null) {
                    StaxUtils.writeNode(elem, writer, true);    
                }              
            }
            writer.writeEndElement();
            writer.flush();
            
        } catch (XMLStreamException xe) {
            throw new Fault(new Message("XML_WRITE_EXC", BUNDLE), xe);
        }
    }

    protected Fault getFault(JBIMessage message) {
        Exception e = message.getContent(Exception.class);
        Fault fault;
        if (e == null) {
            throw new IllegalStateException(new Message("NO_EXCEPTION", BUNDLE).toString());
        } else if (e instanceof Fault) {
            fault = (Fault) e;
        } else {
            fault = new Fault(e);
        }
        return fault;
    }
    
    protected XMLStreamWriter getWriter(JBIMessage message) {
        XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
        if (writer == null) {
            throw new IllegalStateException(new Message("NO_XML_STREAM_WRITER", BUNDLE).toString());
        }
        return writer;
    }
}
