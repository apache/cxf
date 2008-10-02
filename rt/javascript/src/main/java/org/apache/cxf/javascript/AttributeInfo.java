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

package org.apache.cxf.javascript;

import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaType;

/**
 * All the information needed to create the JavaScript for an Xml Schema attribute
 * or xs:anyAttribute.
 */
public final class AttributeInfo {
    private static final Logger LOG = LogUtils.getL7dLogger(AttributeInfo.class);
    private XmlSchemaAnnotated annotated;
    private String javascriptName;
    private String xmlName;
    private XmlSchemaType containingType;
    private XmlSchemaType type;
    private boolean any;
    private boolean anyType;
    private String defaultValue;
    private String fixedValue;
    private boolean global;

    private AttributeInfo() {
    }

    /**
     * Create an elementInfo that stores information about a global, named,
     * element.
     * 
     * @param attribute the element
     * @param currentSchema the schema it came from.
     * @param schemaCollection the collection of all schemas.
     * @param prefixAccumulator the accumulator that assigns prefixes.
     * @return
     */
    public static AttributeInfo forGlobalAttribute(XmlSchemaAttribute attribute, XmlSchema currentSchema,
                                                   SchemaCollection schemaCollection,
                                                   NamespacePrefixAccumulator prefixAccumulator) {
        AttributeInfo attributeInfo = new AttributeInfo();
        attributeInfo.annotated = attribute;
        attributeInfo.global = true;

        factoryCommon(attribute, currentSchema, schemaCollection, prefixAccumulator, attributeInfo);
        return attributeInfo;
    }

    /**
     * Fill in an ElementInfo for an element or xs:any from a sequence.
     * 
     * @param sequenceElement
     * @param currentSchema
     * @param schemaCollection
     * @param prefixAccumulator
     * @return
     */
    public static AttributeInfo forLocalItem(XmlSchemaObject sequenceObject, 
                                             XmlSchema currentSchema,
                                            SchemaCollection schemaCollection,
                                            NamespacePrefixAccumulator prefixAccumulator, QName contextName) {
        XmlSchemaAnnotated annotated = XmlSchemaUtils.getObjectAnnotated(sequenceObject, contextName);
        AttributeInfo attributeInfo = new AttributeInfo();
        XmlSchemaAnnotated realParticle = annotated;

        if (annotated instanceof XmlSchemaAttribute) {
            XmlSchemaAttribute attribute = (XmlSchemaAttribute)annotated;

            if (attribute.getRefName() != null) {
                XmlSchemaAttribute refElement = schemaCollection
                    .getAttributeByQName(attribute.getRefName());
                if (refElement == null) {
                    Message message = new Message("ATTRIBUTE_DANGLING_REFERENCE", LOG, attribute
                        .getQName(), attribute.getRefName());
                    throw new UnsupportedConstruct(message.toString());
                }
                realParticle = refElement;
                attributeInfo.global = true;
            }
        }

        factoryCommon(realParticle, currentSchema, schemaCollection, prefixAccumulator, attributeInfo);

        attributeInfo.annotated = realParticle;

        return attributeInfo;
    }

    private static void factoryCommon(XmlSchemaAnnotated particle, XmlSchema currentSchema,
                                      SchemaCollection schemaCollection,
                                      NamespacePrefixAccumulator prefixAccumulator, 
                                      AttributeInfo attributeInfo) {

        if (particle instanceof XmlSchemaAttribute) {
            XmlSchemaAttribute attribute = (XmlSchemaAttribute)particle;
            String attributeNamespaceURI = attribute.getQName().getNamespaceURI();
            boolean attributeNoNamespace = "".equals(attributeNamespaceURI);

            XmlSchema attributeSchema = null;
            if (!attributeNoNamespace) {
                attributeSchema = schemaCollection.getSchemaByTargetNamespace(attributeNamespaceURI);
                if (attributeSchema == null) {
                    throw new RuntimeException("Missing schema " + attributeNamespaceURI);
                }
            }

            boolean qualified = !attributeNoNamespace
                                && XmlSchemaUtils.isAttributeQualified(attribute, true, currentSchema,
                                                                     attributeSchema);
            attributeInfo.xmlName = prefixAccumulator.xmlAttributeString(attribute, qualified);
            // we are assuming here that we are not dealing, in close proximity,
            // with elements with identical local names and different
            // namespaces.
            attributeInfo.javascriptName = attribute.getQName().getLocalPart();
            attributeInfo.defaultValue = attribute.getDefaultValue();
            attributeInfo.fixedValue = attribute.getFixedValue();
            factorySetupType(attribute, schemaCollection, attributeInfo);
        } else { // any
            attributeInfo.any = true;
            attributeInfo.xmlName = null; // unknown until runtime.
            // TODO: multiple 'any'
            attributeInfo.javascriptName = "any";
            attributeInfo.type = null; // runtime for any.

        }
    }

