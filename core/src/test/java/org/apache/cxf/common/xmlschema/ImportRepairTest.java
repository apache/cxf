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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.staxutils.PrettyPrintXMLStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSerializer;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.utils.NamespaceMap;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 *
 */
public class ImportRepairTest {

    static boolean dumpSchemas;

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

        XmlSchemaAttribute importingAttribute = new XmlSchemaAttribute(importingSchema, false);
        importingAttribute.getRef().setTargetQName(new QName(ATTRIBUTE_SCHEMA, "imported"));
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

    Method findMethod(Object o, String name) {
        for (Method m: o.getClass().getMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }

    private void tryToParseSchemas() throws Exception {
        // Get DOM Implementation using DOM Registry
        final List<DOMLSInput> inputs = new ArrayList<>();
        final Map<String, LSInput> resolverMap = new HashMap<>();

        for (XmlSchema schema : collection.getXmlSchemas()) {
            if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(schema.getTargetNamespace())) {
                continue;
            }
            Document document = new XmlSchemaSerializer().serializeSchema(schema, false)[0];
            DOMLSInput input = new DOMLSInput(document, schema.getTargetNamespace());
            dumpSchema(document);
            resolverMap.put(schema.getTargetNamespace(), input);
            inputs.add(input);
        }

        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        DOMImplementation impl = registry.getDOMImplementation("XS-Loader");


        try {
            Object schemaLoader = findMethod(impl, "createXSLoader").invoke(impl, new Object[1]);
            DOMConfiguration config = (DOMConfiguration)findMethod(schemaLoader, "getConfig").invoke(schemaLoader);

            config.setParameter("validate", Boolean.TRUE);
            try {
                //bug in the JDK doesn't set this, but accesses it
                config.setParameter("http://www.oracle.com/xml/jaxp/properties/xmlSecurityPropertyManager",
                                    Class.forName("com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager")
                                    .getDeclaredConstructor().newInstance());

                config.setParameter("http://apache.org/xml/properties/security-manager",
                                    Class.forName("com.sun.org.apache.xerces.internal.utils.XMLSecurityManager")
                                    .getDeclaredConstructor().newInstance());
            } catch (Throwable t) {
                //ignore
            }
            config.setParameter("error-handler", new DOMErrorHandler() {

                public boolean handleError(DOMError error) {
                    LOG.info("Schema parsing error: " + error.getMessage()
                             + " " + error.getType()
                             + " " + error.getLocation().getUri()
                             + " " + error.getLocation().getLineNumber()
                             + ":" + error.getLocation().getColumnNumber());
                    throw new DOMErrorException(error);
                }
            });
            config.setParameter("resource-resolver", new LSResourceResolver() {

                public LSInput resolveResource(String type, String namespaceURI, String publicId,
                                               String systemId, String baseURI) {
                    return resolverMap.get(namespaceURI);
                }
            });

            Method m = findMethod(schemaLoader, "loadInputList");
            String name = m.getParameterTypes()[0].getName() + "Impl";
            name = name.replace("xs.LS", "impl.xs.util.LS");
            Class<?> c = Class.forName(name);
            Object inputList = c.getConstructor(LSInput[].class, Integer.TYPE)
            .newInstance(inputs.toArray(new LSInput[0]), inputs.size());

            findMethod(schemaLoader, "loadInputList").invoke(schemaLoader, inputList);
        } catch (InvocationTargetException ite) {
            throw (Exception)ite.getTargetException();
        }
    }

    private void dumpSchema(Document document) throws Exception {
        if (!dumpSchemas) {
            return;
        }

        XMLStreamWriter xwriter = StaxUtils.createXMLStreamWriter(System.err);
        xwriter = new PrettyPrintXMLStreamWriter(xwriter, 2);
        StaxUtils.copy(new DOMSource(document), xwriter);
        xwriter.close();
    }

