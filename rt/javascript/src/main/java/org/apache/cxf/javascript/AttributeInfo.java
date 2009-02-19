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

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.cxf.common.xmlschema.XmlSchemaUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.XmlSchemaUse;
import org.apache.ws.commons.schema.constants.Constants.BlockConstants;

/**
 * All the information needed to create the JavaScript for an Xml Schema attribute
 * or xs:anyAttribute.
 */
public final class AttributeInfo implements ItemInfo {
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
    private XmlSchemaUse use;

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
     * Fill in an AttributeInfo for an attribute or anyAttribute from a sequence.
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
        XmlSchemaAnnotated realAnnotated = annotated;

        if (annotated instanceof XmlSchemaAttribute) {
            XmlSchemaAttribute attribute = (XmlSchemaAttribute)annotated;
            attributeInfo.use = attribute.getUse();
            
            if (attribute.getRefName() != null) {
                XmlSchemaAttribute refAttribute = schemaCollection
                    .getAttributeByQName(attribute.getRefName());
                if (refAttribute == null) {
                    throw new UnsupportedConstruct(LOG, "ATTRIBUTE_DANGLING_REFERENCE", attribute
                                                   .getQName(), attribute.getRefName()); 
                }
                realAnnotated = refAttribute;
                attributeInfo.global = true;
            }
        } else if (annotated instanceof XmlSchemaAnyAttribute) {
            attributeInfo.any = true;
            attributeInfo.xmlName = null; // unknown until runtime.
            attributeInfo.javascriptName = "any";
            attributeInfo.type = null; // runtime for any.
            attributeInfo.use = new XmlSchemaUse(BlockConstants.OPTIONAL);
        } else {
            throw new UnsupportedConstruct(LOG, "UNSUPPORTED_ATTRIBUTE_ITEM", annotated, contextName);
        }

        factoryCommon(realAnnotated, currentSchema, schemaCollection, prefixAccumulator, attributeInfo);

        attributeInfo.annotated = realAnnotated;

        return attributeInfo;
    }

    private static void factoryCommon(XmlSchemaAnnotated annotated, XmlSchema currentSchema,
                                      SchemaCollection schemaCollection,
                                      NamespacePrefixAccumulator prefixAccumulator, 
                                      AttributeInfo attributeInfo) {

        if (annotated instanceof XmlSchemaAttribute) {
            XmlSchemaAttribute attribute = (XmlSchemaAttribute)annotated;
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
            attributeInfo.use = attribute.getUse();
            factorySetupType(attribute, schemaCollection, attributeInfo);
        } else { // any
            attributeInfo.any = true;
            attributeInfo.xmlName = null; // unknown until runtime.
            attributeInfo.javascriptName = "any";
            attributeInfo.type = null; // runtime for any.
            attributeInfo.use = new XmlSchemaUse(BlockConstants.OPTIONAL);
        }
    }

    private static void factorySetupType(XmlSchemaAttribute element, SchemaCollection schemaCollection,
                                         AttributeInfo attributeInfo) {
        attributeInfo.type = element.getSchemaType();
        if (attributeInfo.type == null) {
            if (element.getSchemaTypeName().equals(XmlSchemaConstants.ANY_TYPE_QNAME)) {
                attributeInfo.anyType = true;
            } else {
                attributeInfo.type = schemaCollection.getTypeByQName(element.getSchemaTypeName());
                if (attributeInfo.type == null 
                    && !element.getSchemaTypeName()
                            .getNamespaceURI().equals(XmlSchemaConstants.XSD_NAMESPACE_URI)) {
                    XmlSchemaUtils.unsupportedConstruct("MISSING_TYPE", element.getSchemaTypeName()
                            .toString(), element.getQName(), element);
                }
            }
        } else if (attributeInfo.type.getQName() != null
            && XmlSchemaConstants.ANY_TYPE_QNAME.equals(attributeInfo.type.getQName())) {
            attributeInfo.anyType = true;
        }
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
        if (defaultValue == null && fixedValue != null) {
            return fixedValue;
        }
        return defaultValue;
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

    public boolean isArray() {
        return false;
    }

    public boolean isNillable() {
        return false;
    }

    public boolean isOptional() {
        return !use.equals(new XmlSchemaUse(BlockConstants.REQUIRED));
    }

    public void setDefaultValue(String value) {
        this.defaultValue = value;
    }
}
