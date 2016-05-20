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
package org.apache.cxf.ws.security.wss4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.xml.namespace.NamespaceContext;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


import org.apache.cxf.binding.soap.saaj.SAAJStreamWriter;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.xml.security.encryption.AbstractSerializer;
import org.apache.xml.security.encryption.XMLEncryptionException;

/**
 * Converts <code>String</code>s into <code>Node</code>s and visa versa using CXF's StaxUtils
 */
public class StaxSerializer extends AbstractSerializer {
    XMLInputFactory factory;
    boolean validFactory;
    
    boolean addNamespaces(XMLStreamReader reader, Node ctx) {
        try {
            NamespaceContext nsctx = reader.getNamespaceContext();
            if (nsctx instanceof com.ctc.wstx.sr.InputElementStack) {
                com.ctc.wstx.sr.InputElementStack ies = (com.ctc.wstx.sr.InputElementStack)nsctx;
                com.ctc.wstx.util.InternCache ic = com.ctc.wstx.util.InternCache.getInstance();
                
                Map<String, String> storedNamespaces = new HashMap<String, String>();
                Node wk = ctx;
                while (wk != null) {
                    NamedNodeMap atts = wk.getAttributes();
                    if (atts != null) {
                        for (int i = 0; i < atts.getLength(); ++i) {
                            Node att = atts.item(i);
                            String nodeName = att.getNodeName();
                            if ((nodeName.equals("xmlns") || nodeName.startsWith("xmlns:"))
                                && !storedNamespaces.containsKey(att.getNodeName())) {
                                
                                String prefix = att.getLocalName();
                                if (prefix.equals("xmlns")) {
                                    prefix = "";
                                }
                                prefix = ic.intern(prefix);
                                ies.addNsBinding(prefix, att.getNodeValue());
                                storedNamespaces.put(nodeName, att.getNodeValue());
                            }
                        }
                    }
                    wk = wk.getParentNode();
                }
            }
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }
    
    private XMLStreamReader createWstxReader(byte[] source, Node ctx) throws XMLEncryptionException {
        try {
            if (factory == null) {
                factory = StaxUtils.createXMLInputFactory(true);
                try {
                    factory.setProperty("com.ctc.wstx.fragmentMode",
                                        com.ctc.wstx.api.WstxInputProperties.PARSING_MODE_FRAGMENT);
                    factory.setProperty(org.codehaus.stax2.XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE, Boolean.TRUE);
                    validFactory = true;
                } catch (Throwable t) {
                    //ignore
                    validFactory = false;
                }
            }
            if (validFactory) {
                XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(source));
                if (addNamespaces(reader, ctx)) {
                    return reader;
                }
            }
        } catch (Throwable e) {
            //ignore
        }
        return null;
    }
    /**
     * @param source
     * @param ctx
     * @return the Node resulting from the parse of the source
     * @throws XMLEncryptionException
     */
    public Node deserialize(byte[] source, Node ctx) throws XMLEncryptionException {
        XMLStreamReader reader = createWstxReader(source, ctx);
        if (reader != null) {
            return deserialize(ctx, reader, false);            
        }
        return deserialize(ctx, new InputSource(createStreamContext(source, ctx)));
    }
    
