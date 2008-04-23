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


import java.io.OutputStream;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.model.SoapHeaderInfo;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.headers.Header;
import org.apache.cxf.headers.HeaderManager;
import org.apache.cxf.headers.HeaderProcessor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.WriteOnCloseOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.staxutils.StaxUtils;

public class SoapOutInterceptor extends AbstractSoapInterceptor {
    public static final String WROTE_ENVELOPE_START = "wrote.envelope.start";
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(SoapOutInterceptor.class);
    
    private Bus bus;
    
    public SoapOutInterceptor(Bus b) {
        super(Phase.WRITE);
        bus = b;
    }
    public SoapOutInterceptor(Bus b, String phase) {
        super(phase);
        bus = b;
    }
    
    public void handleMessage(SoapMessage message) {
        // Yes this is ugly, but it avoids us from having to implement any kind of caching strategy
        if (!MessageUtils.isTrue(message.get(WROTE_ENVELOPE_START))) {
            writeSoapEnvelopeStart(message);
            
            OutputStream os = message.getContent(OutputStream.class);
            // Unless we're caching the whole message in memory skip the envelope writing
            // if there's a fault later.
            if (!(os instanceof WriteOnCloseOutputStream) && !MessageUtils.isDOMPresent(message)) {
                message.put(WROTE_ENVELOPE_START, Boolean.TRUE);
            }
        }

        // Add a final interceptor to write end elements
        message.getInterceptorChain().add(new SoapOutEndingInterceptor());
    }
    
    private void writeSoapEnvelopeStart(SoapMessage message) {
        SoapVersion soapVersion = message.getVersion();
        try {            
            XMLStreamWriter xtw = message.getContent(XMLStreamWriter.class);
            xtw.setPrefix(soapVersion.getPrefix(), soapVersion.getNamespace());
            xtw.writeStartElement(soapVersion.getPrefix(), 
                                  soapVersion.getEnvelope().getLocalPart(),
                                  soapVersion.getNamespace());
            xtw.writeNamespace(soapVersion.getPrefix(), soapVersion.getNamespace());
            
            boolean preexistingHeaders = message.hasHeaders();

            if (preexistingHeaders) {
                xtw.writeStartElement(soapVersion.getPrefix(), 
                                      soapVersion.getHeader().getLocalPart(),
                                      soapVersion.getNamespace());   
                List<Header> hdrList = message.getHeaders();
                for (Header header : hdrList) {
                    DataBinding b = header.getDataBinding();
                    if (b == null) {
                        HeaderProcessor hp = bus.getExtension(HeaderManager.class)
                                .getHeaderProcessor(header.getName().getNamespaceURI());
                        if (hp != null) {
                            b = hp.getDataBinding();
                        }
                    }
                    if (b != null) {
                        b.createWriter(XMLStreamWriter.class)
                            .write(header.getObject(), xtw);
                    } else {
                        Element node = (Element)header.getObject();
                        StaxUtils.copy(node, xtw);
                    }
                }
            }
            boolean endedHeader = handleHeaderPart(preexistingHeaders, message);
            if (preexistingHeaders && !endedHeader) {
                xtw.writeEndElement();
            }

            xtw.writeStartElement(soapVersion.getPrefix(), 
                                  soapVersion.getBody().getLocalPart(),
                                  soapVersion.getNamespace());
            
            // Interceptors followed such as Wrapped/RPC/Doc Interceptor will write SOAP body
        } catch (XMLStreamException e) {
            throw new SoapFault(
                new org.apache.cxf.common.i18n.Message("XML_WRITE_EXC", BUNDLE), e, soapVersion.getSender());
        }
    }
    
