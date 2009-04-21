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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;

import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSerializer;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.apache.xerces.xs.LSInputList;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 */
public class ImportRepairTest extends Assert {
    private static final class ListLSInput implements LSInputList {
        private final List<DOMLSInput> inputs;

        private ListLSInput(List<DOMLSInput> inputs) {
            this.inputs = inputs;
        }

        public int getLength() {
            return inputs.size();
        }

        public LSInput item(int index) {
            return inputs.get(index);
        }
    }

    private static final Logger LOG = LogUtils.getL7dLogger(ImportRepairTest.class);

    private static final String IMPORTING_SCHEMA = "urn:importing";
    private static final String BASE_TYPE_SCHEMA1 = "urn:baseType1";
    private static final String BASE_TYPE_SCHEMA2 = "urn:baseType2";
    private static final String ELEMENT_TYPE_SCHEMA = "urn:elementType";
    private static final String ELEMENT_SCHEMA = "urn:element";
    private static final String ATTRIBUTE_SCHEMA = "urn:attribute";
    private static final String ATTRIBUTE_TYPE_SCHEMA = "urn:attributeType";

    private SchemaCollection collection;

    @Test
    public void testImportRepairs() throws Exception {
        if (System.getProperty("java.vendor").contains("IBM")) {
            //the version of xerces built into IBM jdk won't work
            //and we cannot get a good version unless we endorse it
            return;
        }
        
        collection = new SchemaCollection();
        XmlSchema importingSchema = newSchema(IMPORTING_SCHEMA);
        XmlSchema baseTypeSchema1 = newSchema(BASE_TYPE_SCHEMA1);
        XmlSchema baseTypeSchema2 = newSchema(BASE_TYPE_SCHEMA2);
        XmlSchema elementTypeSchema = newSchema(ELEMENT_TYPE_SCHEMA);
        XmlSchema elementSchema = newSchema(ELEMENT_SCHEMA);
        XmlSchema attributeSchema = newSchema(ATTRIBUTE_SCHEMA);
        XmlSchema attributeTypeSchema = newSchema(ATTRIBUTE_TYPE_SCHEMA);

        createBaseType1(baseTypeSchema1);

        createBaseType2(baseTypeSchema2);
        XmlSchemaComplexContentExtension derivedType1Extension = createDerivedType1(importingSchema);
        createDerivedType2(importingSchema);

        createImportedElement(elementSchema);

        createTypeImportingElement(importingSchema);

        createTypeImportedByElement(elementTypeSchema);

        createElementWithImportedType(importingSchema);

        createImportedAttribute(attributeSchema);

        XmlSchemaAttribute importingAttribute = new XmlSchemaAttribute();
        importingAttribute.setRefName(new QName(ATTRIBUTE_SCHEMA, "imported"));
        // borrow derivedType1 to make the reference.
        derivedType1Extension.getAttributes().add(importingAttribute);

        createImportedAttributeType(attributeTypeSchema);

        createAttributeImportingType(importingSchema);

        /*
         * Notice that no imports have been added. In an ideal world, XmlSchema would do this for us.
         */
        try {
            tryToParseSchemas();
            fail("Expected an exception");
        } catch (DOMErrorException e) {
            //ignore, expected
        }
        LOG.info("adding imports");
        collection.addCrossImports();
        tryToParseSchemas();
    }

    private void tryToParseSchemas() throws ClassNotFoundException, InstantiationException,
        IllegalAccessException, XmlSchemaSerializerException, TransformerException {
        // Get DOM Implementation using DOM Registry
        final List<DOMLSInput> inputs = new ArrayList<DOMLSInput>();
        final Map<String, LSInput> resolverMap = new HashMap<String, LSInput>();

        for (XmlSchema schema : collection.getXmlSchemas()) {
            if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(schema.getTargetNamespace())) {
                continue;
            }
            Document document = new XmlSchemaSerializer().serializeSchema(schema, false)[0];
            DOMLSInput input = new DOMLSInput(document, schema.getTargetNamespace());
            resolverMap.put(schema.getTargetNamespace(), input);
            inputs.add(input);
        }

        System.setProperty(DOMImplementationRegistry.PROPERTY,
                           "org.apache.xerces.dom.DOMXSImplementationSourceImpl");
        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();

        XSImplementation impl = (XSImplementation)registry.getDOMImplementation("XS-Loader");

        XSLoader schemaLoader = impl.createXSLoader(null);
        schemaLoader.getConfig().setParameter("validate", Boolean.TRUE);
        schemaLoader.getConfig().setParameter("error-handler", new DOMErrorHandler() {

            public boolean handleError(DOMError error) {
                LOG.info("Schema parsing error: " + error.getMessage());
                throw new DOMErrorException(error);
            }
        });
        schemaLoader.getConfig().setParameter("resource-resolver", new LSResourceResolver() {

            public LSInput resolveResource(String type, String namespaceURI, String publicId,
                                           String systemId, String baseURI) {
                return resolverMap.get(namespaceURI);
            }
        });

