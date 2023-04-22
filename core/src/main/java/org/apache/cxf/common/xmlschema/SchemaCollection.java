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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroupRef;
import org.apache.ws.commons.schema.XmlSchemaAttributeOrGroupRef;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaContentModel;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.extensions.ExtensionRegistry;
import org.apache.ws.commons.schema.resolver.URIResolver;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.apache.ws.commons.schema.utils.NamespacePrefixList;
import org.apache.ws.commons.schema.utils.XmlSchemaObjectBase;

/**
 * Wrapper class for XmlSchemaCollection that deals with various quirks and bugs.
 */
public class SchemaCollection {

    private final XmlSchemaCollection xmlSchemaCollection;
    private final Map<XmlSchema, Set<XmlSchemaType>> xmlTypesCheckedForCrossImportsPerSchema
        = new HashMap<>();

    public SchemaCollection() {
        this(new XmlSchemaCollection());
    }

    public SchemaCollection(XmlSchemaCollection col) {
        xmlSchemaCollection = col;
        if (xmlSchemaCollection.getNamespaceContext() == null) {
            // an empty prefix map avoids extra checks for null.
            xmlSchemaCollection.setNamespaceContext(new NamespaceMap());
        }
    }

    public XmlSchemaCollection getXmlSchemaCollection() {
        return xmlSchemaCollection;
    }

    public boolean equals(Object obj) {
        if (obj instanceof SchemaCollection) {
            return xmlSchemaCollection.equals(((SchemaCollection)obj).xmlSchemaCollection);
        } else if (obj instanceof XmlSchemaCollection) {
            return xmlSchemaCollection.equals(obj);
        }
        return false;
    }

    public XmlSchemaElement getElementByQName(QName qname) {
        return xmlSchemaCollection.getElementByQName(qname);
    }

    public XmlSchemaAttribute getAttributeByQName(QName qname) {
        return xmlSchemaCollection.getAttributeByQName(qname);
    }

    public ExtensionRegistry getExtReg() {
        return xmlSchemaCollection.getExtReg();
    }

    public NamespacePrefixList getNamespaceContext() {
        return xmlSchemaCollection.getNamespaceContext();
    }

    public XmlSchemaType getTypeByQName(QName schemaTypeName) {
        return xmlSchemaCollection.getTypeByQName(schemaTypeName);
    }

    public XmlSchema[] getXmlSchema(String systemId) {
        return xmlSchemaCollection.getXmlSchema(systemId);
    }

    public XmlSchema[] getXmlSchemas() {
        return xmlSchemaCollection.getXmlSchemas();
    }

    public int hashCode() {
        return xmlSchemaCollection.hashCode();
    }

    public void init() {
        xmlSchemaCollection.init();
    }

    public XmlSchema read(Element elem, String uri) {
        return xmlSchemaCollection.read(elem, uri);
    }

    public XmlSchema read(Document d, String uri) {
        return xmlSchemaCollection.read(d, uri);
    }

    public XmlSchema read(Element elem) {
        return xmlSchemaCollection.read(elem);
    }

    public void setBaseUri(String baseUri) {
        xmlSchemaCollection.setBaseUri(baseUri);
    }

    public void setExtReg(ExtensionRegistry extReg) {
        xmlSchemaCollection.setExtReg(extReg);
    }

    public void setNamespaceContext(NamespacePrefixList namespaceContext) {
        xmlSchemaCollection.setNamespaceContext(namespaceContext);
    }

    public void setSchemaResolver(URIResolver schemaResolver) {
        xmlSchemaCollection.setSchemaResolver(schemaResolver);
    }

    /**
     * This function is not part of the XmlSchema API. Who knows why?
     *
     * @param namespaceURI targetNamespace
     * @return schema, or null.
     */
    public XmlSchema getSchemaByTargetNamespace(String namespaceURI) {
        for (XmlSchema schema : xmlSchemaCollection.getXmlSchemas()) {
            if (namespaceURI != null && namespaceURI.equals(schema.getTargetNamespace())
                || namespaceURI == null && schema.getTargetNamespace() == null) {
                return schema;
            }
        }
        return null;
    }

