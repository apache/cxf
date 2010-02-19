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

package org.apache.cxf.common.xmlschema;

import java.io.Reader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import org.apache.ws.commons.schema.ValidationEventHandler;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaContentModel;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaObjectTable;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.extensions.ExtensionRegistry;
import org.apache.ws.commons.schema.resolver.URIResolver;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.apache.ws.commons.schema.utils.NamespacePrefixList;
import org.apache.ws.commons.schema.utils.TargetNamespaceValidator;


/**
 * Wrapper class for XmlSchemaCollection that deals with various quirks and bugs. One bug is WSCOMMONS-272.
 */
public class SchemaCollection {
    private static final Method GET_ELEMENT_BY_NAME_METHOD;
    static {
        Method m = null;
        try {
            m = XmlSchema.class.getMethod("getElementByName", new Class[] {
                String.class
            });
        } catch (Exception ex) {
            // ignore
        }
        GET_ELEMENT_BY_NAME_METHOD = m;
    }

    private XmlSchemaCollection schemaCollection;
    private Map<XmlSchema, Set<XmlSchemaType>> xmlTypesCheckedForCrossImportsPerSchema 
        = new HashMap<XmlSchema, Set<XmlSchemaType>>();


    public SchemaCollection() {
        this(new XmlSchemaCollection());
    }

    public SchemaCollection(XmlSchemaCollection col) {
        schemaCollection = col;
        if (schemaCollection.getNamespaceContext() == null) {
            // an empty prefix map avoids extra checks for null.
            schemaCollection.setNamespaceContext(new NamespaceMap());
        }
    }

    public XmlSchemaCollection getXmlSchemaCollection() {
        return schemaCollection;
    }

    public boolean equals(Object obj) {
        if (obj instanceof SchemaCollection) {
            return schemaCollection.equals(((SchemaCollection)obj).schemaCollection);
        } else if (obj instanceof XmlSchemaCollection) {
            return schemaCollection.equals(obj);
        }
        return false;
    }

    public XmlSchemaElement getElementByQName(QName qname) {
        return schemaCollection.getElementByQName(qname);
    }

    public XmlSchemaAttribute getAttributeByQName(QName qname) {
        String uri = qname.getNamespaceURI();
        for (XmlSchema schema : schemaCollection.getXmlSchemas()) {
            if (uri.equals(schema.getTargetNamespace())) {
                XmlSchemaObjectTable attributes = schema.getAttributes();
                XmlSchemaAttribute attribute = (XmlSchemaAttribute)attributes.getItem(qname);
                if (attribute != null) {
                    return attribute;
                }
            }
        }
        return null;
    }

    public ExtensionRegistry getExtReg() {
        return schemaCollection.getExtReg();
    }

    public NamespacePrefixList getNamespaceContext() {
        return schemaCollection.getNamespaceContext();
    }

    public XmlSchemaType getTypeByQName(QName schemaTypeName) {
        XmlSchemaType xst = schemaCollection.getTypeByQName(schemaTypeName);

        // HACKY workaround for WSCOMMONS-355
        if (xst == null && "http://www.w3.org/2001/XMLSchema".equals(schemaTypeName.getNamespaceURI())) {
            XmlSchema sch = getSchemaByTargetNamespace(schemaTypeName.getNamespaceURI());

            if ("anySimpleType".equals(schemaTypeName.getLocalPart())) {
                XmlSchemaSimpleType type = new XmlSchemaSimpleType(sch);
                type.setName(schemaTypeName.getLocalPart());
                sch.addType(type);
                xst = type;
            } else if ("anyType".equals(schemaTypeName.getLocalPart())) {
                XmlSchemaType type = new XmlSchemaType(sch);
                type.setName(schemaTypeName.getLocalPart());
                sch.addType(type);
                xst = type;
            }
        }

        return xst;
    }

    public XmlSchema[] getXmlSchema(String systemId) {
        return schemaCollection.getXmlSchema(systemId);
    }