    InputStream createStreamContext(byte[] source, Node ctx) throws XMLEncryptionException {
        Vector<InputStream> v = new Vector<>(2);

        LoadingByteArrayOutputStream byteArrayOutputStream = new LoadingByteArrayOutputStream();
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(byteArrayOutputStream, "UTF-8");
            outputStreamWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><dummy");

            // Run through each node up to the document node and find any xmlns: nodes
            Map<String, String> storedNamespaces = new HashMap<String, String>();
            Node wk = ctx;
            while (wk != null) {
                NamedNodeMap atts = wk.getAttributes();
                if (atts != null) {
                    for (int i = 0; i < atts.getLength(); ++i) {
                        Node att = atts.item(i);
                        String nodeName = att.getNodeName();
                        if ((nodeName.equals("xmlns") || nodeName.startsWith("xmlns:"))
                                && !storedNamespaces.containsKey(att.getNodeName())) {
                            outputStreamWriter.write(" ");
                            outputStreamWriter.write(nodeName);
                            outputStreamWriter.write("=\"");
                            outputStreamWriter.write(att.getNodeValue());
                            outputStreamWriter.write("\"");
                            storedNamespaces.put(nodeName, att.getNodeValue());
                        }
                    }
                }
                wk = wk.getParentNode();
            }
            outputStreamWriter.write(">");
            outputStreamWriter.close();
            v.add(byteArrayOutputStream.createInputStream());
            v.addElement(new ByteArrayInputStream(source));
            byteArrayOutputStream = new LoadingByteArrayOutputStream();
            outputStreamWriter = new OutputStreamWriter(byteArrayOutputStream, "UTF-8");
            outputStreamWriter.write("</dummy>");
            outputStreamWriter.close();
            v.add(byteArrayOutputStream.createInputStream());
        } catch (UnsupportedEncodingException e) {
            throw new XMLEncryptionException(e);
        } catch (IOException e) {
            throw new XMLEncryptionException(e);
        }
        return new SequenceInputStream(v.elements());
    }

    /**
     * @param source
     * @param ctx
     * @return the Node resulting from the parse of the source
     * @throws XMLEncryptionException
     */
    public Node deserialize(String source, Node ctx) throws XMLEncryptionException {
        String fragment = createContext(source, ctx);
        return deserialize(ctx, new InputSource(new StringReader(fragment)));
    }
    
    @Override
    public byte[] serializeToByteArray(Element element) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(baos);
            StaxUtils.copy(element, writer);
            writer.close();
            return baos.toByteArray();
        }
    }
    
    @Override
    public byte[] serializeToByteArray(NodeList content) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(baos);
            for (int i = 0; i < content.getLength(); i++) {
                StaxUtils.copy(new DOMSource(content.item(i)), writer);
            }
            writer.close();
            return baos.toByteArray();
        }
    }

    /**
     * @param ctx
     * @param inputSource
     * @return the Node resulting from the parse of the source
     * @throws XMLEncryptionException
     */
    private Node deserialize(Node ctx, InputSource inputSource) throws XMLEncryptionException {
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(inputSource);
        return deserialize(ctx, reader, true);
    }
    private Node deserialize(Node ctx, XMLStreamReader reader, boolean wrapped) throws XMLEncryptionException {
        Document contextDocument = null;
        if (Node.DOCUMENT_NODE == ctx.getNodeType()) {
            contextDocument = (Document)ctx;
        } else {
            contextDocument = ctx.getOwnerDocument();
        }

        XMLStreamWriter writer = null;
        try {
            if (ctx instanceof SOAPElement) {
                SOAPElement el = (SOAPElement)ctx;
                while (el != null && !(el instanceof SOAPEnvelope)) {
                    el = el.getParentElement();
                }
                //cannot load into fragment due to a ClassCastException iwthin SAAJ addChildElement 
                //which only checks for Document as parent, not DocumentFragment
                Element element = ctx.getOwnerDocument().createElementNS("dummy", "dummy");
                writer = new SAAJStreamWriter((SOAPEnvelope)el, element);
                StaxUtils.copy(reader, writer);
                
                DocumentFragment result = contextDocument.createDocumentFragment();
                Node child = element.getFirstChild();
                if (wrapped) {
                    child = child.getFirstChild();
                }
                if (child != null && child.getNextSibling() == null) {
                    return child;
                }
                while (child != null) {
                    Node nextChild = child.getNextSibling();
                    result.appendChild(child);
                    child = nextChild;
                }
                
                return result;
            }
            // Import to a dummy fragment
            DocumentFragment dummyFragment = contextDocument.createDocumentFragment();
            writer = StaxUtils.createXMLStreamWriter(new DOMResult(dummyFragment));
            StaxUtils.copy(reader, writer);
            
            // Remove the "dummy" wrapper
            
            if (wrapped) {
                DocumentFragment result = contextDocument.createDocumentFragment();
                Node child = dummyFragment.getFirstChild().getFirstChild();
                if (child != null && child.getNextSibling() == null) {
                    return child;
                }
                while (child != null) {
                    Node nextChild = child.getNextSibling();
                    result.appendChild(child);
                    child = nextChild;
                }
                dummyFragment = result;
            }
            return dummyFragment;
        } catch (XMLStreamException ex) {
            throw new XMLEncryptionException(ex);
        }
    }

}
