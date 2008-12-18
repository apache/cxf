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

package org.apache.cxf.jaxws.handler.soap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.SoapVersionFactory;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxws.handler.AbstractProtocolHandlerInterceptor;
import org.apache.cxf.jaxws.handler.HandlerChainInvoker;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManagerImpl;
import org.apache.cxf.staxutils.PartialXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createNiceControl;

public class SOAPHandlerInterceptorTest extends Assert {

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    // SAAJ tree is created from DOMXMLStreamWriter. Any changes to SOAPMessage should be streamed back to
    // outputStream
    @Test
    public void testChangeSOAPBodyOutBound() throws Exception {
        List<Handler> list = new ArrayList<Handler>();
        list.add(new SOAPHandler<SOAPMessageContext>() {
            public boolean handleMessage(SOAPMessageContext smc) {
                Boolean outboundProperty = (Boolean)smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
                if (outboundProperty.booleanValue()) {
                    try {
                        smc.setMessage(prepareSOAPMessage("resources/greetMeRpcLitRespChanged.xml"));
                    } catch (Exception e) {
                        throw new Fault(e);
                    }
                }
                return true;
            }

            public boolean handleFault(SOAPMessageContext smc) {
                return true;
            }

            public Set<QName> getHeaders() {
                return null;
            }

            public void close(MessageContext messageContext) {
            }
        });
        HandlerChainInvoker invoker = new HandlerChainInvoker(list);

        IMocksControl control = createNiceControl();
        Binding binding = control.createMock(Binding.class);
        Exchange exchange = control.createMock(Exchange.class);
        expect(exchange.get(HandlerChainInvoker.class)).andReturn(invoker).anyTimes();
        SoapMessage message = new SoapMessage(new MessageImpl());
        message.setExchange(exchange);
        // This is to set direction to outbound
        expect(exchange.getOutMessage()).andReturn(message).anyTimes();
        CachedStream originalEmptyOs = new CachedStream();
        
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(originalEmptyOs);
        message.setContent(XMLStreamWriter.class, writer);

        InterceptorChain chain = new PhaseInterceptorChain((new PhaseManagerImpl()).getOutPhases());
        //Interceptors after SOAPHandlerInterceptor DOMXMLStreamWriter to write
        chain.add(new AbstractProtocolHandlerInterceptor<SoapMessage>(binding, Phase.MARSHAL) {

            public void handleMessage(SoapMessage message) throws Fault {
                try {
                    XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
                    SoapVersion soapVersion = Soap11.getInstance();
                    writer.setPrefix(soapVersion.getPrefix(), soapVersion.getNamespace());
                    writer.writeStartElement(soapVersion.getPrefix(), 
                                          soapVersion.getEnvelope().getLocalPart(),
                                          soapVersion.getNamespace());
                    writer.writeNamespace(soapVersion.getPrefix(), soapVersion.getNamespace());
                    writer.writeEndElement();
                    
                    writer.flush();
                } catch (Exception e) {
                    // do nothing
                }
            }

        });
        
        chain.add(new SOAPHandlerInterceptor(binding));
        message.setInterceptorChain(chain);
        control.replay();

        chain.doIntercept(message);
        
        control.verify();

        writer.flush();
        
        // Verify SOAPMessage
        SOAPMessage resultedMessage = message.getContent(SOAPMessage.class);
        assertNotNull(resultedMessage);
        SOAPBody bodyNew = resultedMessage.getSOAPBody();
        Iterator itNew = bodyNew.getChildElements(new QName("http://apache.org/hello_world_rpclit",
                                                            "sendReceiveDataResponse"));
        SOAPBodyElement bodyElementNew = (SOAPBodyElement)itNew.next();
        Iterator outIt = bodyElementNew
            .getChildElements(new QName("http://apache.org/hello_world_rpclit/types", "out"));
        Element outElement = (SOAPElement)outIt.next();
        assertNotNull(outElement);        
        Element elem3Element = 
            DOMUtils.findAllElementsByTagNameNS(outElement, 
                                                "http://apache.org/hello_world_rpclit/types", 
                                                "elem3").get(0);
        assertEquals("100", elem3Element.getTextContent());
    }