        schemaLoader.loadInputList(new ListLSInput(inputs));
    }

    private void createTypeImportedByElement(XmlSchema elementTypeSchema) {
        XmlSchemaComplexType elementImportedType = new XmlSchemaComplexType(elementTypeSchema);
        elementImportedType.setName("importedElementType");
        elementTypeSchema.addType(elementImportedType);
        elementTypeSchema.getItems().add(elementImportedType);
        elementImportedType.setParticle(new XmlSchemaSequence());
    }

    private XmlSchema newSchema(String uri) {
        XmlSchema schema = collection.newXmlSchemaInCollection(uri);
        NamespaceMap map = new NamespaceMap();
        map.add("", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schema.setNamespaceContext(map);
        return schema;
    }

    private void createAttributeImportingType(XmlSchema importingSchema) {
        XmlSchemaAttribute attributeImportingType = new XmlSchemaAttribute();
        attributeImportingType.setName("importingType");
        importingSchema.getAttributes().add(new QName(ELEMENT_SCHEMA, "importingTypeAttribute"),
                                            attributeImportingType);
        importingSchema.getItems().add(attributeImportingType);
        attributeImportingType.setSchemaTypeName(new QName(ATTRIBUTE_TYPE_SCHEMA, "importedAttributeType"));
    }

    private void createImportedAttributeType(XmlSchema attributeTypeSchema) {
        XmlSchemaSimpleType attributeImportedType = new XmlSchemaSimpleType(attributeTypeSchema);
        attributeImportedType.setName("importedAttributeType");
        attributeTypeSchema.addType(attributeImportedType);
        attributeTypeSchema.getItems().add(attributeImportedType);
        XmlSchemaSimpleTypeRestriction simpleContent = new XmlSchemaSimpleTypeRestriction();
        attributeImportedType.setContent(simpleContent);
        simpleContent.setBaseTypeName(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string"));
    }

    private void createImportedAttribute(XmlSchema attributeSchema) {
        XmlSchemaAttribute importedAttribute = new XmlSchemaAttribute();
        importedAttribute.setName("imported");
        importedAttribute.setSchemaTypeName(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string"));
        attributeSchema.getAttributes().add(new QName(ATTRIBUTE_SCHEMA, "imported"), importedAttribute);
        attributeSchema.getItems().add(importedAttribute);
    }

    private void createElementWithImportedType(XmlSchema importingSchema) {
        XmlSchemaElement elementWithImportedType = new XmlSchemaElement();
        elementWithImportedType.setName("elementWithImportedType");
        elementWithImportedType.setSchemaTypeName(new QName(ELEMENT_TYPE_SCHEMA, "importedElementType"));
        importingSchema.getItems().add(elementWithImportedType);
        importingSchema.getElements().add(elementWithImportedType.getQName(), elementWithImportedType);
    }

    private void createTypeImportingElement(XmlSchema importingSchema) {
        XmlSchemaComplexType typeWithElementRef = new XmlSchemaComplexType(importingSchema);
        typeWithElementRef.setName("typeWithRef");
        importingSchema.addType(typeWithElementRef);
        importingSchema.getItems().add(typeWithElementRef);
        XmlSchemaSequence sequence = new XmlSchemaSequence();
        typeWithElementRef.setParticle(sequence);
        XmlSchemaElement refElement = new XmlSchemaElement();
        sequence.getItems().add(refElement);
        refElement.setRefName(new QName(ELEMENT_SCHEMA, "importedElement"));
    }

    private void createImportedElement(XmlSchema elementSchema) {
        XmlSchemaElement importedElement = new XmlSchemaElement();
        importedElement.setName("importedElement");
        importedElement.setSchemaTypeName(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string"));
        elementSchema.getElements().add(new QName(ELEMENT_SCHEMA, "importedElement"), importedElement);
        elementSchema.getItems().add(importedElement);
    }

    private void createDerivedType2(XmlSchema importingSchema) {
        XmlSchemaComplexContent complexContent;
        XmlSchemaComplexType derivedType2 = new XmlSchemaComplexType(importingSchema);
        derivedType2.setName("derivedRestriction");
        XmlSchemaComplexContentRestriction restriction = new XmlSchemaComplexContentRestriction();
        restriction.setBaseTypeName(new QName(BASE_TYPE_SCHEMA2, "baseType2"));
        complexContent = new XmlSchemaComplexContent();
        complexContent.setContent(restriction);
        derivedType2.setContentModel(complexContent);
        importingSchema.addType(derivedType2);
        importingSchema.getItems().add(derivedType2);
    }

    private XmlSchemaComplexContentExtension createDerivedType1(XmlSchema importingSchema) {
        XmlSchemaComplexType derivedType1 = new XmlSchemaComplexType(importingSchema);
        derivedType1.setName("derivedExtension");
        XmlSchemaComplexContentExtension extension = new XmlSchemaComplexContentExtension();
        extension.setBaseTypeName(new QName(BASE_TYPE_SCHEMA1, "baseType1"));
        XmlSchemaComplexContent complexContent = new XmlSchemaComplexContent();
        complexContent.setContent(extension);
        derivedType1.setContentModel(complexContent);
        importingSchema.addType(derivedType1);
        importingSchema.getItems().add(derivedType1);
        return extension;
    }

    private XmlSchemaComplexType createBaseType2(XmlSchema baseTypeSchema2) {
        XmlSchemaComplexType baseType2 = new XmlSchemaComplexType(baseTypeSchema2);
        baseType2.setName("baseType2");
        baseTypeSchema2.addType(baseType2);
        baseTypeSchema2.getItems().add(baseType2);
        baseType2.setParticle(new XmlSchemaSequence());
        return baseType2;
    }

    private void createBaseType1(XmlSchema baseTypeSchema1) {
        XmlSchemaComplexType baseType1 = new XmlSchemaComplexType(baseTypeSchema1);
        baseType1.setName("baseType1");
        baseTypeSchema1.addType(baseType1);
        baseTypeSchema1.getItems().add(baseType1);
        baseType1.setParticle(new XmlSchemaSequence());
    }

}
