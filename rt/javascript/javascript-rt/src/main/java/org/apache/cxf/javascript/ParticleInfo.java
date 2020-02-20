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

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.common.xmlschema.XmlSchemaUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaChoiceMember;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;

/**
 * All the information needed to create the JavaScript for an Xml Schema element
 * or xs:any.
 */
public final class ParticleInfo implements ItemInfo {
    private static final Logger LOG = LogUtils.getL7dLogger(ParticleInfo.class);
    private XmlSchemaParticle particle;
    private String javascriptName;
    private String xmlName;
    private XmlSchemaType containingType;
    // in the RPC case, we can have a type and no element.
    private XmlSchemaType type;
    private boolean empty;

    private boolean isGroup;
    private List<ParticleInfo> children;

    // These are exactly the same values as we find in the XmlSchemaElement.
    // there is no rationalization. But the accessors take care of business.
    private long minOccurs;
    private long maxOccurs;
    private boolean nillable;
    private boolean any;
    private boolean anyType;
    private String defaultValue;
    private boolean global;

    private ParticleInfo() {
    }

    /**
     * Create an elementInfo that stores information about a global, named,
     * element.
     *
     * @param element the element
     * @param currentSchema the schema it came from.
     * @param schemaCollection the collection of all schemas.
     * @param prefixAccumulator the accumulator that assigns prefixes.
     * @return
     */
    public static ParticleInfo forGlobalElement(XmlSchemaElement element, XmlSchema currentSchema,
                                                SchemaCollection schemaCollection,
                                                NamespacePrefixAccumulator prefixAccumulator) {
        ParticleInfo elementInfo = new ParticleInfo();
        elementInfo.particle = element;
        elementInfo.minOccurs = element.getMinOccurs();
        elementInfo.maxOccurs = element.getMaxOccurs();
        elementInfo.nillable = element.isNillable();
        elementInfo.global = true;

        factoryCommon(element, currentSchema, schemaCollection, prefixAccumulator, elementInfo);
        return elementInfo;
    }

    /**
     * Create element information for a part element. For a part, the JavaScript
     * and Element names are calculated in advance, and the element itself might
     * be null! In that case, the minOccurs and maxOccurs are conventional. Note
     * that in some cases the code in ServiceJavascriptBuilder uses a local
     * element (or xa:any) from inside the part element, instead of the part
     * element itself.
     *
     * @param element the element, or null
     * @param schemaCollection the schema collection, for resolving types.
     * @param javascriptName javascript variable name
     * @param xmlElementName xml element string
     * @return
     */
    public static ParticleInfo forPartElement(XmlSchemaElement element, SchemaCollection schemaCollection,
                                              String javascriptName, String xmlElementName) {
        ParticleInfo elementInfo = new ParticleInfo();
        elementInfo.particle = element;
        if (element == null) {
            elementInfo.minOccurs = 1;
            elementInfo.maxOccurs = 1;
        } else {
            elementInfo.minOccurs = element.getMinOccurs();
            elementInfo.maxOccurs = element.getMaxOccurs();
            elementInfo.nillable = element.isNillable();
            factorySetupType(element, schemaCollection, elementInfo);
        }
        elementInfo.javascriptName = javascriptName;
        elementInfo.xmlName = xmlElementName;
        elementInfo.global = true;

        return elementInfo;
    }