    @Test
    public void testChangeSOAPHeaderInBound() throws Exception {
        List<Handler> list = new ArrayList<Handler>();
        list.add(new SOAPHandler<SOAPMessageContext>() {
            public boolean handleMessage(SOAPMessageContext smc) {
                try {
                    Boolean outboundProperty = (Boolean)smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
                    if (!outboundProperty.booleanValue()) {
                        // change mustUnderstand to false
                        SOAPMessage message = smc.getMessage();
                        SOAPHeader soapHeader = message.getSOAPHeader();
                        Element headerElementNew = (Element)soapHeader.getFirstChild();

                        SoapVersion soapVersion = Soap11.getInstance();                        
                        Attr attr = 
                            headerElementNew.getOwnerDocument().createAttributeNS(soapVersion.getNamespace(), 
                                                                                  "SOAP-ENV:mustUnderstand"); 
                        attr.setValue("false");
                        headerElementNew.setAttributeNodeNS(attr);          
                    }
                } catch (Exception e) {
                    throw new Fault(e);
                }
                return true;
            }

            public boolean handleFault(SOAPMessageContext smc) {
                return true;
            }

            public Set<QName> getHeaders() {
                return null;
            }

            public void close(MessageContext messageContext) {
            }
        });
        HandlerChainInvoker invoker = new HandlerChainInvoker(list);

        IMocksControl control = createNiceControl();
        Binding binding = control.createMock(Binding.class);
        Exchange exchange = control.createMock(Exchange.class);
        expect(exchange.get(HandlerChainInvoker.class)).andReturn(invoker).anyTimes();
        // This is to set direction to inbound
        expect(exchange.getOutMessage()).andReturn(null);
        
        SoapMessage message = new SoapMessage(new MessageImpl());
        message.setExchange(exchange);
        XMLStreamReader reader = preparemXMLStreamReader("resources/greetMeRpcLitReq.xml");
        message.setContent(XMLStreamReader.class, reader);
        Object[] headerInfo = prepareSOAPHeader();
        
        message.setContent(Node.class, headerInfo[0]);
        
        Node node = ((Element) headerInfo[1]).getFirstChild();
        
        message.getHeaders().add(new Header(new QName(node.getNamespaceURI(), node.getLocalName()), node));
        
        control.replay();

        SOAPHandlerInterceptor li = new SOAPHandlerInterceptor(binding);
        li.handleMessage(message);
        control.verify();

        // Verify SOAPMessage header
        SOAPMessage soapMessageNew = message.getContent(SOAPMessage.class);       

        Element headerElementNew = DOMUtils.getFirstElement(soapMessageNew.getSOAPHeader());
        
        SoapVersion soapVersion = Soap11.getInstance();
        assertEquals("false", headerElementNew.getAttributeNS(soapVersion.getNamespace(), "mustUnderstand"));

        // Verify XMLStreamReader
        XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
        QName qn = xmlReader.getName();
        assertEquals("sendReceiveData", qn.getLocalPart());
        
        // Verify Header Element
        Iterator<Header> iter = message.getHeaders().iterator();
        Element requiredHeader = null;
        while (iter.hasNext()) {
            Header localHdr = iter.next();
            if (localHdr.getObject() instanceof Element) {
                Element elem = (Element) localHdr.getObject();
                if (elem.getNamespaceURI().equals("http://apache.org/hello_world_rpclit/types")
                        && elem.getLocalName().equals("header1")) {
                    requiredHeader = (Element) localHdr.getObject();
                    break;                
                }
            }
        }
        
        assertNotNull("Should have found header1", requiredHeader);
        assertEquals("false", requiredHeader.getAttributeNS(soapVersion.getNamespace(), "mustUnderstand"));
    }

