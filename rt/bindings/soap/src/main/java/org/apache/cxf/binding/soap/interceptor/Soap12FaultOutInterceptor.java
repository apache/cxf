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

package org.apache.cxf.binding.soap.interceptor;

import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.URIMappingInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

public class Soap12FaultOutInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(URIMappingInterceptor.class);

    public Soap12FaultOutInterceptor() {
        super(Phase.MARSHAL);
    }

    public void handleMessage(SoapMessage message) throws Fault {
        LOG.info(getClass() + (String) message.get(SoapMessage.CONTENT_TYPE));
        message.put(org.apache.cxf.message.Message.RESPONSE_CODE, new Integer(500));
        
        XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
        Fault f = (Fault)message.getContent(Exception.class);

        SoapFault fault = SoapFault.createFault(f, message.getVersion());       
        
        try {
            Map<String, String> namespaces = fault.getNamespaces();
            for (Map.Entry<String, String> e : namespaces.entrySet()) {
                writer.writeNamespace(e.getKey(), e.getValue());
            }

            String ns = message.getVersion().getNamespace();
            String defaultPrefix = StaxUtils.getUniquePrefix(writer, ns, true);

            writer.writeStartElement(defaultPrefix, "Fault", ns);

            writer.writeStartElement(defaultPrefix, "Code", ns);
            writer.writeStartElement(defaultPrefix, "Value", ns);
       
            writer.writeCharacters(fault.getCodeString(getFaultCodePrefix(writer, fault.getFaultCode()), 
                                                       defaultPrefix));
            writer.writeEndElement();
            
            if (fault.getSubCode() != null) {
                writer.writeStartElement(defaultPrefix, "Subcode", ns);
                writer.writeCharacters(fault.getSubCodeString(getFaultCodePrefix(writer, fault.getSubCode()), 
                                                              defaultPrefix));                
                writer.writeEndElement();
            }
            writer.writeEndElement();

            writer.writeStartElement(defaultPrefix, "Reason", ns);
            writer.writeStartElement(defaultPrefix, "Text", ns);
            writer.writeAttribute("xml", "http://www.w3.org/XML/1998/namespace", "lang ", getLangCode());
            if (fault.getMessage() != null) {
                writer.writeCharacters(fault.getMessage());
            } else {
                writer.writeCharacters("Fault occurred while processing.");
            }
            writer.writeEndElement();
            writer.writeEndElement();

            if (fault.hasDetails()) {
                Element detail = fault.getDetail();
                writer.writeStartElement(defaultPrefix, "Detail", ns);

                NodeList details = detail.getChildNodes();
                for (int i = 0; i < details.getLength(); i++) {
                    StaxUtils.writeNode(details.item(i), writer, true);
                }

                // Details
                writer.writeEndElement();
            }

            if (fault.getRole() != null) {
                writer.writeStartElement("faultactor");
                writer.writeCharacters(fault.getRole());
                writer.writeEndElement();
            }

            // Fault
            writer.writeEndElement();
        } catch (XMLStreamException xe) {
            throw new Fault(new Message("XML_WRITE_EXC", LOG), xe);
        }
    }
    
    private String getLangCode() {        
        String code = LOG.getResourceBundle().getLocale().getDisplayLanguage();
        if (StringUtils.isEmpty(code)) {
            return "en";
        }
        return code;
    }
}