    private boolean handleHeaderPart(boolean preexistingHeaders, SoapMessage message) {
        //add MessagePart to soapHeader if necessary
        boolean endedHeader = false;
        Exchange exchange = message.getExchange();
        BindingOperationInfo bop = (BindingOperationInfo)exchange.get(BindingOperationInfo.class
                                                                            .getName());
        if (bop == null) {
            return endedHeader;
        }
        
        XMLStreamWriter xtw = message.getContent(XMLStreamWriter.class);        
        boolean startedHeader = false;
        BindingOperationInfo unwrappedOp = bop;
        if (bop.isUnwrapped()) {
            unwrappedOp = bop.getWrappedOperation();
        }
        boolean client = isRequestor(message);
        BindingMessageInfo bmi = client ? unwrappedOp.getInput() : unwrappedOp.getOutput();
        BindingMessageInfo wrappedBmi = client ? bop.getInput() : bop.getOutput();
        
        if (bmi == null) {
            return endedHeader;
        }
        
        List<MessagePartInfo> parts = wrappedBmi.getMessageInfo().getMessageParts();
        if (parts.size() > 0) {
            MessageContentsList objs = MessageContentsList.getContentsList(message);
            if (objs == null) {
                return endedHeader;
            }
            SoapVersion soapVersion = message.getVersion();
            List<SoapHeaderInfo> headers = bmi.getExtensors(SoapHeaderInfo.class);
            if (headers == null) {
                return endedHeader;
            }            

            for (SoapHeaderInfo header : headers) {
                MessagePartInfo part = header.getPart();
                if (part.getIndex() >= objs.size()) {
                    // The optional out of band header is not a part of parameters of the method
                    continue;
                }
                Object arg = objs.get(part);
                objs.remove(part);
                if (!(startedHeader || preexistingHeaders)) {
                    try {
                        xtw.writeStartElement(soapVersion.getPrefix(), 
                                              soapVersion.getHeader().getLocalPart(),
                                              soapVersion.getNamespace());
                    } catch (XMLStreamException e) {
                        throw new SoapFault(new org.apache.cxf.common.i18n.Message("XML_WRITE_EXC", BUNDLE), 
                            e, soapVersion.getSender());
                    }
                    startedHeader = true;
                }
                DataWriter<XMLStreamWriter> dataWriter = getDataWriter(message);
                dataWriter.write(arg, header.getPart(), xtw);
            }
            
            if (startedHeader || preexistingHeaders) {
                try {
                    xtw.writeEndElement();
                    endedHeader = true;
                } catch (XMLStreamException e) {
                    throw new SoapFault(new org.apache.cxf.common.i18n.Message("XML_WRITE_EXC", BUNDLE), 
                        e, soapVersion.getSender());
                }
            }
        }
        return endedHeader;
    }       
    
    protected boolean isRequestor(Message message) {
        return Boolean.TRUE.equals(message.containsKey(Message.REQUESTOR_ROLE));
    }

    protected DataWriter<XMLStreamWriter> getDataWriter(Message message) {
        Service service = ServiceModelUtil.getService(message.getExchange());
        DataWriter<XMLStreamWriter> dataWriter = service.getDataBinding().createWriter(XMLStreamWriter.class);
        dataWriter.setAttachments(message.getAttachments());
        
        if (dataWriter == null) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("NO_DATAWRITER", BUNDLE, service
                .getName()));
        }

        return dataWriter;
    }
        
    public class SoapOutEndingInterceptor extends AbstractSoapInterceptor {
        public SoapOutEndingInterceptor() {
            super(SoapOutEndingInterceptor.class.getName(), Phase.WRITE_ENDING);
        }

        public void handleMessage(SoapMessage message) throws Fault {
            SoapVersion soapVersion = message.getVersion();
            try {
                XMLStreamWriter xtw = message.getContent(XMLStreamWriter.class);
                if (xtw != null) {
                    xtw.writeEndElement();            
                    // Write Envelope end element
                    xtw.writeEndElement();
                    xtw.writeEndDocument();
                    
                    xtw.flush();
                }
            } catch (XMLStreamException e) {
                throw new SoapFault(new org.apache.cxf.common.i18n.Message("XML_WRITE_EXC", BUNDLE), e,
                                    soapVersion.getSender());
            }
        }

    }    
}