    public XmlSchema[] getXmlSchemas() {
        return schemaCollection.getXmlSchemas();
    }

    public int hashCode() {
        return schemaCollection.hashCode();
    }

    public void init() {
        schemaCollection.init();
    }

    public XmlSchema read(Document doc, String uri, ValidationEventHandler veh,
                          TargetNamespaceValidator validator) {
        return schemaCollection.read(doc, uri, veh, validator);
    }

    public XmlSchema read(Document doc, String uri, ValidationEventHandler veh) {
        return schemaCollection.read(doc, uri, veh);
    }

    public XmlSchema read(Document doc, ValidationEventHandler veh) {
        return schemaCollection.read(doc, veh);
    }

    public XmlSchema read(Element elem, String uri) {
        return schemaCollection.read(elem, uri);
    }

    public XmlSchema read(Element elem) {
        return schemaCollection.read(elem);
    }

    public XmlSchema read(InputSource inputSource, ValidationEventHandler veh) {
        return schemaCollection.read(inputSource, veh);
    }

    public XmlSchema read(Reader r, ValidationEventHandler veh) {
        return schemaCollection.read(r, veh);
    }

    public XmlSchema read(Source source, ValidationEventHandler veh) {
        return schemaCollection.read(source, veh);
    }

    public void setBaseUri(String baseUri) {
        schemaCollection.setBaseUri(baseUri);
    }

    public void setExtReg(ExtensionRegistry extReg) {
        schemaCollection.setExtReg(extReg);
    }

    public void setNamespaceContext(NamespacePrefixList namespaceContext) {
        schemaCollection.setNamespaceContext(namespaceContext);
    }

    public void setSchemaResolver(URIResolver schemaResolver) {
        schemaCollection.setSchemaResolver(schemaResolver);
    }

    /**
     * This function is not part of the XmlSchema API. Who knows why?
     * 
     * @param namespaceURI targetNamespace
     * @return schema, or null.
     */
    public XmlSchema getSchemaByTargetNamespace(String namespaceURI) {
        for (XmlSchema schema : schemaCollection.getXmlSchemas()) {
            if (schema.getTargetNamespace().equals(namespaceURI)) {
                return schema;
            }
        }
        return null;
    }

    public XmlSchema getSchemaForElement(QName name) {
        for (XmlSchema schema : schemaCollection.getXmlSchemas()) {
            if (name.getNamespaceURI().equals(schema.getTargetNamespace())) {

                // for XmlSchema 1.4, we should use:
                // schema.getElementByName(name.getLocalPart()) != null
                // but that doesn't exist in 1.3 so for now, use reflection
                try {
                    if (GET_ELEMENT_BY_NAME_METHOD != null) {
                        if (GET_ELEMENT_BY_NAME_METHOD.invoke(schema, new Object[] {
                            name.getLocalPart()
                        }) != null) {
                            return schema;
                        }
                    } else if (schema.getElementByName(name) != null) {
                        return schema;
                    }

                } catch (java.lang.reflect.InvocationTargetException ex) {
                    // ignore
                } catch (IllegalAccessException ex) {
                    // ignore
                }
            }
        }
        return null;
    }

    /**
     * Once upon a time, XmlSchema had a bug in the constructor used in this function. So this wrapper was
     * created to hold a workaround.
     * 
     * @param namespaceURI TNS for new schema.
     * @return new schema
     */

    public XmlSchema newXmlSchemaInCollection(String namespaceURI) {
        return new XmlSchema(namespaceURI, schemaCollection);
    }

    /**
     * Validate that a qualified name points to some namespace in the schema.
     * 
     * @param qname
     */
    public void validateQNameNamespace(QName qname) {
        // astonishingly, xmlSchemaCollection has no accessor by target URL.
        if ("".equals(qname.getNamespaceURI())) {
            return; // references to the 'unqualified' namespace are OK even if there is no schema for it.
        }
        for (XmlSchema schema : schemaCollection.getXmlSchemas()) {
            if (schema.getTargetNamespace().equals(qname.getNamespaceURI())) {
                return;
            }
        }
        throw new InvalidXmlSchemaReferenceException(qname + " refers to unknown namespace.");
    }