    /**
     * Fill in an ElementInfo for an element or xs:any from a sequence.
     *
     * @param sequenceObject
     * @param currentSchema
     * @param schemaCollection
     * @param prefixAccumulator
     * @param contextName
     * @return
     */
    public static ParticleInfo forLocalItem(XmlSchemaObject sequenceObject, XmlSchema currentSchema,
                                            SchemaCollection schemaCollection,
                                            NamespacePrefixAccumulator prefixAccumulator, QName contextName) {
        XmlSchemaParticle sequenceParticle =
            JavascriptUtils.getObjectParticle(sequenceObject, contextName, currentSchema);
        ParticleInfo elementInfo = new ParticleInfo();
        XmlSchemaParticle realParticle = sequenceParticle;

        elementInfo.setMinOccurs(sequenceParticle.getMinOccurs());
        elementInfo.setMaxOccurs(sequenceParticle.getMaxOccurs());

        if (sequenceParticle instanceof XmlSchemaElement) {
            XmlSchemaElement sequenceElement = (XmlSchemaElement)sequenceParticle;

            if (sequenceElement.getRef().getTargetQName() != null) {
                XmlSchemaElement refElement = sequenceElement.getRef().getTarget();
                if (refElement == null) {
                    Message message = new Message("ELEMENT_DANGLING_REFERENCE", LOG, sequenceElement
                        .getQName(), sequenceElement.getRef().getTargetQName());
                    throw new UnsupportedConstruct(message.toString());
                }
                realParticle = refElement;
                elementInfo.global = true;
            }
            elementInfo.nillable = ((XmlSchemaElement)realParticle).isNillable();
        } else if (sequenceParticle instanceof XmlSchemaChoice) {
            XmlSchemaChoice choice = (XmlSchemaChoice)sequenceParticle;

            if (sequenceParticle.getMaxOccurs() > 1) {
                Message message = new Message("GROUP_ELEMENT_MULTI_OCCURS", LOG, sequenceParticle
                        .getClass().getSimpleName());
                throw new UnsupportedConstruct(message.toString());
            }

            elementInfo.children = new LinkedList<>();

            List<XmlSchemaChoiceMember> items = choice.getItems();
            for (XmlSchemaChoiceMember item : items) {
                XmlSchemaObject schemaObject = JavascriptUtils.getObjectParticle((XmlSchemaObject)item, contextName,
                        currentSchema);
                ParticleInfo childParticle = ParticleInfo.forLocalItem(schemaObject, currentSchema, schemaCollection,
                        prefixAccumulator, contextName);

                if (childParticle.isAny()) {
                    Message message = new Message("GROUP_ELEMENT_ANY", LOG, sequenceParticle.getClass()
                            .getSimpleName());
                    throw new UnsupportedConstruct(message.toString());
                }

                childParticle.setMinOccurs(0);
                elementInfo.children.add(childParticle);
            }
        } else if (sequenceParticle instanceof XmlSchemaSequence) {
            XmlSchemaSequence nestedSequence = (XmlSchemaSequence)sequenceParticle;

            if (sequenceParticle.getMaxOccurs() > 1) {
                Message message = new Message("SEQUENCE_ELEMENT_MULTI_OCCURS", LOG, sequenceParticle
                        .getClass().getSimpleName());
                throw new UnsupportedConstruct(message.toString());
            }

            elementInfo.children = new LinkedList<>();

            List<XmlSchemaSequenceMember> items = nestedSequence.getItems();
            for (XmlSchemaSequenceMember item : items) {
                XmlSchemaObject schemaObject = JavascriptUtils.getObjectParticle((XmlSchemaObject)item, contextName,
                        currentSchema);
                ParticleInfo childParticle = ParticleInfo.forLocalItem(schemaObject, currentSchema, schemaCollection,
                        prefixAccumulator, contextName);

                if (childParticle.isAny()) {
                    Message message = new Message("SEQUENCE_ELEMENT_ANY", LOG, sequenceParticle.getClass()
                            .getSimpleName());
                    throw new UnsupportedConstruct(message.toString());
                }

                if (sequenceParticle.getMinOccurs() == 0) {
                    childParticle.setMinOccurs(0);
                }
                elementInfo.children.add(childParticle);
            }

        }

        factoryCommon(realParticle, currentSchema, schemaCollection, prefixAccumulator, elementInfo);

        elementInfo.particle = realParticle;

        return elementInfo;
    }

    private static void factoryCommon(XmlSchemaParticle particle, XmlSchema currentSchema,
                                      SchemaCollection schemaCollection,
                                      NamespacePrefixAccumulator prefixAccumulator,
                                      ParticleInfo elementInfo) {

        if (particle instanceof XmlSchemaElement) {
            XmlSchemaElement element = (XmlSchemaElement)particle;
            QName elementQName = XmlSchemaUtils.getElementQualifiedName(element, currentSchema);
            String elementNamespaceURI = elementQName.getNamespaceURI();

            boolean elementNoNamespace = "".equals(elementNamespaceURI);

            XmlSchema elementSchema = null;
            if (!elementNoNamespace) {
                elementSchema = schemaCollection.getSchemaByTargetNamespace(elementNamespaceURI);
                if (elementSchema == null) {
                    throw new RuntimeException("Missing schema " + elementNamespaceURI);
                }
            }

            boolean qualified = !elementNoNamespace
                                && XmlSchemaUtils.isElementQualified(element, true, currentSchema,
                                                                     elementSchema);
            elementInfo.xmlName = prefixAccumulator.xmlElementString(elementQName, qualified);
            // we are assuming here that we are not dealing, in close proximity,
            // with elements with identical local names and different
            // namespaces.
            elementInfo.javascriptName = elementQName.getLocalPart();
            String schemaDefaultValue = element.getDefaultValue();
            /*
             * Schema default values are carried as strings.
             * In javascript, for actual strings, we need quotes, but not for
             * numbers. The following is a trick.
             */
            schemaDefaultValue = protectDefaultValue(schemaDefaultValue);

            elementInfo.defaultValue = schemaDefaultValue;
            factorySetupType(element, schemaCollection, elementInfo);

            elementInfo.isGroup = false;
        } else if (particle instanceof XmlSchemaChoice) {
            elementInfo.isGroup = true;
        } else if (particle instanceof XmlSchemaSequence) {
            elementInfo.isGroup = true;
        } else { // any
            elementInfo.any = true;
            elementInfo.xmlName = null; // unknown until runtime.
            // TODO: multiple 'any'
            elementInfo.javascriptName = "any";
            elementInfo.type = null; // runtime for any.
            elementInfo.isGroup = false;
        }
    }