    @Test
    public void testChangeSOAPHeaderOutBound() throws Exception {
        List<Handler> list = new ArrayList<Handler>();
        list.add(new SOAPHandler<SOAPMessageContext>() {
            public boolean handleMessage(SOAPMessageContext smc) {
                try {
                    Boolean outboundProperty = (Boolean)smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
                    if (outboundProperty.booleanValue()) {
                        // change mustUnderstand to false
                        SOAPMessage message = smc.getMessage();
                         
                        SOAPHeader soapHeader = message.getSOAPHeader();
                        Iterator it = soapHeader.getChildElements(new QName(
                            "http://apache.org/hello_world_rpclit/types", "header1"));
                        SOAPHeaderElement headerElementNew = (SOAPHeaderElement)it.next();

                        SoapVersion soapVersion = Soap11.getInstance();
                        Attr attr = 
                            headerElementNew.getOwnerDocument().createAttributeNS(soapVersion.getNamespace(), 
                                                                                  "SOAP-ENV:mustUnderstand"); 
                        attr.setValue("false");
                        headerElementNew.setAttributeNodeNS(attr);
                    }
                } catch (Exception e) {
                    throw new Fault(e);
                }
                return true;
            }

            public boolean handleFault(SOAPMessageContext smc) {
                return true;
            }

            public Set<QName> getHeaders() {
                return null;
            }

            public void close(MessageContext messageContext) {
            }
        });
        HandlerChainInvoker invoker = new HandlerChainInvoker(list);

        IMocksControl control = createNiceControl();
        Binding binding = control.createMock(Binding.class);
        Exchange exchange = control.createMock(Exchange.class);
        expect(exchange.get(HandlerChainInvoker.class)).andReturn(invoker).anyTimes();
        SoapMessage message = new SoapMessage(new MessageImpl());
        message.setExchange(exchange);
        // This is to set direction to outbound
        expect(exchange.getOutMessage()).andReturn(message).anyTimes();
        CachedStream originalEmptyOs = new CachedStream();
        message.setContent(OutputStream.class, originalEmptyOs);

        InterceptorChain chain = new PhaseInterceptorChain((new PhaseManagerImpl()).getOutPhases());
        //Interceptors after SOAPHandlerInterceptor DOMXMLStreamWriter to write
        chain.add(new AbstractProtocolHandlerInterceptor<SoapMessage>(binding, Phase.MARSHAL) {

            public void handleMessage(SoapMessage message) throws Fault {
                try {
                    XMLStreamWriter writer = message.getContent(XMLStreamWriter.class); 
                    SoapVersion soapVersion = Soap11.getInstance();
                    writer.setPrefix(soapVersion.getPrefix(), soapVersion.getNamespace());
                    writer.writeStartElement(soapVersion.getPrefix(), 
                                          soapVersion.getEnvelope().getLocalPart(),
                                          soapVersion.getNamespace());
                    writer.writeNamespace(soapVersion.getPrefix(), soapVersion.getNamespace());
                    
                    Object[] headerInfo = prepareSOAPHeader();
                    StaxUtils.writeElement((Element) headerInfo[1], writer, true, false);
                    
                    writer.writeEndElement();
                    
                    writer.flush();
                } catch (Exception e) {
                    // do nothing
                }
            }

        });
        chain.add(new SOAPHandlerInterceptor(binding));
        message.setInterceptorChain(chain);
        control.replay();

        chain.doIntercept(message);
        
        control.verify();

        // Verify SOAPMessage header
        SOAPMessage soapMessageNew = message.getContent(SOAPMessage.class);

        SOAPHeader soapHeader = soapMessageNew.getSOAPHeader();
        Iterator itNew = soapHeader.getChildElements(new QName("http://apache.org/hello_world_rpclit/types",
            "header1"));
        SOAPHeaderElement headerElementNew = (SOAPHeaderElement)itNew.next();
        SoapVersion soapVersion = Soap11.getInstance();
        assertEquals("false", headerElementNew.getAttributeNS(soapVersion.getNamespace(), "mustUnderstand"));
        originalEmptyOs.close();
    }

    @Test
    public void testGetSOAPMessageInBound() throws Exception {
        List<Handler> list = new ArrayList<Handler>();
        list.add(new SOAPHandler<SOAPMessageContext>() {
            public boolean handleMessage(SOAPMessageContext smc) {
                try {
                    smc.getMessage();
                } catch (Exception e) {
                    throw new Fault(e);
                }
                return true;
            }

            public boolean handleFault(SOAPMessageContext smc) {
                return true;
            }

            public Set<QName> getHeaders() {
                return null;
            }

            public void close(MessageContext messageContext) {
            }
        });
        HandlerChainInvoker invoker = new HandlerChainInvoker(list);

        IMocksControl control = createNiceControl();
        Binding binding = control.createMock(Binding.class);
        Exchange exchange = control.createMock(Exchange.class);
        expect(exchange.get(HandlerChainInvoker.class)).andReturn(invoker).anyTimes();
        // This is to set direction to inbound
        expect(exchange.getOutMessage()).andReturn(null);

        SoapMessage message = new SoapMessage(new MessageImpl());
        message.setExchange(exchange);

        XMLStreamReader reader = preparemXMLStreamReader("resources/greetMeRpcLitReq.xml");
        message.setContent(XMLStreamReader.class, reader);
        control.replay();

        SOAPHandlerInterceptor li = new SOAPHandlerInterceptor(binding);
        li.handleMessage(message);
        control.verify();

        // Verify SOAPMessage
        SOAPMessage soapMessageNew = message.getContent(SOAPMessage.class);
        SOAPBody bodyNew = soapMessageNew.getSOAPBody();
        Iterator itNew = bodyNew.getChildElements();
        SOAPBodyElement bodyElementNew = (SOAPBodyElement)itNew.next();
        assertEquals("sendReceiveData", bodyElementNew.getLocalName());

        // Verify the XMLStreamReader
        XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
        QName qn = xmlReader.getName();
        assertEquals("sendReceiveData", qn.getLocalPart());
    }

