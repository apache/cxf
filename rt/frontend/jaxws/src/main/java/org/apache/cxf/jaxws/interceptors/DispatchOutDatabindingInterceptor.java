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

package org.apache.cxf.jaxws.interceptors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.Service;
import javax.xml.ws.Service.Mode;

import org.w3c.dom.Node;

import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.databinding.source.NodeDataWriter;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.handler.logical.DispatchLogicalHandlerInterceptor;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.XMLMessage;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

public class DispatchOutDatabindingInterceptor extends AbstractOutDatabindingInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(DispatchOutDatabindingInterceptor.class);
    private DispatchOutDatabindingEndingInterceptor ending;
    
    private final Service.Mode mode;
    private MessageFactory soap11Factory;
    private MessageFactory soap12Factory;
    
    public DispatchOutDatabindingInterceptor(Mode mode) {
        super(Phase.WRITE);
        ending = new DispatchOutDatabindingEndingInterceptor();
        
        this.mode = mode;
    }
    private MessageFactory getFactory(SoapMessage message) throws SOAPException {
        return getFactory(message.getVersion());
    }
    private synchronized MessageFactory getFactory(SoapVersion version) throws SOAPException {
        if (version instanceof Soap11) {
            if (soap11Factory == null) { 
                soap11Factory = MessageFactory.newInstance();
            } 
            return soap11Factory;
        }
        if (soap12Factory == null) {
            soap12Factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        }
        return soap12Factory;
    }
    public void handleMessage(Message message) throws Fault {
        Object obj = null;
        Object result = message.getContent(List.class);
        if (result != null) {
            obj = ((List)result).get(0);
            message.setContent(Object.class, obj);
        } else {
            obj = message.getContent(Object.class);
        }
        message.removeContent(Object.class);

        if (obj == null) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("DISPATCH_OBJECT_CANNOT_BE_NULL", LOG));
        }

        if (message instanceof SoapMessage) {
            Source source = null;
            if (mode == Service.Mode.PAYLOAD) {
                source = handlePayloadMode(obj, message);
            } else {
                if (obj instanceof DataSource) {
                    throw new Fault(
                                    new org.apache.cxf.common.i18n.Message(
                                        "DISPATCH_OBJECT_NOT_SUPPORTED_SOAPBINDING",
                                        LOG, "DataSource", "MESSAGE"));
                } else if (obj instanceof SOAPMessage) {
                    source = handleSOAPMessage(obj, message);

                } else if (obj instanceof Source) {
                    source = (Source)obj;
                } 
            }
            
            PostDispatchOutLogicalHandlerInterceptor postSoap = 
                new PostDispatchOutLogicalHandlerInterceptor();
            message.getInterceptorChain().add(postSoap);
            
            message.setContent(Source.class, source);
        } else if (message instanceof XMLMessage) {
            if (obj instanceof SOAPMessage) {
                throw new Fault(
                                new org.apache.cxf.common.i18n.Message(
                                    "DISPATCH_OBJECT_NOT_SUPPORTED_XMLBINDING",
                                    LOG, "SOAPMessage", "PAYLOAD/MESSAGE"));
            }

            if (mode == Service.Mode.PAYLOAD && obj instanceof DataSource) {
                throw new Fault(
                                new org.apache.cxf.common.i18n.Message(
                                    "DISPATCH_OBJECT_NOT_SUPPORTED_XMLBINDING",
                                    LOG, "DataSource", "PAYLOAD"));
            }

            if (obj instanceof DataSource) {
                message.setContent(DataSource.class, obj);
            } else if (obj instanceof Source) {
                message.setContent(Source.class, obj);
            } else {
                // JAXB element
                try {
                    org.apache.cxf.service.Service service = 
                        message.getExchange().get(org.apache.cxf.service.Service.class);
                    DataWriter<XMLStreamWriter> dataWriter = getDataWriter(message, service,
                                                                           XMLStreamWriter.class);
                    W3CDOMStreamWriter xmlWriter = new W3CDOMStreamWriter();
                    dataWriter.write(obj, xmlWriter);                    

                    Source source = new DOMSource(xmlWriter.getDocument().getDocumentElement());   
                    message.setContent(Source.class, source);                
                } catch (ParserConfigurationException e) {
                    throw new Fault(new org.apache.cxf.common.i18n.Message("EXCEPTION_WRITING_OBJECT",
                                                                           LOG), e);
                }                
            }
        }
        message.getInterceptorChain().add(ending);
    }
    
    private Source handleSOAPMessage(Object obj, Message message) {
        SOAPMessage soapMessage = (SOAPMessage)obj;
        try {
            //workaround bug in Sun SAAJ impl
            soapMessage.getSOAPPart().getEnvelope();
        } catch (SOAPException e1) {
            //ignore
        }
        Source source = new DOMSource(soapMessage.getSOAPPart());
        
        if (soapMessage.countAttachments() > 0) {
            if (message.getAttachments() == null) {
                message.setAttachments(new ArrayList<Attachment>(soapMessage
                        .countAttachments()));
            }
            Iterator<AttachmentPart> it = CastUtils.cast(soapMessage.getAttachments());
            while (it.hasNext()) {
                AttachmentPart part = it.next();
                AttachmentImpl att = new AttachmentImpl(part.getContentId());
                try {
                    att.setDataHandler(part.getDataHandler());
                } catch (SOAPException e) {
                    throw new Fault(e);
                }
                Iterator<MimeHeader> it2 = CastUtils.cast(part.getAllMimeHeaders());
                while (it2.hasNext()) {
                    MimeHeader header = it2.next();
                    att.setHeader(header.getName(), header.getValue());
                }
                message.getAttachments().add(att);
            }
        }
        return source;
    }

    private Source handlePayloadMode(Object obj, Message message) {
        Source source = null;
        if (obj instanceof SOAPMessage || obj instanceof DataSource) {
            throw new Fault(
                            new org.apache.cxf.common.i18n.Message(
                                "DISPATCH_OBJECT_NOT_SUPPORTED_SOAPBINDING",
                                LOG, obj.getClass(), "PAYLOAD"));
        } else if (obj instanceof Source) {
            source = (Source)obj;
        } else {
            //JAXB
            try {
                org.apache.cxf.service.Service service = 
                    message.getExchange().get(org.apache.cxf.service.Service.class);
                SOAPMessage msg = newSOAPMessage(null, ((SoapMessage)message).getVersion());
                DataWriter<Node> dataWriter = getDataWriter(message, service, Node.class);
                dataWriter.write(obj, msg.getSOAPBody());
                //msg.writeTo(System.out);
                source = new DOMSource(DOMUtils.getChild(msg.getSOAPBody(), Node.ELEMENT_NODE));
            } catch (Exception e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("EXCEPTION_WRITING_OBJECT",
                                                                       LOG), e);
            }
        }
        return source;
    }

    private class DispatchOutDatabindingEndingInterceptor extends AbstractOutDatabindingInterceptor {
        public DispatchOutDatabindingEndingInterceptor() {
            super(Phase.WRITE_ENDING);
        }
        
        public void handleMessage(Message message) throws Fault {                
            
            XMLStreamWriter xmlWriter = message.getContent(XMLStreamWriter.class);
            SOAPMessage soapMessage = message.getContent(SOAPMessage.class);
            Source source = message.getContent(Source.class);
            DataSource dataSource = message.getContent(DataSource.class);
            
            try {
                if (xmlWriter != null) {
                    xmlWriter.flush();
                } else if (soapMessage != null) {
                    Map<String, List<String>> heads 
                        = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
                    if (heads == null) {
                        heads = new HashMap<String, List<String>>();
                        message.put(Message.PROTOCOL_HEADERS, heads);
                    }
                    
                    soapMessage.saveChanges();
                    Iterator<MimeHeader> smh = CastUtils.cast(soapMessage.getMimeHeaders().getAllHeaders());
                    while (smh.hasNext()) {
                        MimeHeader head = smh.next();
                        if ("Content-Type".equals(head.getName())) {
                            message.put(Message.CONTENT_TYPE, head.getValue());
                        } else if (!"Content-Length".equals(head.getName())) {
                            if (!heads.containsKey(head.getName())) {
                                heads.put(head.getName(), new ArrayList<String>());
                            }
                            List<String>l = heads.get(head.getName());
                            l.add(head.getValue());
                        }
                    }
                    OutputStream os = message.getContent(OutputStream.class);
                    soapMessage.writeTo(os);
                    os.flush();
                } else if (source != null) {
                    if (message.getAttachments() != null
                        && !message.getAttachments().isEmpty()) {
                        message.put(AttachmentOutInterceptor.WRITE_ATTACHMENTS, Boolean.TRUE);
                        new AttachmentOutInterceptor().handleMessage(message);
                    }
                    OutputStream os = message.getContent(OutputStream.class);
                    doTransform(source, os);
                    os.flush();
                } else if (dataSource != null) {
                    if (message.getAttachments() != null
                        && !message.getAttachments().isEmpty()) {
                        message.put(AttachmentOutInterceptor.WRITE_ATTACHMENTS, Boolean.TRUE);
                        new AttachmentOutInterceptor().handleMessage(message);
                    }
                    message.put(Message.CONTENT_TYPE, dataSource.getContentType());
                    OutputStream os = message.getContent(OutputStream.class);
                    doTransform(dataSource, os);
                    os.flush();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new Fault(new org.apache.cxf.common.i18n.Message("EXCEPTION_WRITING_OBJECT", LOG, ex));
            }
        }      
    }
    
    //This interceptor is invoked after DispatchLogicalHandlerInterceptor, converts Source to SOAPMessage
    private class PostDispatchOutLogicalHandlerInterceptor extends AbstractInDatabindingInterceptor {

        public PostDispatchOutLogicalHandlerInterceptor() {
            super(Phase.PRE_MARSHAL);
            addAfter(DispatchLogicalHandlerInterceptor.class.getName());            
        }

        public void handleMessage(Message message) throws Fault {
            Object obj = null;
            
            //Convert Source to SOAPMessage
            if (message instanceof SoapMessage) {
                Source source = message.getContent(Source.class);
                message.removeContent(Source.class);

                //workaround bug in Sun SAAJ impl where
                //source doesn't work if the SOAPPart was already 
                //created from a source
                if (source instanceof DOMSource) {
                    DOMSource ds = (DOMSource)source;
                    if (ds.getNode() instanceof SOAPPart) {
                        try {
                            ((SOAPPart)ds.getNode()).getEnvelope();
                        } catch (SOAPException e) {
                            //ignore
                        }
                    }
                }
                
                if (mode == Service.Mode.PAYLOAD) {
                    // Input is Source in payload mode, need to wrap it
                    // with a SOAPMessage
                    try {
                        obj = newSOAPMessage(null, ((SoapMessage)message).getVersion());
                        DataWriter<Node> dataWriter = new NodeDataWriter();
                        dataWriter.write(source, ((SOAPMessage)obj).getSOAPBody());
                    } catch (Exception e) {
                        throw new Fault(new org.apache.cxf.common.i18n.Message("EXCEPTION_WRITING_OBJECT",
                                                                               LOG), e);
                    }
                } else {
                    try {
                        MessageFactory msgFactory = getFactory((SoapMessage)message);
                        SOAPMessage msg = msgFactory.createMessage();
                        msg.getSOAPPart().setContent(source);
                        if (message.getAttachments() != null) {
                            for (Attachment att : message.getAttachments()) {
                                AttachmentPart part = msg.createAttachmentPart(att.getDataHandler());
                                if (att.getId() != null) {
                                    part.setContentId(att.getId());
                                }
                                for (Iterator<String> it = att.getHeaderNames(); it.hasNext();) {
                                    String s = it.next();
                                    part.setMimeHeader(s, att.getHeader(s));
                                }
                                msg.addAttachmentPart(part);
                            }
                        }
                        msg.saveChanges();
                        obj = msg;                    
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new Fault(e);
                    }
                }                
                message.setContent(SOAPMessage.class, obj);
            } 
        }
    }

    private SOAPMessage newSOAPMessage(InputStream is, SoapVersion version) throws Exception {
        SOAPMessage msg = null;
        
        MimeHeaders headers = new MimeHeaders();
        MessageFactory msgFactory = getFactory(version);
        
        if (is != null) {
            msg = msgFactory.createMessage(headers, is);
        } else {
            msg = msgFactory.createMessage();
        }        
        msg.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
        msg.getSOAPPart().getEnvelope().addNamespaceDeclaration(WSDLConstants.NP_SCHEMA_XSD,
                                                                WSDLConstants.NS_SCHEMA_XSD);
        msg.getSOAPPart().getEnvelope().addNamespaceDeclaration(WSDLConstants.NP_SCHEMA_XSI,
                                                                WSDLConstants.NS_SCHEMA_XSI);
        return msg;
    }

    private void doTransform(Object obj, OutputStream os) throws TransformerException, IOException {
        if (obj instanceof Source) {
            Transformer transformer = XMLUtils.newTransformer();
            transformer.transform((Source)obj, new StreamResult(os));
        }
        if (obj instanceof DataSource) {
            InputStream is = ((DataSource)obj).getInputStream();
            IOUtils.copy(is, os);
            is.close();
        }
    }

}