    private static String protectDefaultValue(String schemaDefaultValue) {
        if (schemaDefaultValue == null) {
            return null;
        }
        boolean leaveAlone = false;
        try {
            Long.parseLong(schemaDefaultValue);
            leaveAlone = true;
        } catch (NumberFormatException nfe) {
            try {
                Double.parseDouble(schemaDefaultValue);
                leaveAlone = true;
            } catch (NumberFormatException nfe2) {
                //
            }
        }
        if (!leaveAlone) {
            StringBuilder builder = new StringBuilder();
            builder.append('\'');
            for (char c : schemaDefaultValue.toCharArray()) {
                if (c == '\'') {
                    builder.append("\\'");
                } else if (c == '\\') {
                    builder.append("\\\\");
                } else {
                    builder.append(c);
                }
            }
            builder.append('\'');
            schemaDefaultValue = builder.toString();
        }
        return schemaDefaultValue;
    }

    private static void factorySetupType(XmlSchemaElement element, SchemaCollection schemaCollection,
                                         ParticleInfo elementInfo) {
        elementInfo.type = element.getSchemaType();
        if (elementInfo.type == null) {
            if (element.getSchemaTypeName() == null // no type at all -> anyType
                || element.getSchemaTypeName().equals(Constants.XSD_ANYTYPE)) {
                elementInfo.anyType = true;
            } else {
                elementInfo.type = schemaCollection.getTypeByQName(element.getSchemaTypeName());
                if (elementInfo.type == null
                    && !element.getSchemaTypeName()
                            .getNamespaceURI().equals(Constants.URI_2001_SCHEMA_XSD)) {
                    JavascriptUtils.unsupportedConstruct("MISSING_TYPE", element.getSchemaTypeName()
                            .toString(), element.getQName(), element);
                }
            }
        } else if (elementInfo.type.getQName() != null
            && Constants.XSD_ANYTYPE.equals(elementInfo.type.getQName())) {
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
        } else if (particle instanceof XmlSchemaElement) {
            XmlSchemaElement element = (XmlSchemaElement)particle;
            if (element.getQName() != null) {
                return element.getQName();
            }
        }
        Message message = new Message("IMPOSSIBLE_GLOBAL_ITEM", LOG, JavascriptUtils.cleanedUpSchemaSource(particle));
        LOG.severe(message.toString());
        throw new UnsupportedConstruct(message);
    }

    public XmlSchemaParticle getParticle() {
        return particle;
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

    public boolean isGroup() {
        return isGroup;
    }

    public List<ParticleInfo> getChildren() {
        return children;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public long getMinOccurs() {
        return minOccurs;
    }

    private void setMinOccurs(long value) {
        minOccurs = value;

        if (isGroup()) {
            for (ParticleInfo child : getChildren()) {
                child.setMinOccurs(value);
            }
        }
    }

    public long getMaxOccurs() {
        return maxOccurs;
    }

    private void setMaxOccurs(long value) {
        maxOccurs = value;

        if (isGroup()) {
            for (ParticleInfo child : getChildren()) {
                child.setMaxOccurs(value);
            }
        }
    }

    public boolean isArray() {
        return maxOccurs > 1;
    }

    public boolean isOptional() {
        return minOccurs == 0 && maxOccurs == 1;
    }

    /**
     * @return Returns the nillable flag for the element. False for 'xs:any'
     */
    public boolean isNillable() {
        return nillable;
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
        if (isOptional()) {
            return "null";
        }
        return defaultValue;
    }

    /**
     * @param defaultValue The defaultValue to set.
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * True if this describes a global, named, element.
     *
     * @return
     */
    public boolean isGlobal() {
        return global;
    }

    @Override
    public String toString() {
        return "ItemInfo: " + javascriptName;
    }
}
