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

package org.apache.cxf.jaxws.handler.logical;


import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.XMLMessage;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;


public class LogicalMessageImpl implements LogicalMessage {
    private static final Logger LOG = LogUtils.getL7dLogger(LogicalMessageImpl.class);
    private final LogicalMessageContextImpl msgContext;
    
    public LogicalMessageImpl(LogicalMessageContextImpl lmctx) {
        msgContext = lmctx;
    }

    public Source getPayload() {
        Source source = null;

        Service.Mode mode = msgContext.getWrappedMessage().getExchange().get(Service.Mode.class);
        
        if (mode != null) {
            //Dispatch/Provider case
            source = handleDispatchProviderCase(mode);
        } else {
            Message message = msgContext.getWrappedMessage();
            source = message.getContent(Source.class);
            if (source == null) {
                // need to convert
                SOAPMessage msg = message.getContent(SOAPMessage.class);
                XMLStreamReader reader = null;
                if (msg != null) {
                    try {
                        Node node = msg.getSOAPBody().getFirstChild();
                        while (node != null && !(node instanceof Element))  {
                            node = node.getNextSibling();
                        }
                        source = new DOMSource(node);
                        reader = StaxUtils.createXMLStreamReader(source);
                    } catch (SOAPException e) {
                        throw new Fault(e);
                    }
                }

                if (source == null) {
                    try {
                        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
                        reader = message.getContent(XMLStreamReader.class);
                        //content must be an element thing, skip over any whitespace
                        StaxUtils.toNextTag(reader);
                        StaxUtils.copy(reader, writer);
                        source = new DOMSource(writer.getDocument().getDocumentElement());
                        reader = StaxUtils.createXMLStreamReader(writer.getDocument());
                    } catch (ParserConfigurationException e) {
                        throw new Fault(e);
                    } catch (XMLStreamException e) {
                        throw new Fault(e);
                    }
                }
                message.setContent(XMLStreamReader.class, reader);
                message.setContent(Source.class, source);
            } else if (!(source instanceof DOMSource)) {
                W3CDOMStreamWriter writer;
                try {
                    writer = new W3CDOMStreamWriter();
                } catch (ParserConfigurationException e) {
                    throw new Fault(e);
                }
                XMLStreamReader reader = message.getContent(XMLStreamReader.class);
                if (reader == null) {
                    reader = StaxUtils.createXMLStreamReader(source);
                }
                try {
                    StaxUtils.copy(reader, writer);
                } catch (XMLStreamException e) {
                    throw new Fault(e);
                }

                source = new DOMSource(writer.getDocument().getDocumentElement());

                reader = StaxUtils.createXMLStreamReader(writer.getDocument());
                message.setContent(XMLStreamReader.class, reader);
                message.setContent(Source.class, source);
            }
        }

        return source;
    }

    private Source handleDispatchProviderCase(Service.Mode mode) {
        Source source = null;
        Message message = msgContext.getWrappedMessage();
        Source obj = message.getContent(Source.class);
        if (message instanceof SoapMessage) {
            // StreamSource may only be used once, need to make a copy
            if (obj instanceof StreamSource) {
                try {
                    CachedOutputStream cos = new CachedOutputStream();
                    Transformer transformer = XMLUtils.newTransformer();
                    transformer.transform(obj, new StreamResult(cos));

                    obj = new StreamSource(cos.getInputStream());
                    message.setContent(Source.class, new StreamSource(cos.getInputStream()));
                    cos.close();
                } catch (Exception e) {
                    throw new Fault(e);
                }
            }

            if (mode == Service.Mode.PAYLOAD) {
                source = (Source)obj;
            } else {
                try {
                    CachedOutputStream cos = new CachedOutputStream();
                    Transformer transformer = XMLUtils.newTransformer();

                    transformer.transform(obj, new StreamResult(cos));
                    InputStream in = cos.getInputStream();
                    SOAPMessage msg = initSOAPMessage(in);
                    source = new DOMSource(((SOAPMessage)msg).getSOAPBody().getFirstChild());
                    in.close();
                    cos.close();
                } catch (Exception e) {
                    throw new Fault(e);
                }
            }
        } else if (message instanceof XMLMessage) {
            if (obj != null) {
                source = (Source)obj;
            } else if (message.getContent(DataSource.class) != null) {
                throw new Fault(new org.apache.cxf.common.i18n.Message(
                                    "GETPAYLOAD_OF_DATASOURCE_NOT_VALID_XMLHTTPBINDING",
                                    LOG));
            }          
        }
        return source;
    }

    public void setPayload(Source s) {       
        Message message = msgContext.getWrappedMessage();
        Service.Mode mode = (Service.Mode)msgContext.getWrappedMessage()
            .getContextualProperty(Service.Mode.class.getName());
        if (mode != null) {
            if (message instanceof SoapMessage) {
                if (mode == Service.Mode.MESSAGE) {
                    try {
                        // REVISIT: should try to use the original SOAPMessage
                        // instead of creating a new empty one.
                        SOAPMessage msg = initSOAPMessage(null);
                        write(s, msg.getSOAPBody());
                        s = new DOMSource(((SOAPMessage)msg).getSOAPPart());
                    } catch (Exception e) {
                        throw new Fault(e);
                    }
                }
            } else if (message instanceof XMLMessage && message.getContent(DataSource.class) != null) {
                throw new Fault(
                                new org.apache.cxf.common.i18n.Message(
                                    "GETPAYLOAD_OF_DATASOURCE_NOT_VALID_XMLHTTPBINDING",
                                    LOG));
            }
        } else {
            XMLStreamReader reader = StaxUtils.createXMLStreamReader(s);
            msgContext.getWrappedMessage().setContent(XMLStreamReader.class, reader);
        }
        msgContext.getWrappedMessage().setContent(Source.class, s);          
    }

    public Object getPayload(JAXBContext arg0) {
        try {
            return arg0.createUnmarshaller().unmarshal(getPayload());
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }

    public void setPayload(Object arg0, JAXBContext arg1) {
        try {
            W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
            arg1.createMarshaller().marshal(arg0, writer);
            Source source = new DOMSource(writer.getDocument().getDocumentElement());            
            
            setPayload(source);
        } catch (ParserConfigurationException e) {
            throw new WebServiceException(e);
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }
   
    private void write(Source source, Node n) {
        try {
            if (source instanceof DOMSource && ((DOMSource)source).getNode() == null) {
                return;
            }

            XMLStreamWriter writer = new W3CDOMStreamWriter((Element)n);
            StaxUtils.copy(source, writer);
        } catch (XMLStreamException e) {
            throw new Fault(e);
        }
    }   

    private SOAPMessage initSOAPMessage(InputStream is) throws SOAPException, IOException {
        SOAPMessage msg = null;
        if (is != null) {
            msg = MessageFactory.newInstance().createMessage(null, is);
        } else {
            msg = MessageFactory.newInstance().createMessage();
        }
        msg.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
        msg.getSOAPPart().getEnvelope().addNamespaceDeclaration(WSDLConstants.NP_SCHEMA_XSD,
                                                                WSDLConstants.NS_SCHEMA_XSD);
        msg.getSOAPPart().getEnvelope().addNamespaceDeclaration(WSDLConstants.NP_SCHEMA_XSI,
                                                                WSDLConstants.NS_SCHEMA_XSI);

        return msg;
    }  
}
