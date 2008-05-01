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

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaForm;


public final class SchemaInfo extends AbstractPropertiesHolder {
  
    ServiceInfo serviceInfo;
    String namespaceUri;
    Element element;
    boolean isElementQualified;
    boolean isAttributeQualified;
    XmlSchema schema;
    String systemId;
    
    public SchemaInfo(ServiceInfo serviceInfo, String namespaceUri) {
        this(serviceInfo, namespaceUri, false, false);
    }
    public SchemaInfo(ServiceInfo serviceInfo, String namespaceUri,
                      boolean qElement, boolean qAttribute) {
        this.serviceInfo = serviceInfo;
        this.namespaceUri = namespaceUri;
        this.isElementQualified = qElement;
        this.isAttributeQualified = qAttribute;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer(this.getClass().getName());
        buffer.append(" [namespaceURI: ");
        buffer.append(namespaceUri);
        buffer.append("] [systemId: ");
        buffer.append(systemId);
        buffer.append("]");
        
        return buffer.toString();
    }
    
    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public String getNamespaceURI() {
        return namespaceUri;
    }

    public void setNamespaceURI(String nsUri) {
        this.namespaceUri = nsUri;
    }

    public synchronized Element getElement() {
        if (element == null && getSchema() != null) {
            CachedOutputStream cout = new CachedOutputStream();
            XmlSchema sch = getSchema();
            synchronized (sch) {
                getSchema().write(cout);
            }
            Document sdoc = null;
            try {
                sdoc = XMLUtils.parse(cout.getInputStream());
                cout.close();
            } catch (Exception e1) {
                return null;
            }
            
            Element e = sdoc.getDocumentElement();
            // XXX A problem can occur with the ibm jdk when the XmlSchema
            // object is serialized. The xmlns declaration gets incorrectly
            // set to the same value as the targetNamespace attribute.
            // The aegis databinding tests demonstrate this particularly.
            if (e.getPrefix() == null
                && !WSDLConstants.NS_SCHEMA_XSD.equals(e.getAttributeNS(WSDLConstants.NS_XMLNS,
                                                                        WSDLConstants.NP_XMLNS))) {
                e.setAttributeNS(WSDLConstants.NS_XMLNS, 
                                 WSDLConstants.NP_XMLNS, 
                                 WSDLConstants.NS_SCHEMA_XSD);
            }
            setElement(e);
        }
        return element;
    }

    public void setElement(Element element) {
        this.element = element;        
        String form = element.getAttribute("elementFormDefault");
        if ((form != null) && form.equals("qualified")) {
            isElementQualified = true;
        }
        form = element.getAttribute("attributeFormDefault");
        if ((form != null) && form.equals("qualified")) {
            isAttributeQualified = true;
        }
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
        isElementQualified = schema.getElementFormDefault().getValue().equals(XmlSchemaForm.QUALIFIED);
        isAttributeQualified = schema.getAttributeFormDefault().getValue().equals(XmlSchemaForm.QUALIFIED);
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
}
