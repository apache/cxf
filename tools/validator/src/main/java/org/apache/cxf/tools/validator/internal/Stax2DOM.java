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

package org.apache.cxf.tools.validator.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.util.URIParserUtil;

public class Stax2DOM {
    static final String XML_NS = "http://www.w3.org/2000/xmlns/";

    private  Element currentElement;
    private  Document doc;
    private XMLInputFactory factory;
    private  XMLEventReader reader;

    public Stax2DOM() {
    }

    private void init() {
        try {
            factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ToolException(e);
        }
    }

    public Document getDocument(String wsdl) throws ToolException {
        try {
            URI wsdlURI = new URI(URIParserUtil.getAbsoluteURI(wsdl));
            if (wsdlURI.toString().startsWith("http")) {
                return getDocument(wsdlURI.toURL());
            }
            return getDocument(wsdlURI.toURL());
        } catch (Exception e) {
            throw new ToolException(e);
        }
    }

    public Document getDocument(URL url) throws ToolException {
        if (reader == null) {
            init();
            try {
                reader = factory.createXMLEventReader(url.openStream());
            } catch (FileNotFoundException fe) {
                throw new ToolException("Cannot get the wsdl " + url, fe);
            } catch (XMLStreamException e) {
                throw new ToolException(e);
            } catch (IOException ioe) {
                throw new ToolException(ioe);
            }
        }
        return getDocument(reader, url.toString());
    }

    public Document getDocument(File wsdl) throws ToolException {
        if (reader == null) {
            init();
            try {
                reader = factory.createXMLEventReader(new FileReader(wsdl));
            } catch (FileNotFoundException fe) {
                throw new ToolException("Cannot get the wsdl " + wsdl, fe);
            } catch (XMLStreamException e) {
                throw new ToolException(e);
            }

        }
        return getDocument(reader, wsdl.toString());
    }

    public Document getDocument(XMLEventReader xmlEventReader) throws ToolException {
        return getDocument(xmlEventReader, null);
    }

    public Document getDocument(XMLEventReader xmlEventReader, String wsdlurl) throws ToolException {
        try {
            doc = XMLUtils.newDocument();
        } catch (ParserConfigurationException e) {
            throw new ToolException(e);
        }
        doc.setDocumentURI(wsdlurl);

        currentElement = doc.getDocumentElement();

        while (xmlEventReader.hasNext()) {
            XMLEvent xmleve = (XMLEvent)xmlEventReader.next();

            if (xmleve.getEventType() == XMLStreamConstants.END_ELEMENT) {
                endElement();
            }

            if (xmleve.getEventType() == XMLStreamConstants.START_ELEMENT) {
                startElement((StartElement)xmleve);
            }
        }
        return doc;
    }

    public void startElement(StartElement ele) {
        Element element = null;
        if (!StringUtils.isEmpty(ele.getName().getPrefix())) {
            element = doc.createElementNS(ele.getName().getNamespaceURI(),
                                          ele.getName().getPrefix() + ":"
                                          + ele.getName().getLocalPart());
        } else {
            element = doc.createElementNS(ele.getName().getNamespaceURI(), 
                                          ele.getName().getLocalPart());
        }

        Iterator ite = ele.getNamespaces();
        while (ite.hasNext()) {
            Namespace ns = (Namespace) ite.next();
            String pfx = ns.getPrefix();
            String uri = ns.getNamespaceURI();
            if (!StringUtils.isEmpty(pfx)) {
                Attr attr = element.getOwnerDocument().createAttributeNS(XML_NS,
                                                                        "xmlns:" + pfx);
                attr.setValue(uri);
                element.setAttributeNodeNS(attr);
            } else {
                Attr attr = element.getOwnerDocument().createAttributeNS(XML_NS,
                                                                         "xmlns");
                attr.setValue(uri);
                element.setAttributeNodeNS(attr);
            }
        }
        ite = ele.getAttributes();
        while (ite.hasNext()) {
            Attribute attr = (Attribute)ite.next();
            element.setAttribute(attr.getName().getLocalPart(), attr.getValue());
        }


        if (currentElement == null) {
            doc.appendChild(element);
        } else {
            currentElement.appendChild(element);
        }

        currentElement = element;
        element.setUserData(WSDLConstants.NODE_LOCATION, ele.getLocation(), null);

    }

    public void endElement() {
        Node node = currentElement.getParentNode();
        if (node instanceof Document) {
            currentElement = ((Document)node).getDocumentElement();
        } else {
            currentElement = (Element)currentElement.getParentNode();
        }
    }

}