    private void createTypeImportedByElement(XmlSchema elementTypeSchema) {
        XmlSchemaComplexType elementImportedType = new XmlSchemaComplexType(elementTypeSchema, true);
        elementImportedType.setName("importedElementType");
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
        XmlSchemaAttribute attributeImportingType = new XmlSchemaAttribute(importingSchema, true);
        attributeImportingType.setName("importingType");
        attributeImportingType.setSchemaTypeName(new QName(ATTRIBUTE_TYPE_SCHEMA, "importedAttributeType"));
    }

    private void createImportedAttributeType(XmlSchema attributeTypeSchema) {
        XmlSchemaSimpleType attributeImportedType = new XmlSchemaSimpleType(attributeTypeSchema, true);
        attributeImportedType.setName("importedAttributeType");
        XmlSchemaSimpleTypeRestriction simpleContent = new XmlSchemaSimpleTypeRestriction();
        attributeImportedType.setContent(simpleContent);
        simpleContent.setBaseTypeName(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string"));
    }

    private void createImportedAttribute(XmlSchema attributeSchema) {
        XmlSchemaAttribute importedAttribute = new XmlSchemaAttribute(attributeSchema, true);
        importedAttribute.setName("imported");
        importedAttribute.setSchemaTypeName(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string"));
    }

    private void createElementWithImportedType(XmlSchema importingSchema) {
        XmlSchemaElement elementWithImportedType = new XmlSchemaElement(importingSchema, true);
        elementWithImportedType.setName("elementWithImportedType");
        elementWithImportedType.setSchemaTypeName(new QName(ELEMENT_TYPE_SCHEMA, "importedElementType"));
    }

    private void createTypeImportingElement(XmlSchema importingSchema) {
        XmlSchemaComplexType typeWithElementRef = new XmlSchemaComplexType(importingSchema, true);
        typeWithElementRef.setName("typeWithRef");
        XmlSchemaSequence sequence = new XmlSchemaSequence();
        typeWithElementRef.setParticle(sequence);
        XmlSchemaElement refElement = new XmlSchemaElement(importingSchema, false);
        refElement.getRef().setTargetQName(new QName(ELEMENT_SCHEMA, "importedElement"));
    }

    private void createImportedElement(XmlSchema elementSchema) {
        XmlSchemaElement importedElement = new XmlSchemaElement(elementSchema, true);
        importedElement.setName("importedElement");
        importedElement.setSchemaTypeName(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string"));
    }

    private void createDerivedType2(XmlSchema importingSchema) {
        XmlSchemaComplexContent complexContent;
        XmlSchemaComplexType derivedType2 = new XmlSchemaComplexType(importingSchema, true);
        derivedType2.setName("derivedRestriction");
        XmlSchemaComplexContentRestriction restriction = new XmlSchemaComplexContentRestriction();
        restriction.setBaseTypeName(new QName(BASE_TYPE_SCHEMA2, "baseType2"));
        complexContent = new XmlSchemaComplexContent();
        complexContent.setContent(restriction);
        derivedType2.setContentModel(complexContent);
    }

    private XmlSchemaComplexContentExtension createDerivedType1(XmlSchema importingSchema) {
        XmlSchemaComplexType derivedType1 = new XmlSchemaComplexType(importingSchema, true);
        derivedType1.setName("derivedExtension");
        XmlSchemaComplexContentExtension extension = new XmlSchemaComplexContentExtension();
        extension.setBaseTypeName(new QName(BASE_TYPE_SCHEMA1, "baseType1"));
        XmlSchemaComplexContent complexContent = new XmlSchemaComplexContent();
        complexContent.setContent(extension);
        derivedType1.setContentModel(complexContent);
        return extension;
    }

    private XmlSchemaComplexType createBaseType2(XmlSchema baseTypeSchema2) {
        XmlSchemaComplexType baseType2 = new XmlSchemaComplexType(baseTypeSchema2, true);
        baseType2.setName("baseType2");
        baseType2.setParticle(new XmlSchemaSequence());
        return baseType2;
    }

    private void createBaseType1(XmlSchema baseTypeSchema1) {
        XmlSchemaComplexType baseType1 = new XmlSchemaComplexType(baseTypeSchema1, true);
        baseType1.setName("baseType1");
        baseType1.setParticle(new XmlSchemaSequence());
    }

}