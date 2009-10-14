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

package org.apache.cxf.staxutils;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import org.apache.cxf.common.util.StringUtils;

/**
 * 
 */
public class StreamWriterContentHandler implements ContentHandler {
    
    XMLStreamWriter writer;
    Map<String, String> mapping = new LinkedHashMap<String, String>();
    
    public StreamWriterContentHandler(XMLStreamWriter w) {
        writer = w;
    }

    /**
     * Method endDocument.
     *
     * @throws SAXException
     */
    public void endDocument() throws SAXException {
        // do nothing
    }

    /**
     * Method startDocument.
     *
     * @throws SAXException
     */
    public void startDocument() throws SAXException {
        // 
    }

    /**
     * Method characters.
     *
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     */
    public void characters(char ch[], int start, int length) throws SAXException {
        try {
            writer.writeCharacters(ch, start, length);
        } catch (XMLStreamException e) {
            throw new SAXException(e);
        }
    }

    /**
     * Method ignorableWhitespace.
     *
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     */
    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
    }

    /**
     * Method endPrefixMapping.
     *
     * @param prefix
     * @throws SAXException
     */
    public void endPrefixMapping(String prefix) throws SAXException {
    }

    /**
     * Method skippedEntity.
     *
     * @param name
     * @throws SAXException
     */
    public void skippedEntity(String name) throws SAXException {
    }

    /**
     * Method setDocumentLocator.
     *
     * @param locator
     */
    public void setDocumentLocator(Locator locator) {
    }

    /**
     * Method processingInstruction.
     *
     * @param target
     * @param data
     * @throws SAXException
     */
    public void processingInstruction(String target, String data)
        throws SAXException {
    }

    /**
     * Method startPrefixMapping.
     *
     * @param prefix
     * @param uri
     * @throws SAXException
     */
    public void startPrefixMapping(String prefix, String uri)
        throws SAXException {
        mapping.put(prefix, uri);
        try {
            writer.setPrefix(prefix, uri);
        } catch (XMLStreamException e) {
            throw new SAXException(e);
        }
    }

    /**
     * Method endElement.
     *
     * @param namespaceURI
     * @param localName
     * @param qName
     * @throws SAXException
     */
    public void endElement(String namespaceURI,
                           String localName,
                           String qName) throws SAXException {
        try {
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new SAXException(e);
        }
    }

    /**
     * Method getPrefix.
     *
     * @param qname
     * @return Returns String.
     */
    private String getPrefix(String ns) {
        int idx = ns.indexOf(':');
        if (idx != -1) {
            return ns.substring(0, idx);
        }
        return null;
    }

    /**
     * Method startElement.
     *
     * @param namespaceURI
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes atts) throws SAXException {
        try {
            String prefix = getPrefix(qName);
            
            // it is only the prefix we want to learn from the QName! so we can get rid of the
            // spliting QName
            if (prefix == null) {
                writer.writeStartElement(namespaceURI, localName);
            } else {
                writer.writeStartElement(prefix, localName, namespaceURI);
            }
            for (Map.Entry<String, String> e : mapping.entrySet()) {
                if ("".equals(e.getKey())) {
                    writer.writeDefaultNamespace(e.getValue());
                } else {
                    writer.writeNamespace(e.getKey(), e.getValue());
                }
            }
            mapping.clear();
            if (atts != null) {
                int attCount = atts.getLength();
                for (int i = 0; i < attCount; i++) {
                    if (StringUtils.isEmpty(atts.getURI(i))) {
                        String s = atts.getLocalName(i);
                        if (StringUtils.isEmpty(s)) {
                            s = atts.getQName(i);
                        }
                        writer.writeAttribute(s,
                                              atts.getValue(i));
                    } else {
                        String pfx = atts.getQName(i);
                        if (pfx.indexOf(':') != -1) {
                            pfx = pfx.substring(0, pfx.indexOf(':'));
                            writer.writeAttribute(pfx,
                                                  atts.getURI(i),
                                                  atts.getLocalName(i),
                                                  atts.getValue(i));
                        } else {
                            writer.writeAttribute(atts.getURI(i),
                                              atts.getLocalName(i),
                                              atts.getValue(i));
                        }
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new SAXException(e);
        }
    }
    
    

}