    private static void factorySetupType(XmlSchemaAttribute element, SchemaCollection schemaCollection,
                                         AttributeInfo elementInfo) {
        elementInfo.type = element.getSchemaType();
        if (elementInfo.type == null) {
            if (element.getSchemaTypeName().equals(XmlSchemaConstants.ANY_TYPE_QNAME)) {
                elementInfo.anyType = true;
            } else {
                elementInfo.type = schemaCollection.getTypeByQName(element.getSchemaTypeName());
                if (elementInfo.type == null 
                    && !element.getSchemaTypeName()
                            .getNamespaceURI().equals(XmlSchemaConstants.XSD_NAMESPACE_URI)) {
                    XmlSchemaUtils.unsupportedConstruct("MISSING_TYPE", element.getSchemaTypeName()
                            .toString(), element.getQName(), element);
                }
            }
        } else if (elementInfo.type.getQName() != null
            && XmlSchemaConstants.ANY_TYPE_QNAME.equals(elementInfo.type.getQName())) {
            elementInfo.anyType = true;
        }
    }

    /**
     * As a general rule, the JavaScript code is organized by types. The
     * exception is global elements that have anonymous types. In those cases,
     * the JavaScript code has its functions named according to the element.
     * This method returns the QName for the type or element, accordingly. If a
     * schema has a local element with an anonymous, complex, type, this will
     * throw. This will need to be fixed.
     * 
     * @return the qname.
     */
    public QName getControllingName() {
        if (type != null && type.getQName() != null) {
            return type.getQName();
        } else if (annotated instanceof XmlSchemaElement) {
            XmlSchemaElement element = (XmlSchemaElement)annotated;
            if (element.getQName() != null) {
                return element.getQName();
            }
        }
        Message message = new Message("IMPOSSIBLE_GLOBAL_ITEM", LOG, XmlSchemaUtils
            .cleanedUpSchemaSource(annotated));
        LOG.severe(message.toString());
        throw new UnsupportedConstruct(message);
    }

    /**
     * Return the object for the Attribute or the anyAttribute.
     * @return
     */
    public XmlSchemaAnnotated getAnnotated() {
        return annotated;
    }

    public String getJavascriptName() {
        return javascriptName;
    }

    public void setJavascriptName(String name) {
        javascriptName = name;
    }

    public String getXmlName() {
        return xmlName;
    }

    public void setXmlName(String elementXmlName) {
        this.xmlName = elementXmlName;
    }

    public XmlSchemaType getContainingType() {
        return containingType;
    }

    public void setContainingType(XmlSchemaType containingType) {
        this.containingType = containingType;
    }

    public XmlSchemaType getType() {
        return type;
    }

    public void setType(XmlSchemaType type) {
        this.type = type;
    }

    public boolean isAny() {
        return any;
    }

    public boolean isAnyType() {
        return anyType;
    }

    /**
     * *
     * 
     * @return Returns the defaultValue.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @param defaultValue The defaultValue to set.
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * True if this describes a global, named, attribute.
     * 
     * @return
     */
    public boolean isGlobal() {
        return global;
    }

    public String getFixedValue() {
        return fixedValue;
    }

    public void setFixedValue(String fixedValue) {
        this.fixedValue = fixedValue;
    }
}
