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

import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import org.apache.cxf.binding.xml.XMLConstants;
import org.apache.cxf.binding.xml.XMLFault;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.NSStack;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

public class XMLFaultOutInterceptor extends AbstractOutDatabindingInterceptor {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(XMLFaultOutInterceptor.class);

    public XMLFaultOutInterceptor() {
        super(Phase.MARSHAL);
    }
    public XMLFaultOutInterceptor(String phase) {
        super(phase);
    }

    public void handleMessage(Message message) throws Fault {
        
        if (mustPropogateException(message)) {
            throw (Fault) message.getContent(Exception.class);
        }
        
        message.put(org.apache.cxf.message.Message.RESPONSE_CODE, new Integer(500));
        NSStack nsStack = new NSStack();
        nsStack.push();

        XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
        Fault f = (Fault) message.getContent(Exception.class);
        XMLFault xmlFault = XMLFault.createFault(f);
        try {
            nsStack.add(XMLConstants.NS_XML_FORMAT);
            String prefix = nsStack.getPrefix(XMLConstants.NS_XML_FORMAT);
            StaxUtils.writeStartElement(writer, prefix, XMLFault.XML_FAULT_ROOT, 
                    XMLConstants.NS_XML_FORMAT);
            StaxUtils.writeStartElement(writer, prefix, XMLFault.XML_FAULT_STRING, 
                    XMLConstants.NS_XML_FORMAT);
            Throwable t = xmlFault.getCause();
            writer.writeCharacters(t == null ? xmlFault.getMessage() : t.toString());
            // fault string
            writer.writeEndElement();
            // call StaxUtils to write Fault detail.
            
            if (xmlFault.getDetail() != null) {
                StaxUtils.writeStartElement(writer, prefix, XMLFault.XML_FAULT_DETAIL,
                        XMLConstants.NS_XML_FORMAT);
                StaxUtils.writeNode(DOMUtils.getChild(xmlFault.getDetail(), Node.ELEMENT_NODE), 
                                    writer, false);
                writer.writeEndElement();
            }
            // fault root
            writer.writeEndElement();
            writer.flush();
        } catch (XMLStreamException xe) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("XML_WRITE_EXC", BUNDLE), xe);
        }
    }
    
    @Override
    public void handleFault(Message message) throws Fault {
        if (mustPropogateException(message)) {
            throw (Fault) message.getContent(Exception.class);
        }
    }
    
    protected boolean mustPropogateException(Message m) {
        return Boolean.TRUE.equals(m.getExchange().get(Message.PROPOGATE_EXCEPTION));
    }
}
