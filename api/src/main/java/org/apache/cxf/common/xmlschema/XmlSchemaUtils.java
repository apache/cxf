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
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeOrGroupRef;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaContentModel;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;

/**
 * Some functions that avoid problems with Commons XML Schema.
 */
public final class XmlSchemaUtils {
    public static final String XSI_NIL = "xsi:nil='true'";
    public static final String XSI_NS_ATTR = WSDLConstants.NP_XMLNS + ":"
        + WSDLConstants.NP_SCHEMA_XSI + "='" + WSDLConstants.NS_SCHEMA_XSI + "'";
    public static final String XSI_NIL_WITH_PREFIX = XSI_NS_ATTR + " xsi:nil='true'";

    private static final Logger LOG = LogUtils.getL7dLogger(XmlSchemaUtils.class);
    private static final XmlSchemaSequence EMPTY_SEQUENCE = new XmlSchemaSequence();
    private static final XmlSchemaChoice EMPTY_CHOICE = new XmlSchemaChoice();
    private static final XmlSchemaAll EMPTY_ALL = new XmlSchemaAll();

    private XmlSchemaUtils() {
    }

    /**
     * Wrapper around XmlSchemaElement.setName that checks for inconsistency with
     * refName.
     * @param element
     * @param name
     */
    public static void setElementName(XmlSchemaElement element, String name) {
        if (name != null
            && element.isRef()
            && !element.getRef().getTargetQName().getLocalPart().equals(name)
            && (element.getQName() == null || element.getQName().getLocalPart().equals(name))) {
            LOG.severe("Attempt to set the name of an element with a reference name.");
            throw new
                XmlSchemaInvalidOperation("Attempt to set the name of an element "
                                          + "with a reference name.");
        }
        element.setName(name);
    }

    /**
     * Wrapper around XmlSchemaElement.setRefName that checks for inconsistency with
     * name and QName.
     * @param element
     * @param name
     */
    public static void setElementRefName(XmlSchemaElement element, QName name) {
        if (name != null
            && ((element.getQName() != null && !element.getQName().equals(name))
            || (element.getName() != null && !element.getName().equals(name.getLocalPart())))) {
            LOG.severe("Attempt to set the refName of an element with a name or QName");
            throw new
                XmlSchemaInvalidOperation("Attempt to set the refName of an element "
                                          + "with a name or QName.");
        }
        element.getRef().setTargetQName(name);
        // cxf conventionally keeps something in the name slot.
    }