    @Test
    public void testGetUnderstoodHeadersReturnsNull() {
        List<Handler> list = new ArrayList<Handler>();
        list.add(new SOAPHandler<SOAPMessageContext>() {
            public boolean handleMessage(SOAPMessageContext smc) {
                return true;
            }

            public boolean handleFault(SOAPMessageContext smc) {
                return true;
            }

            public Set<QName> getHeaders() {
                return null;
            }

            public void close(MessageContext messageContext) {
            }
        });
        HandlerChainInvoker invoker = new HandlerChainInvoker(list);

        IMocksControl control = createNiceControl();
        Binding binding = control.createMock(Binding.class);
        SoapMessage message = control.createMock(SoapMessage.class);
        Exchange exchange = control.createMock(Exchange.class);
        expect(binding.getHandlerChain()).andReturn(list);
        expect(message.getExchange()).andReturn(exchange).anyTimes();
        expect(message.keySet()).andReturn(new HashSet<String>());
        expect(exchange.get(HandlerChainInvoker.class)).andReturn(invoker);
        control.replay();

        SOAPHandlerInterceptor li = new SOAPHandlerInterceptor(binding);
        Set<QName> understood = li.getUnderstoodHeaders();
        assertNotNull(understood);
        assertTrue(understood.isEmpty());
    }

    private XMLStreamReader preparemXMLStreamReader(String resouceName) throws Exception {
        InputStream is = this.getClass().getResourceAsStream(resouceName);
        XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(is);

        // skip until soap body
        if (xmlReader.nextTag() == XMLStreamConstants.START_ELEMENT) {
            String ns = xmlReader.getNamespaceURI();
            SoapVersion soapVersion = SoapVersionFactory.getInstance().getSoapVersion(ns);
            // message.setVersion(soapVersion);

            QName qn = xmlReader.getName();
            while (!qn.equals(soapVersion.getBody()) && !qn.equals(soapVersion.getHeader())) {
                while (xmlReader.nextTag() != XMLStreamConstants.START_ELEMENT) {
                    // nothing to do
                }
                qn = xmlReader.getName();
            }
            if (qn.equals(soapVersion.getHeader())) {
                XMLStreamReader filteredReader = new PartialXMLStreamReader(xmlReader, soapVersion.getBody());

                StaxUtils.read(filteredReader);
            }
            // advance just past body.
            xmlReader.next();

            while (xmlReader.isWhiteSpace()) {
                xmlReader.next();
            }
        }
        return xmlReader;
    }

    private Object[] prepareSOAPHeader() throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        SoapVersion soapVersion = Soap11.getInstance();
        Element envElement = doc.createElementNS(soapVersion.getEnvelope().getNamespaceURI(),
                                                 soapVersion.getEnvelope().getLocalPart());
        
        Element headerElement = doc.createElementNS(soapVersion.getNamespace(), 
                                                    soapVersion.getHeader().getLocalPart());
        
        Element bodyElement = doc.createElementNS(soapVersion.getBody().getNamespaceURI(),
                                                  soapVersion.getBody().getLocalPart());
        
        Element childElement = doc.createElementNS("http://apache.org/hello_world_rpclit/types",
                                                   "ns2:header1");
        Attr attr = 
            childElement.getOwnerDocument().createAttributeNS(soapVersion.getNamespace(), 
                                                                  "SOAP-ENV:mustUnderstand"); 
        attr.setValue("true");
        childElement.setAttributeNodeNS(attr);  
        
        headerElement.appendChild(childElement);
        envElement.appendChild(headerElement);
        envElement.appendChild(bodyElement);
        doc.appendChild(envElement);
        
        return new Object[] {doc, headerElement};
    }

    private SOAPMessage prepareSOAPMessage(String resouceName) throws Exception {
        InputStream is = this.getClass().getResourceAsStream(resouceName);
        SOAPMessage soapMessage = null;
        MessageFactory factory = MessageFactory.newInstance();
        MimeHeaders mhs = null;
        soapMessage = factory.createMessage(mhs, is);
        return soapMessage;
    }


    private class CachedStream extends CachedOutputStream {
        protected void doFlush() throws IOException {
            currentStream.flush();
        }

        protected void doClose() throws IOException {
        }

        protected void onWrite() throws IOException {
        }
    }

}