    public void validateElementName(QName referrer, QName elementQName) {
        XmlSchemaElement element = schemaCollection.getElementByQName(elementQName);
        if (element == null) {
            throw new InvalidXmlSchemaReferenceException(referrer + " references non-existent element "
                                                         + elementQName);
        }
    }

    public void validateTypeName(QName referrer, QName typeQName) {
        XmlSchemaType type = schemaCollection.getTypeByQName(typeQName);
        if (type == null) {
            throw new InvalidXmlSchemaReferenceException(referrer + " references non-existent type "
                                                         + typeQName);
        }
    }

    public void addGlobalElementToSchema(XmlSchemaElement element) {
        synchronized (this) {
            XmlSchema schema = getSchemaByTargetNamespace(element.getQName().getNamespaceURI());
            if (schema == null) {
                schema = newXmlSchemaInCollection(element.getQName().getNamespaceURI());
            }
            schema.getItems().add(element);
            // believe it or not, it is up to us to do both of these adds!
            schema.getElements().add(element.getQName(), element);
        }
    }

    public void addCrossImports() {
        /*
         * We need to inventory all the cross-imports to see if any are missing.
         */
        for (XmlSchema schema : schemaCollection.getXmlSchemas()) {
            addOneSchemaCrossImports(schema);
        }
    }

    private void addOneSchemaCrossImports(XmlSchema schema) {
        /*
         * We need to visit all the top-level items.
         */
        for (int x = 0; x < schema.getItems().getCount(); x++) {
            XmlSchemaObject item = schema.getItems().getItem(x);
            if (item instanceof XmlSchemaElement) {
                addElementCrossImportsElement(schema, item);
            } else if (item instanceof XmlSchemaAttribute) {
                XmlSchemaAttribute attr = (XmlSchemaAttribute)item;
                XmlSchemaUtils.addImportIfNeeded(schema, attr.getRefName());
                XmlSchemaUtils.addImportIfNeeded(schema, attr.getSchemaTypeName());
                if (attr.getSchemaType() != null) {
                    XmlSchemaUtils.addImportIfNeeded(schema, attr.getSchemaType().getBaseSchemaTypeName());
                }
            } else if (item instanceof XmlSchemaType) {
                XmlSchemaType type = (XmlSchemaType)item;
                addCrossImportsType(schema, type);
            }
        }
    }

    private void addElementCrossImportsElement(XmlSchema schema, XmlSchemaObject item) {
        XmlSchemaElement element = (XmlSchemaElement)item;
        XmlSchemaUtils.addImportIfNeeded(schema, element.getRefName());
        XmlSchemaUtils.addImportIfNeeded(schema, element.getSchemaTypeName());
        // if there's an anonymous type, it might have element refs in it.
        XmlSchemaType schemaType = element.getSchemaType();
        if (!crossImportsAdded(schema, schemaType)) {
            addCrossImportsType(schema, schemaType);
        }
    }

    /**
     * Determines whether the schema has already received (cross) imports for
     * the schemaType
     * @param schema
     * @param schemaType
     * @return false if cross imports for schemaType must still be added to schema
     */
    private boolean crossImportsAdded(XmlSchema schema, XmlSchemaType schemaType) {
        boolean result = true;
        if (schemaType != null) {
            Set<XmlSchemaType> xmlTypesCheckedForCrossImports;
            if (!xmlTypesCheckedForCrossImportsPerSchema.containsKey(schema)) {
                xmlTypesCheckedForCrossImports = new HashSet<XmlSchemaType>();
                xmlTypesCheckedForCrossImportsPerSchema.put(schema, xmlTypesCheckedForCrossImports);
            } else {
                xmlTypesCheckedForCrossImports = xmlTypesCheckedForCrossImportsPerSchema.get(schema);
            }
            if (!xmlTypesCheckedForCrossImports.contains(schemaType)) {
                // cross imports for this schemaType have not yet been added
                xmlTypesCheckedForCrossImports.add(schemaType);
                result = false;
            }
        }
        return result;
    }

