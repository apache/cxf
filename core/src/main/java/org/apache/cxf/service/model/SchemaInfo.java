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

package org.apache.cxf.service.model;

import java.lang.ref.SoftReference;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;

public final class SchemaInfo extends AbstractPropertiesHolder {

    private String namespaceUri;
    private boolean isElementQualified;
    private boolean isAttributeQualified;
    private XmlSchema schema;
    private String systemId;
    // Avoid re-serializing all the time. Particularly as a cached WSDL will
    // hold a reference to the element.
    private SoftReference<Element> cachedElement;

    public SchemaInfo(String namespaceUri) {
        this(namespaceUri, false, false);
    }
    public SchemaInfo(String namespaceUri,
                      boolean qElement, boolean qAttribute) {
        this.namespaceUri = namespaceUri;
        this.isElementQualified = qElement;
        this.isAttributeQualified = qAttribute;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder(64).append(this.getClass().getName());
        buffer.append(" [namespaceURI: ");
        buffer.append(namespaceUri);
        buffer.append("] [systemId: ");
        buffer.append(systemId);
        buffer.append(']');

        return buffer.toString();
    }

    public String getNamespaceURI() {
        return namespaceUri;
    }

    public void setNamespaceURI(String nsUri) {
        this.namespaceUri = nsUri;
    }

    public synchronized void setElement(Element el) {
        cachedElement = new SoftReference<>(el);
    }

    /**
     * Build and return a DOM tree for this schema.
     * @return a DOM Element representation of the schema
     */
    public synchronized Element getElement() {
        // if someone recently used this DOM tree, take advantage.
        Element element = cachedElement == null ? null : cachedElement.get();
        if (element != null) {
            return element;
        }
        if (getSchema() == null) {
            throw new RuntimeException("No XmlSchema in SchemaInfo");
        }

        XmlSchema sch = getSchema();
        synchronized (sch) {
            XmlSchema schAgain = getSchema();
            // XML Schema blows up when the context is null as opposed to empty.
            // Some unit tests really want to see 'tns:'.
            if (schAgain.getNamespaceContext() == null) {
                NamespaceMap nsMap = new NamespaceMap();
                nsMap.add("xsd", Constants.URI_2001_SCHEMA_XSD);
                nsMap.add("tns", schAgain.getTargetNamespace());
                schAgain.setNamespaceContext(nsMap);
            }
            Document serializedSchema;
            try {
                serializedSchema = schAgain.getSchemaDocument();
            } catch (XmlSchemaSerializerException e) {
                throw new RuntimeException("Error serializing Xml Schema", e);
            }
            element = serializedSchema.getDocumentElement();
            cachedElement = new SoftReference<>(element);
        }
        // A problem can occur with the ibm jdk when the XmlSchema
        // object is serialized. The xmlns declaration gets incorrectly
        // set to the same value as the targetNamespace attribute.
        // The aegis databinding tests demonstrate this particularly.
        if (element.getPrefix() == null
            && !Constants.URI_2001_SCHEMA_XSD.equals(element.getAttributeNS(Constants.XMLNS_ATTRIBUTE_NS_URI,
                                                                            Constants.XMLNS_ATTRIBUTE))) {

            Attr attr = element.getOwnerDocument()
                .createAttributeNS(Constants.XMLNS_ATTRIBUTE_NS_URI, Constants.XMLNS_ATTRIBUTE);
            attr.setValue(Constants.URI_2001_SCHEMA_XSD);
            element.setAttributeNodeNS(attr);
        }
        return element;
    }

    public boolean isElementFormQualified() {
        return isElementQualified;
    }

    public boolean isAttributeFormQualified() {
        return isAttributeQualified;
    }

    public XmlSchema getSchema() {
        return schema;
    }

    public void setSchema(XmlSchema schema) {
        this.schema = schema;
        isElementQualified = schema.getElementFormDefault().equals(XmlSchemaForm.QUALIFIED);
        isAttributeQualified = schema.getAttributeFormDefault().equals(XmlSchemaForm.QUALIFIED);
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public XmlSchemaElement getElementByQName(QName qname) {
        String uri = qname.getNamespaceURI();
        if (schema != null
            && schema.getTargetNamespace() != null
            && schema.getTargetNamespace().equals(uri)) {
            return schema.getElementByName(qname);
        }
        return null;
    }

    String getNamespaceUri() {
        return namespaceUri;
    }

    boolean isElementQualified() {
        return isElementQualified;
    }

    boolean isAttributeQualified() {
        return isAttributeQualified;
    }
}
