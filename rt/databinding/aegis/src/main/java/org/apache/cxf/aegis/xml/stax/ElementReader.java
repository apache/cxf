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
package org.apache.cxf.aegis.xml.stax;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.xml.AbstractMessageReader;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * Reads literal encoded messages.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class ElementReader extends AbstractMessageReader implements MessageReader {
    private static final Pattern QNAME_PATTERN = Pattern.compile("([^:]+):([^:]+)");

    private DepthXMLStreamReader root;

    private String value;

    private String localName;

    private QName name;

    private QName xsiType;

    private boolean hasCheckedChildren;

    private boolean hasChildren;

    private String namespace;

    private int depth;

    private int currentAttribute;

    /**
     * @param root
     */
    public ElementReader(DepthXMLStreamReader root) {
        this.root = root;
        this.localName = root.getLocalName();
        this.name = root.getName();
        this.namespace = root.getNamespaceURI();

        extractXsiType();

        depth = root.getDepth();
    }

    public ElementReader(XMLStreamReader reader) {
        this(reader instanceof DepthXMLStreamReader ? (DepthXMLStreamReader)reader
            : new DepthXMLStreamReader(reader));
    }

    /**
     * @param is
     * @throws XMLStreamException
     */
    public ElementReader(InputStream is) throws XMLStreamException {
        // XMLInputFactory factory = XMLInputFactory.newInstance();
        // XMLStreamReader xmlReader = factory.createXMLStreamReader(is);
        XMLStreamReader xmlReader = StaxUtils.createXMLStreamReader(is, null);

        xmlReader.nextTag();

        this.root = new DepthXMLStreamReader(xmlReader);
        this.localName = root.getLocalName();
        this.name = root.getName();
        this.namespace = root.getNamespaceURI();

        extractXsiType();

        depth = root.getDepth();
    }

    private void extractXsiType() {
        /*
         * We're making a conscious choice here -- garbage in == garbage out.
         */
        String xsiTypeQname = root.getAttributeValue(SOAPConstants.XSI_NS, "type");
        if (xsiTypeQname != null) {
            Matcher m = QNAME_PATTERN.matcher(xsiTypeQname);
            if (m.matches()) {
                NamespaceContext nc = root.getNamespaceContext();
                this.xsiType = new QName(nc.getNamespaceURI(m.group(1)), m.group(2), m.group(1));
            } else {
                this.xsiType = new QName(this.namespace, xsiTypeQname, "");
            }
        }
    }

    /**
     * @see org.codehaus.xfire.aegis.MessageReader#getValue()
     */
    public String getValue() {
        if (value == null) {
            try {
                if (isXsiNil()) {
                    readToEnd();
                    return null;
                }
                value = root.getElementText();
                
                hasCheckedChildren = true;
                hasChildren = false;
                if (root.hasNext()) {
                    root.next();
                }
            } catch (XMLStreamException e) {
                throw new DatabindingException("Could not read XML stream.", e);
            }
            
            if (value == null) {
                value = "";
            }
        }
        return value;
    }

    public String getValue(String ns, String attr) {
        return root.getAttributeValue(ns, attr);
    }

    public boolean hasMoreElementReaders() {
        // Check to see if we checked before,
        // so we don't mess up the stream position.
        if (!hasCheckedChildren) {
            checkHasMoreChildReaders();
        }

        return hasChildren;
    }

    private boolean checkHasMoreChildReaders() {
        try {
            int event = root.getEventType();
            while (root.hasNext()) {
                switch (event) {
                case XMLStreamReader.START_ELEMENT:
                    if (root.getDepth() > depth) {
                        hasCheckedChildren = true;
                        hasChildren = true;

                        return true;
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if (root.getDepth() < depth) {
                        hasCheckedChildren = true;
                        hasChildren = false;

                        if (root.hasNext()) {
                            root.next();
                        }
                        return false;
                    }
                    break;
                case XMLStreamReader.END_DOCUMENT:
                    // We should never get here...
                    hasCheckedChildren = true;
                    hasChildren = false;
                    return false;
                default:
                    break;
                }

                if (root.hasNext()) {
                    event = root.next();
                }
            }

            hasCheckedChildren = true;
            hasChildren = false;
            return false;
        } catch (XMLStreamException e) {
            throw new DatabindingException("Error parsing document.", e);
        }
    }

    public MessageReader getNextElementReader() {
        if (!hasCheckedChildren) {
            checkHasMoreChildReaders();
        }

        if (!hasChildren) {
            return null;
        }

        hasCheckedChildren = false;

        return new ElementReader(root);
    }

    public QName getName() {
        return name;
    }

    public String getLocalName() {
        return localName;
    }

    public String getNamespace() {
        return namespace;
    }

    public QName getXsiType() {
        return xsiType;
    }

    public XMLStreamReader getXMLStreamReader() {
        return root;
    }

    public boolean hasMoreAttributeReaders() {
        if (!root.isStartElement()) {
            return false;
        }

        return currentAttribute < root.getAttributeCount();
    }

    public MessageReader getAttributeReader(QName qName) {
        String attribute = root.getAttributeValue(qName.getNamespaceURI(), qName.getLocalPart());
        if (attribute == null && "".equals(qName.getNamespaceURI())) {
            // The qName namespaceURI of the attribute seems to be null
            // rather than "" when using the ibmjdk.
            // The MtomTest systest fails unless we do this.
            attribute = root.getAttributeValue(null, qName.getLocalPart());
        }
        return new AttributeReader(qName, attribute);
    }

    public MessageReader getNextAttributeReader() {
        MessageReader reader = new AttributeReader(root.getAttributeName(currentAttribute), root
            .getAttributeValue(currentAttribute));
        currentAttribute++;

        return reader;
    }

    public String getNamespaceForPrefix(String prefix) {
        return root.getNamespaceURI(prefix);
    }
}