    private void addCrossImportsType(XmlSchema schema, XmlSchemaType schemaType) {
        if (schemaType != null) {
            // the base type might cross schemas.
            XmlSchemaUtils.addImportIfNeeded(schema, schemaType.getBaseSchemaTypeName());
            if (schemaType instanceof XmlSchemaComplexType) {
                XmlSchemaComplexType complexType = (XmlSchemaComplexType)schemaType;
                addCrossImports(schema, complexType.getContentModel());
                addCrossImportsAttributeList(schema, complexType.getAttributes());
                // could it be a choice or something else?
                XmlSchemaSequence sequence = XmlSchemaUtils.getSequence(complexType);
                addCrossImportsSequence(schema, sequence);
            }
        }
    }

    private void addCrossImportsSequence(XmlSchema schema, XmlSchemaSequence sequence) {
        XmlSchemaObjectCollection items = sequence.getItems();
        for (int x = 0; x < items.getCount(); x++) {
            XmlSchemaObject seqItem = items.getItem(x);
            if (seqItem instanceof XmlSchemaElement) {
                addElementCrossImportsElement(schema, seqItem);
            }
        }
    }

    private void addCrossImportsAttributeList(XmlSchema schema, XmlSchemaObjectCollection attributes) {
        for (int x = 0; x < attributes.getCount(); x++) {
            XmlSchemaAttribute attr = (XmlSchemaAttribute)attributes.getItem(x);
            XmlSchemaUtils.addImportIfNeeded(schema, attr.getRefName());
        }
    }

    private void addCrossImports(XmlSchema schema, XmlSchemaContentModel contentModel) {
        if (contentModel == null) {
            return;
        }
        XmlSchemaContent content = contentModel.getContent();
        if (content == null) {
            return;
        }
        if (content instanceof XmlSchemaComplexContentExtension) {
            XmlSchemaComplexContentExtension extension = (XmlSchemaComplexContentExtension) content;
            XmlSchemaUtils.addImportIfNeeded(schema, extension.getBaseTypeName());
            addCrossImportsAttributeList(schema, extension.getAttributes());
            XmlSchemaParticle particle = extension.getParticle();
            if (particle instanceof XmlSchemaSequence) {
                addCrossImportsSequence(schema, (XmlSchemaSequence)particle);
            }
        } else if (content instanceof XmlSchemaComplexContentRestriction) {
            XmlSchemaComplexContentRestriction restriction = (XmlSchemaComplexContentRestriction) content;
            XmlSchemaUtils.addImportIfNeeded(schema, restriction.getBaseTypeName());
            addCrossImportsAttributeList(schema, restriction.getAttributes());
        } else if (content instanceof XmlSchemaSimpleContentExtension) {
            XmlSchemaSimpleContentExtension extension = (XmlSchemaSimpleContentExtension) content;
            XmlSchemaUtils.addImportIfNeeded(schema, extension.getBaseTypeName());
            addCrossImportsAttributeList(schema, extension.getAttributes());
        } else if (content instanceof XmlSchemaSimpleContentRestriction) {
            XmlSchemaSimpleContentRestriction restriction = (XmlSchemaSimpleContentRestriction) content;
            XmlSchemaUtils.addImportIfNeeded(schema, restriction.getBaseTypeName());
            addCrossImportsAttributeList(schema, restriction.getAttributes());
        }
    }

    public static void addGlobalElementToSchema(XmlSchema schema, XmlSchemaElement element) {
        synchronized (schema) {
            schema.getItems().add(element);
            // believe it or not, it is up to us to do both of these adds!
            schema.getElements().add(element.getQName(), element);
        }
    }

    public static void addGlobalTypeToSchema(XmlSchema schema, XmlSchemaType type) {
        synchronized (schema) {
            schema.getItems().add(type);
            schema.addType(type);
        }
    }
}