    /**
     * Return true if a simple type is a straightforward XML Schema representation of an enumeration.
     * If we discover schemas that are 'enum-like' with more complex structures, we might
     * make this deal with them.
     * @param type Simple type, possible an enumeration.
     * @return true for an enumeration.
     */
    public static boolean isEumeration(XmlSchemaSimpleType type) {
        XmlSchemaSimpleTypeContent content = type.getContent();
        if (!(content instanceof XmlSchemaSimpleTypeRestriction)) {
            return false;
        }
        XmlSchemaSimpleTypeRestriction restriction = (XmlSchemaSimpleTypeRestriction) content;
        List<XmlSchemaFacet> facets = restriction.getFacets();
        for (XmlSchemaFacet facet : facets) {
            if (!(facet instanceof XmlSchemaEnumerationFacet)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Retrieve the string values for an enumeration.
     * @param type
     * @return
     */
    public static List<String> enumeratorValues(XmlSchemaSimpleType type) {
        XmlSchemaSimpleTypeContent content = type.getContent();
        XmlSchemaSimpleTypeRestriction restriction = (XmlSchemaSimpleTypeRestriction) content;
        List<XmlSchemaFacet> facets = restriction.getFacets();
        List<String> values = new ArrayList<String>();
        for (XmlSchemaFacet facet : facets) {
            XmlSchemaEnumerationFacet enumFacet = (XmlSchemaEnumerationFacet) facet;
            values.add(enumFacet.getValue().toString());
        }
        return values;
    }

    /**
     * Is there an import for a particular namespace in a schema?
     * @param schema
     * @param namespaceUri
     * @return
     */
    public static boolean schemaImportsNamespace(XmlSchema schema, String namespaceUri) {
        List<XmlSchemaExternal> externals =  schema.getExternals();
        for (XmlSchemaExternal what : externals) {
            if (what instanceof XmlSchemaImport) {
                XmlSchemaImport imp = (XmlSchemaImport)what;
                // already there.
                if (namespaceUri.equals(imp.getNamespace())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Assist in managing the required <import namespace='uri'> for imports of peer schemas.
     * @param schema
     * @param namespaceUri
     */
    public static void addImportIfNeeded(XmlSchema schema, String namespaceUri) {
        // no need to import nothing or the XSD schema, or the schema we are fixing.
        if ("".equals(namespaceUri)
            || XmlSchemaConstants.XSD_NAMESPACE_URI.equals(namespaceUri)
            || schema.getTargetNamespace().equals(namespaceUri)) {
            return;
        }

        List<XmlSchemaExternal> externals = schema.getExternals();
        for (XmlSchemaExternal what : externals) {
            if (what instanceof XmlSchemaImport) {
                XmlSchemaImport imp = (XmlSchemaImport)what;
                // already there.
                if (namespaceUri.equals(imp.getNamespace())) {
                    return;
                }
            }
        }
        XmlSchemaImport imp = new XmlSchemaImport(schema);
        imp.setNamespace(namespaceUri);
    }

    /**
     * For convenience, start from a qname, and add the import if it is non-null
     * and has a namespace.
     * @see #addImportIfNeeded(XmlSchema, String)
     * @param schema
     * @param qname
     */
    public static void addImportIfNeeded(XmlSchema schema, QName qname) {
        if (qname == null) {
            return;
        }
        if (qname.getNamespaceURI() == null) {
            return;
        }
        addImportIfNeeded(schema, qname.getNamespaceURI());
    }

    /**
     * This copes with an observed phenomenon in the schema built by the
     * ReflectionServiceFactoryBean. It is creating element such that: (a) the
     * type is not set. (b) the refName is set. (c) the namespaceURI in the
     * refName is set empty. This apparently indicates 'same Schema' to everyone
     * else, so thus function implements that convention here. It is unclear if
     * that is a correct structure, and it if changes, we can simplify or
     * eliminate this function.
     *
     * @param name
     * @param referencingURI
     * @return
     */
    public static XmlSchemaElement findElementByRefName(SchemaCollection xmlSchemaCollection,
                                                         QName name,
                                                         String referencingURI) {
        String uri = name.getNamespaceURI();
        if ("".equals(uri)) {
            uri = referencingURI;
        }
        QName copyName = new QName(uri, name.getLocalPart());
        XmlSchemaElement target = xmlSchemaCollection.getElementByQName(copyName);
        assert target != null;
        return target;
    }

    public static QName getBaseType(XmlSchemaComplexType type) {
        XmlSchemaContentModel model = type.getContentModel();
        if (model == null) {
            return null;
        }
        XmlSchemaContent content = model.getContent();
        if (content == null) {
            return null;
        }

        if (!(content instanceof XmlSchemaComplexContentExtension)) {
            return null;
        }

        XmlSchemaComplexContentExtension ext = (XmlSchemaComplexContentExtension)content;
        return ext.getBaseTypeName();
    }

    public static List<XmlSchemaAttributeOrGroupRef> getContentAttributes(XmlSchemaComplexType type) {
        XmlSchemaContentModel model = type.getContentModel();
        if (model == null) {
            return null;
        }
        XmlSchemaContent content = model.getContent();
        if (content == null) {
            return null;
        }
        if (!(content instanceof XmlSchemaComplexContentExtension)) {
            return null;
        }

        //TODO: the anyAttribute case.
        XmlSchemaComplexContentExtension ext = (XmlSchemaComplexContentExtension)content;
        return ext.getAttributes();
    }

    public static List<XmlSchemaAnnotated>
    getContentAttributes(XmlSchemaComplexType type, SchemaCollection collection) {
        List<XmlSchemaAnnotated> results = new ArrayList<XmlSchemaAnnotated>();
        QName baseTypeName = getBaseType(type);
        if (baseTypeName != null) {
            XmlSchemaComplexType baseType = (XmlSchemaComplexType)collection.getTypeByQName(baseTypeName);
            // recurse onto the base type ...
            results.addAll(getContentAttributes(baseType, collection));
            // and now process our sequence.
            List<XmlSchemaAttributeOrGroupRef> extAttrs = getContentAttributes(type);
            results.addAll(extAttrs);
            return results;
        } else {
            // no base type, the simple case.
            List<XmlSchemaAttributeOrGroupRef> attrs = type.getAttributes();
            results.addAll(attrs);
            return results;
        }
    }

    public static List<XmlSchemaObject> getContentElements(XmlSchemaComplexType type,
                                                           SchemaCollection collection) {
        List<XmlSchemaObject> results = new ArrayList<XmlSchemaObject>();
        QName baseTypeName = getBaseType(type);
        if (baseTypeName != null) {
            XmlSchemaComplexType baseType = (XmlSchemaComplexType)collection.getTypeByQName(baseTypeName);
            // recurse onto the base type ...
            results.addAll(getContentElements(baseType, collection));
            // and now process our sequence.
            XmlSchemaSequence extSequence = getContentSequence(type);
            if (extSequence != null) {
                for (XmlSchemaSequenceMember item : extSequence.getItems()) {
                    /*
                     * For now, leave the return type alone. Fix some day.
                     */
                    results.add((XmlSchemaObject)item);
                }
            }
            return results;
        } else {
            // no base type, the simple case.
            XmlSchemaSequence sequence = getSequence(type);
            for (XmlSchemaSequenceMember item : sequence.getItems()) {
                results.add((XmlSchemaObject)item);
            }
            return results;
        }
    }

    public static XmlSchemaSequence getContentSequence(XmlSchemaComplexType type) {
        XmlSchemaContentModel model = type.getContentModel();
        if (model == null) {
            return null;
        }
        XmlSchemaContent content = model.getContent();
        if (content == null) {
            return null;
        }
        if (!(content instanceof XmlSchemaComplexContentExtension)) {
            return null;
        }

        XmlSchemaComplexContentExtension ext = (XmlSchemaComplexContentExtension)content;
        XmlSchemaParticle particle = ext.getParticle();
        if (particle == null) {
            return null;
        }
        XmlSchemaSequence sequence = null;
        try {
            sequence = (XmlSchemaSequence) particle;
        } catch (ClassCastException cce) {
            unsupportedConstruct("NON_SEQUENCE_PARTICLE", type);
        }
        return sequence;
    }

    /**
     * By convention, an element that is named in its schema's TNS can have a 'name' but
     * no QName. This can get inconvenient for consumers who want to think about qualified names.
     * Unfortunately, XmlSchema elements, unlike types, don't store a reference to their containing
     * schema.
     * @param element
     * @param schema
     * @return
     */
    public static QName getElementQualifiedName(XmlSchemaElement element, XmlSchema schema) {
        if (element.getQName() != null) {
            return element.getQName();
        } else if (element.getName() != null) {
            return new QName(schema.getTargetNamespace(), element.getName());
        } else {
            return null;
        }
    }

    /**
     * Follow a chain of references from element to element until we can obtain
     * a type.
     *
     * @param element
     * @return
     */
    public static XmlSchemaType getElementType(SchemaCollection xmlSchemaCollection,
                                               String referencingURI,
                                               XmlSchemaElement element,
                                               XmlSchemaType containingType) {
        assert element != null;
        if (element.getSchemaTypeName() != null) {
            XmlSchemaType type = xmlSchemaCollection.getTypeByQName(element.getSchemaTypeName());
            if (type == null) {
                Message message = new Message("ELEMENT_TYPE_MISSING", LOG, element.getQName(),
                                              element.getSchemaTypeName().toString());
                throw new UnsupportedConstruct(message);
            }
            return type;
        }
        // The referencing URI only helps if there is a schema that points to
        // it.
        // It might be the URI for the wsdl TNS, which might have no schema.
        if (xmlSchemaCollection.getSchemaByTargetNamespace(referencingURI) == null) {
            referencingURI = null;
        }

        if (referencingURI == null && containingType != null) {
            referencingURI = containingType.getQName().getNamespaceURI();
        }

        XmlSchemaElement originalElement = element;
        while (element.getSchemaType() == null && element.isRef()) {
            /*
             * This code assumes that all schemas are in the collection.
             */
            XmlSchemaElement nextElement = element.getRef().getTarget();
            assert nextElement != null;
            element = nextElement;
        }
        if (element.getSchemaType() == null) {
            unsupportedConstruct("ELEMENT_HAS_NO_TYPE",
                                                originalElement.getName(),
                                                containingType.getQName(),
                                                containingType);
        }
        return element.getSchemaType();
    }

    /**
     * If the object is an attribute or an anyAttribute,
     * return the 'Annotated'. If it's not one of those, or it's a group,
     * throw. We're not ready for groups yet.
     * @param object
     * @return
     */
    public static XmlSchemaAnnotated getObjectAnnotated(XmlSchemaObject object, QName contextName) {

        if (!(object instanceof XmlSchemaAnnotated)) {
            unsupportedConstruct("NON_ANNOTATED_ATTRIBUTE",
                                                object.getClass().getSimpleName(),
                                                contextName, object);
        }
        if (!(object instanceof XmlSchemaAttribute)
            && !(object instanceof XmlSchemaAnyAttribute)) {
            unsupportedConstruct("EXOTIC_ATTRIBUTE",
                                                object.getClass().getSimpleName(), contextName,
                                                object);
        }

        return (XmlSchemaAnnotated) object;
    }

    /**
     * If the object is an element or an any, return the particle. If it's not a particle, or it's a group,
     * throw. We're not ready for groups yet.
     * @param object
     * @return
     */
    public static XmlSchemaParticle getObjectParticle(XmlSchemaObject object, QName contextName) {

        if (!(object instanceof XmlSchemaParticle)) {
            unsupportedConstruct("NON_PARTICLE_CHILD",
                                                object.getClass().getSimpleName(),
                                                contextName, object);
        }
        if (!(object instanceof XmlSchemaElement)
            && !(object instanceof XmlSchemaAny)) {
            unsupportedConstruct("GROUP_CHILD",
                                                object.getClass().getSimpleName(), contextName,
                                                object);
        }

        return (XmlSchemaParticle) object;
    }

    public static XmlSchemaElement getReferredElement(XmlSchemaElement element,
                                                      SchemaCollection xmlSchemaCollection) {
        if (element.isRef()) {
            /*
             * Calling getTarget works if everything is in the collection already.
             */
            XmlSchemaElement refElement = element.getRef().getTarget();
            if (refElement == null) {
                throw new RuntimeException("Dangling reference");
            }
            return refElement;
        }
        return null;
    }

    public static XmlSchemaSequence getSequence(XmlSchemaComplexType type) {
        XmlSchemaParticle particle = type.getParticle();
        XmlSchemaSequence sequence = null;

        if (particle == null) {
            // the code that uses this wants to iterate. An empty one is more useful than
            // a null pointer, and certainly an exception.
            return EMPTY_SEQUENCE;
        }

        try {
            sequence = (XmlSchemaSequence) particle;
        } catch (ClassCastException cce) {
            unsupportedConstruct("NON_SEQUENCE_PARTICLE", type);
        }

        return sequence;
    }
    public static XmlSchemaChoice getChoice(XmlSchemaComplexType type) {
        XmlSchemaParticle particle = type.getParticle();
        XmlSchemaChoice choice = null;

        if (particle == null) {
            // the code that uses this wants to iterate. An empty one is more useful than
            // a null pointer, and certainly an exception.
            return EMPTY_CHOICE;
        }

        try {
            choice = (XmlSchemaChoice) particle;
        } catch (ClassCastException cce) {
            unsupportedConstruct("NON_CHOICE_PARTICLE", type);
        }

        return choice;
    }
    public static XmlSchemaAll getAll(XmlSchemaComplexType type) {
        XmlSchemaParticle particle = type.getParticle();
        XmlSchemaAll all = null;

        if (particle == null) {
            // the code that uses this wants to iterate. An empty one is more useful than
            // a null pointer, and certainly an exception.
            return EMPTY_ALL;
        }

        try {
            all = (XmlSchemaAll) particle;
        } catch (ClassCastException cce) {
            unsupportedConstruct("NON_CHOICE_PARTICLE", type);
        }

        return all;
    }
    public static boolean isAttributeNameQualified(XmlSchemaAttribute attribute, XmlSchema schema) {
        if (attribute.isRef()) {
            throw new RuntimeException("isElementNameQualified on element with ref=");
        }
        if (attribute.getForm().equals(XmlSchemaForm.QUALIFIED)) {
            return true;
        }
        if (attribute.getForm().equals(XmlSchemaForm.UNQUALIFIED)) {
            return false;
        }
        return schema.getAttributeFormDefault().equals(XmlSchemaForm.QUALIFIED);
    }

    /**
     * due to a bug, feature, or just plain oddity of JAXB, it isn't good enough
     * to just check the form of an element and of its schema. If schema 'a'
     * (default unqualified) has a complex type with an element with a ref= to
     * schema (b) (default unqualified), JAXB seems to expect to see a
     * qualifier, anyway. <br/> So, if the element is local to a complex type,
     * all we care about is the default element form of the schema and the local
     * form of the element. <br/> If, on the other hand, the element is global,
     * we might need to compare namespaces. <br/>
     *
     * @param attribute the attribute
     * @param global if this element is a global element (complex type ref= to
     *                it, or in a part)
     * @param localSchema the schema of the complex type containing the
     *                reference, only used for the 'odd case'.
     * @param elementSchema the schema for the element.
     * @return if the element needs to be qualified.
     */
    public static boolean isAttributeQualified(XmlSchemaAttribute attribute,
                                             boolean global,
                                             XmlSchema localSchema,
                                             XmlSchema attributeSchema) {
        if (attribute.getQName() == null) {
            throw new RuntimeException("getSchemaQualifier on anonymous element.");
        }
        if (attribute.isRef()) {
            throw new RuntimeException("getSchemaQualified on the 'from' side of ref=.");
        }


        if (global) {
            return isAttributeNameQualified(attribute, attributeSchema)
                || (localSchema != null
                    && !(attribute.getQName().getNamespaceURI().equals(localSchema.getTargetNamespace())));
        } else {
            return isAttributeNameQualified(attribute, attributeSchema);
        }
    }

    public static boolean isComplexType(XmlSchemaType type) {
        return type instanceof XmlSchemaComplexType;
    }

    public static boolean isElementNameQualified(XmlSchemaElement element, XmlSchema schema) {
        if (element.isRef()) {
            throw new RuntimeException("isElementNameQualified on element with ref=");
        }
        if (element.getForm().equals(XmlSchemaForm.QUALIFIED)) {
            return true;
        }
        if (element.getForm().equals(XmlSchemaForm.UNQUALIFIED)) {
            return false;
        }
        return schema.getElementFormDefault().equals(XmlSchemaForm.QUALIFIED);
    }

    /**
     * due to a bug, feature, or just plain oddity of JAXB, it isn't good enough
     * to just check the form of an element and of its schema. If schema 'a'
     * (default unqualified) has a complex type with an element with a ref= to
     * schema (b) (default unqualified), JAXB seems to expect to see a
     * qualifier, anyway. <br/> So, if the element is local to a complex type,
     * all we care about is the default element form of the schema and the local
     * form of the element. <br/> If, on the other hand, the element is global,
     * we might need to compare namespaces. <br/>
     *
     * @param element the element.
     * @param global if this element is a global element (complex type ref= to
     *                it, or in a part)
     * @param localSchema the schema of the complex type containing the
     *                reference, only used for the 'odd case'.
     * @param elementSchema the schema for the element.
     * @return if the element needs to be qualified.
     */
    public static boolean isElementQualified(XmlSchemaElement element,
                                             boolean global,
                                             XmlSchema localSchema,
                                             XmlSchema elementSchema) {
        QName qn = getElementQualifiedName(element, localSchema);
        if (qn == null) {
            throw new RuntimeException("isElementQualified on anonymous element.");
        }
        if (element.isRef()) {
            throw new RuntimeException("isElementQualified on the 'from' side of ref=.");
        }


        if (global) {
            return isElementNameQualified(element, elementSchema)
                || (localSchema != null
                    && !(qn.getNamespaceURI().equals(localSchema.getTargetNamespace())));
        } else {
            return isElementNameQualified(element, elementSchema);
        }
    }

    public static boolean isParticleArray(XmlSchemaParticle particle) {
        return particle.getMaxOccurs() > 1;
    }

    public static boolean isParticleOptional(XmlSchemaParticle particle) {
        return particle.getMinOccurs() == 0 && particle.getMaxOccurs() == 1;
    }

    public static void unsupportedConstruct(String messageKey,
                                            String what,
                                            QName subjectName,
                                            XmlSchemaObject subject) {
        Message message = new Message(messageKey, LOG, what,
                                      subjectName == null ? "anonymous" : subjectName,
                                      cleanedUpSchemaSource(subject));
        LOG.severe(message.toString());
        throw new UnsupportedConstruct(message);

    }

    public static void unsupportedConstruct(String messageKey, XmlSchemaType subject) {
        Message message = new Message(messageKey, LOG, subject.getQName(),
                                      cleanedUpSchemaSource(subject));
        LOG.severe(message.toString());
        throw new UnsupportedConstruct(message);
    }

    public static String cleanedUpSchemaSource(XmlSchemaObject subject) {
        if (subject == null || subject.getSourceURI() == null) {
            return "";
        } else {
            return subject.getSourceURI() + ":" + subject.getLineNumber();
        }
    }
}
