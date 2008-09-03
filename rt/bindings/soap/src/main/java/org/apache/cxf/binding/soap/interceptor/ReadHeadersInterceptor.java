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

import java.io.InputStream;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.SoapVersionFactory;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.headers.HeaderManager;
import org.apache.cxf.headers.HeaderProcessor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.PartialXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;


public class ReadHeadersInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(ReadHeadersInterceptor.class);

    private Bus bus;
    public ReadHeadersInterceptor(Bus b) {
        super(Phase.READ);
        bus = b;
    }
    public ReadHeadersInterceptor(Bus b, String phase) {
        super(phase);
        bus = b;
    }

    public void handleMessage(SoapMessage message) {
        if (isGET(message)) {
            LOG.info("ReadHeadersInterceptor skipped in HTTP GET method");
            return;
        }
        XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);

        if (xmlReader == null) {
            InputStream in = (InputStream)message.getContent(InputStream.class);
            if (in == null) {
                throw new RuntimeException("Can't find input stream in message");
            }
            xmlReader = StaxUtils.createXMLStreamReader(in);
        }

        try {
            if (xmlReader.nextTag() == XMLStreamConstants.START_ELEMENT) {
                String ns = xmlReader.getNamespaceURI();
                if (ns == null || "".equals(ns)) {
                    throw new SoapFault(new Message("NO_NAMESPACE", LOG, xmlReader.getLocalName()),
                                        Soap11.getInstance().getVersionMismatch());
                }
                
                SoapVersion soapVersion = SoapVersionFactory.getInstance().getSoapVersion(ns);
                if (soapVersion == null) {
                    throw new SoapFault(new Message("INVALID_VERSION", LOG, ns, xmlReader.getLocalName()),
                                            Soap11.getInstance().getVersionMismatch());
                }
                message.setVersion(soapVersion);

                XMLStreamReader filteredReader = new PartialXMLStreamReader(xmlReader, message.getVersion()
                    .getBody());

                Document doc = StaxUtils.read(filteredReader);

                message.setContent(Node.class, doc);

                // Find header
                // TODO - we could stream read the "known" headers and just DOM read the 
                // unknown ones
                Element element = doc.getDocumentElement();
                QName header = soapVersion.getHeader();
                NodeList headerEls = element.getElementsByTagNameNS(header.getNamespaceURI(), header
                    .getLocalPart());
                for (int i = 0; i < headerEls.getLength(); i++) {
                    Node currentHead  = headerEls.item(i);
                    Node node = currentHead;
                    NodeList heads = node.getChildNodes();
                    int len = heads.getLength();
                    for (int x = 0; x < len; x++) {
                        node = (Node)heads.item(x);
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            Element hel = (Element)node;
                            // Need to add any attributes that are present on the parent element
                            // which otherwise would be lost.
                            if (currentHead.hasAttributes()) {
                                NamedNodeMap nnp = currentHead.getAttributes();
                                for (int ct = 0; ct < nnp.getLength(); ct++) {
                                    Node attr = nnp.item(ct);
                                    Node headerAttrNode = hel.hasAttributes() 
                                            ?  hel.getAttributes().getNamedItemNS(
                                                            attr.getNamespaceURI(), attr.getLocalName()) 
                                            : null;
                                    
                                    if (headerAttrNode == null) {
                                        Attr attribute = hel.getOwnerDocument().createAttributeNS(
                                                attr.getNamespaceURI(), 
                                                attr.getNodeName());
                                        attribute.setNodeValue(attr.getNodeValue());
                                        hel.setAttributeNodeNS(attribute);
                                    }
                                }
                            }
                            
//                            System.out.println("READHEADERSINTERCEPTOR : node name : " 
//                            + node.getLocalName() +  " namespace URI" + node.getNamespaceURI());
                            HeaderProcessor p = bus.getExtension(HeaderManager.class)
                                .getHeaderProcessor(hel.getNamespaceURI());

                            Object obj;
                            DataBinding dataBinding = null;
                            if (p == null || p.getDataBinding() == null) {
                                obj = node;
                            } else {
                                obj = p.getDataBinding().createReader(Node.class).read(node);
                            }
                            //TODO - add the interceptors
                            
                            SoapHeader shead = new SoapHeader(new QName(node.getNamespaceURI(),
                                                                        node.getLocalName()),
                                                               obj,
                                                               dataBinding);
                            String mu = hel.getAttributeNS(soapVersion.getNamespace(),
                                                          soapVersion.getAttrNameMustUnderstand());
                            String act = hel.getAttributeNS(soapVersion.getNamespace(),
                                                            soapVersion.getAttrNameRole());

                            if (!StringUtils.isEmpty(act)) {
                                shead.setActor(act);
                            }
                            shead.setMustUnderstand(Boolean.valueOf(mu) || "1".equals(mu));
                            //mark header as inbound header.(for distinguishing between the  direction to 
                            //avoid piggybacking of headers from request->server->response.
                            shead.setDirection(SoapHeader.Direction.DIRECTION_IN);
                            message.getHeaders().add(shead);
                        }                        
                    }
                }
                //advance to just outside the <soap:body> opening tag, but not 
                //to the nextTag as that may skip over white space that is 
                //important to keep for ws-security signature digests and stuff
                int i = xmlReader.next();
                while (i == XMLStreamReader.NAMESPACE
                    || i == XMLStreamReader.ATTRIBUTE) {
                    i = xmlReader.next();
                }
            }
        } catch (XMLStreamException e) {
            throw new SoapFault(new Message("XML_STREAM_EXC", LOG), e, message.getVersion().getSender());
        }
    }
}