    public XmlSchema getSchemaForElement(QName name) {
        for (XmlSchema schema : xmlSchemaCollection.getXmlSchemas()) {
            if (name.getNamespaceURI().equals(schema.getTargetNamespace())) {

                if (schema.getElementByName(name.getLocalPart()) != null) {
                    return schema;
                } else if (schema.getElementByName(name) != null) {
                    return schema;
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
        return new XmlSchema(namespaceURI, xmlSchemaCollection);
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
        for (XmlSchema schema : xmlSchemaCollection.getXmlSchemas()) {
            if (schema.getTargetNamespace().equals(qname.getNamespaceURI())) {
                return;
            }
        }
        throw new InvalidXmlSchemaReferenceException(qname + " refers to unknown namespace.");
    }

    public void validateElementName(QName referrer, QName elementQName) {
        XmlSchemaElement element = xmlSchemaCollection.getElementByQName(elementQName);
        if (element == null) {
            throw new InvalidXmlSchemaReferenceException(referrer + " references non-existent element "
                                                         + elementQName);
        }
    }

    public void validateTypeName(QName referrer, QName typeQName) {
        XmlSchemaType type = xmlSchemaCollection.getTypeByQName(typeQName);
        if (type == null) {
            throw new InvalidXmlSchemaReferenceException(referrer + " references non-existent type "
                                                         + typeQName);
        }
    }

    public void addCrossImports() {
        /*
         * We need to inventory all the cross-imports to see if any are missing.
         */
        for (XmlSchema schema : xmlSchemaCollection.getXmlSchemas()) {
            addOneSchemaCrossImports(schema);
        }
    }

    private void addOneSchemaCrossImports(XmlSchema schema) {
        /*
         * We need to visit all the top-level items.
         */
        for (XmlSchemaElement element : schema.getElements().values()) {
            addElementCrossImportsElement(schema, element);
        }
        for (XmlSchemaAttribute attribute : schema.getAttributes().values()) {
            XmlSchemaUtils.addImportIfNeeded(schema, attribute.getRef().getTargetQName());
            XmlSchemaUtils.addImportIfNeeded(schema, attribute.getSchemaTypeName());
        }
        for (XmlSchemaType type : schema.getSchemaTypes().values()) {
            addCrossImportsType(schema, type);
        }
    }

    private void addElementCrossImportsElement(XmlSchema schema, XmlSchemaElement item) {
        XmlSchemaElement element = item;
        XmlSchemaUtils.addImportIfNeeded(schema, element.getRef().getTargetQName());
        XmlSchemaUtils.addImportIfNeeded(schema, element.getSchemaTypeName());
        // if there's an anonymous type, it might have element refs in it.
        XmlSchemaType schemaType = element.getSchemaType();
        if (!crossImportsAdded(schema, schemaType)) {
            addCrossImportsType(schema, schemaType);
        }
    }

    /**
     * Determines whether the schema has already received (cross) imports for the schemaType
     *
     * @param schema
     * @param schemaType
     * @return false if cross imports for schemaType must still be added to schema
     */
    private boolean crossImportsAdded(XmlSchema schema, XmlSchemaType schemaType) {
        boolean result = true;
        if (schemaType != null) {
            Set<XmlSchemaType> xmlTypesCheckedForCrossImports;
            if (!xmlTypesCheckedForCrossImportsPerSchema.containsKey(schema)) {
                xmlTypesCheckedForCrossImports = new HashSet<>();
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
        // the base type might cross schemas.
        if (schemaType instanceof XmlSchemaComplexType) {
            XmlSchemaComplexType complexType = (XmlSchemaComplexType)schemaType;
            XmlSchemaUtils.addImportIfNeeded(schema, complexType.getBaseSchemaTypeName());
            addCrossImports(schema, complexType.getContentModel());
            addCrossImportsAttributeList(schema, complexType.getAttributes());
            // could it be a choice or something else?

            if (complexType.getParticle() instanceof XmlSchemaChoice) {
                XmlSchemaChoice choice = (XmlSchemaChoice)complexType.getParticle();
                addCrossImports(schema, choice);
            } else if (complexType.getParticle() instanceof XmlSchemaAll) {
                XmlSchemaAll all = (XmlSchemaAll)complexType.getParticle();
                addCrossImports(schema, all);
            } else if (complexType.getParticle() instanceof XmlSchemaSequence) {
                XmlSchemaSequence sequence = (XmlSchemaSequence)complexType.getParticle();
                addCrossImports(schema, sequence);
            }
        }
    }
    private void addCrossImports(XmlSchema schema, XmlSchemaAll all) {
        for (XmlSchemaObjectBase seqMember : all.getItems()) {
            if (seqMember instanceof XmlSchemaElement) {
                addElementCrossImportsElement(schema, (XmlSchemaElement)seqMember);
            }
        }
    }

    private void addCrossImports(XmlSchema schema, XmlSchemaChoice choice) {
        for (XmlSchemaObjectBase seqMember : choice.getItems()) {
            if (seqMember instanceof XmlSchemaElement) {
                addElementCrossImportsElement(schema, (XmlSchemaElement)seqMember);
            }
        }
    }
    private void addCrossImports(XmlSchema schema, XmlSchemaSequence sequence) {
        for (XmlSchemaSequenceMember seqMember : sequence.getItems()) {
            if (seqMember instanceof XmlSchemaElement) {
                addElementCrossImportsElement(schema, (XmlSchemaElement)seqMember);
            }
        }
    }

    private void addCrossImportsAttributeList(XmlSchema schema, List<XmlSchemaAttributeOrGroupRef> list) {
        for (XmlSchemaAttributeOrGroupRef attr : list) {
            final QName ref;
            if (attr instanceof XmlSchemaAttribute) {
                ref = ((XmlSchemaAttribute)attr).getRef().getTargetQName();
            } else {
                XmlSchemaAttributeGroupRef groupRef = (XmlSchemaAttributeGroupRef)attr;
                ref = groupRef.getRef().getTargetQName();
            }

            if (ref != null) {
                XmlSchemaUtils.addImportIfNeeded(schema, ref);
            }
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
            XmlSchemaComplexContentExtension extension = (XmlSchemaComplexContentExtension)content;
            XmlSchemaUtils.addImportIfNeeded(schema, extension.getBaseTypeName());
            addCrossImportsAttributeList(schema, extension.getAttributes());
            XmlSchemaParticle particle = extension.getParticle();
            if (particle instanceof XmlSchemaSequence) {
                addCrossImports(schema, (XmlSchemaSequence)particle);
            } else if (particle instanceof XmlSchemaChoice) {
                addCrossImports(schema, (XmlSchemaChoice)particle);
            } else if (particle instanceof XmlSchemaAll) {
                addCrossImports(schema, (XmlSchemaAll)particle);
            }
        } else if (content instanceof XmlSchemaComplexContentRestriction) {
            XmlSchemaComplexContentRestriction restriction = (XmlSchemaComplexContentRestriction)content;
            XmlSchemaUtils.addImportIfNeeded(schema, restriction.getBaseTypeName());
            addCrossImportsAttributeList(schema, restriction.getAttributes());
        } else if (content instanceof XmlSchemaSimpleContentExtension) {
            XmlSchemaSimpleContentExtension extension = (XmlSchemaSimpleContentExtension)content;
            XmlSchemaUtils.addImportIfNeeded(schema, extension.getBaseTypeName());
            addCrossImportsAttributeList(schema, extension.getAttributes());
        } else if (content instanceof XmlSchemaSimpleContentRestriction) {
            XmlSchemaSimpleContentRestriction restriction = (XmlSchemaSimpleContentRestriction)content;
            XmlSchemaUtils.addImportIfNeeded(schema, restriction.getBaseTypeName());
            addCrossImportsAttributeList(schema, restriction.getAttributes());
        }
    }

}
