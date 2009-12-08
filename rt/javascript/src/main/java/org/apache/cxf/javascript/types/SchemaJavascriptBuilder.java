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

package org.apache.cxf.javascript.types;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.common.xmlschema.XmlSchemaUtils;
import org.apache.cxf.javascript.AttributeInfo;
import org.apache.cxf.javascript.ItemInfo;
import org.apache.cxf.javascript.JavascriptUtils;
import org.apache.cxf.javascript.NameManager;
import org.apache.cxf.javascript.NamespacePrefixAccumulator;
import org.apache.cxf.javascript.ParticleInfo;
import org.apache.cxf.javascript.UnsupportedConstruct;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaObjectTable;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;

/**
 * Generate Javascript for a schema, and provide information needed for the
 * service builder. As of this pass, there is no support for non-sequence types
 * or for attribute mappings.
 */
public class SchemaJavascriptBuilder {

    private static final Logger LOG = LogUtils.getL7dLogger(SchemaJavascriptBuilder.class);
    private static int anyPrefixCounter;
    private SchemaCollection xmlSchemaCollection;
    private NameManager nameManager;
    private NamespacePrefixAccumulator prefixAccumulator;
    private SchemaInfo schemaInfo;

    // In general, I (bimargulies) hate fields that are temporary communications
    // between members of a class. However, given the style restrictions on the
    // number of parameters, it's the least of the evils.
    private StringBuilder code;
    private StringBuilder accessors;
    private JavascriptUtils utils;

    public SchemaJavascriptBuilder(SchemaCollection schemaCollection,
                                   NamespacePrefixAccumulator prefixAccumulator, NameManager nameManager) {
        this.xmlSchemaCollection = schemaCollection;
        this.nameManager = nameManager;
        this.prefixAccumulator = prefixAccumulator;
    }

    public String generateCodeForSchema(SchemaInfo schema) {
        schemaInfo = schema;
        code = new StringBuilder();
        code.append("//\n");
        code.append("// Definitions for schema: " + schema.getNamespaceURI());
        if (schema.getSystemId() != null) {
            code.append("\n//  " + schema.getSystemId());
        }
        code.append("\n//\n");

        XmlSchemaObjectTable schemaTypes = schema.getSchema().getSchemaTypes();
        Iterator namesIterator = schemaTypes.getNames();
        while (namesIterator.hasNext()) {
            QName name = (QName)namesIterator.next();
            XmlSchemaObject xmlSchemaObject = (XmlSchemaObject)schemaTypes.getItem(name);
            if (xmlSchemaObject instanceof XmlSchemaComplexType) {
                try {
                    XmlSchemaComplexType complexType = (XmlSchemaComplexType)xmlSchemaObject;
                    if (!JavascriptUtils.notVeryComplexType(complexType)
                        && complexType.getName() != null) {
                        complexTypeConstructorAndAccessors(complexType.getQName(), complexType);
                        complexTypeSerializerFunction(complexType.getQName(), complexType);
                        domDeserializerFunction(complexType.getQName(), complexType);
                    }
                } catch (UnsupportedConstruct usc) {
                    LOG.warning(usc.toString());
                    continue; // it could be empty, but the style checker
                    // would complain.
                }
            } else if (xmlSchemaObject instanceof XmlSchemaSimpleType) {
                XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType)xmlSchemaObject;
                if (XmlSchemaUtils.isEumeration(simpleType)) {
                    List<String> values = XmlSchemaUtils.enumeratorValues(simpleType);
                    code.append("//\n");
                    code.append("// Simple type (enumeration) " + simpleType.getQName() + "\n");
                    code.append("//\n");
                    for (String value : values) {
                        code.append("// - " + value + "\n");
                    }
                }
            }
        }

        // now add in global elements with anonymous types.
        schemaTypes = schema.getSchema().getElements();
        namesIterator = schemaTypes.getNames();
        while (namesIterator.hasNext()) {
            QName name = (QName)namesIterator.next();
            XmlSchemaObject xmlSchemaObject = (XmlSchemaObject)schemaTypes.getItem(name);
            if (xmlSchemaObject instanceof XmlSchemaElement) { // the
                // alternative
                // is too wierd
                // to
                // contemplate.
                try {
                    XmlSchemaElement element = (XmlSchemaElement)xmlSchemaObject;
                    if (element.getSchemaTypeName() == null && element.getSchemaType() == null) {
                        Message message = new Message("ELEMENT_MISSING_TYPE", LOG, element.getQName(),
                                                      element.getSchemaTypeName(), schema.getNamespaceURI());
                        LOG.warning(message.toString());
                        continue;
                    }
                    XmlSchemaType type;
                    if (element.getSchemaType() != null) {
                        type = element.getSchemaType();
                    } else {
                        type = schema.getSchema().getTypeByName(element.getSchemaTypeName());
                    }
                    if (!(type instanceof XmlSchemaComplexType)) {
                        // we never make classes for simple type.
                        continue;
                    }

                    XmlSchemaComplexType complexType = (XmlSchemaComplexType)type;
                    // for named types we don't bother to generate for the
                    // element.
                    if (!JavascriptUtils.notVeryComplexType(complexType)
                        && complexType.getName() == null) {
                        complexTypeConstructorAndAccessors(element.getQName(), complexType);
                        complexTypeSerializerFunction(element.getQName(), complexType);
                        domDeserializerFunction(element.getQName(), complexType);
                    }
                } catch (UnsupportedConstruct usc) {
                    continue; // it could be empty, but the style checker
                    // would complain.
                }
            }
        }

        String returnValue = code.toString();
        LOG.finer(returnValue);
        return returnValue;
    }

    // In general, I (bimargulies) hate fields that are temporary communications
    // between members of a class. However, given the style restrictions on the
    // number
    // of parameters, it's the least of the evils.

    public void complexTypeConstructorAndAccessors(QName name, XmlSchemaComplexType type) {
        accessors = new StringBuilder();
        utils = new JavascriptUtils(code);
        List<XmlSchemaObject> items = XmlSchemaUtils.getContentElements(type, xmlSchemaCollection);
        List<XmlSchemaAnnotated> attrs = XmlSchemaUtils.getContentAttributes(type, xmlSchemaCollection);

        final String elementPrefix = "this._";

        String typeObjectName = nameManager.getJavascriptName(name);
        code.append("//\n");
        code.append("// Constructor for XML Schema item " + name.toString() + "\n");
        code.append("//\n");
        code.append("function " + typeObjectName + " () {\n");
        // to assist in debugging we put a type property into every object.
        utils.appendLine("this.typeMarker = '" + typeObjectName + "';");
        for (XmlSchemaObject thing : items) {
            ParticleInfo itemInfo = ParticleInfo.forLocalItem(thing, 
                                                              schemaInfo.getSchema(),
                                                              xmlSchemaCollection, 
                                                              prefixAccumulator, 
                                                              type.getQName()); 
            constructOneItem(type, elementPrefix, typeObjectName, itemInfo);
        }
        
        for (XmlSchemaAnnotated thing : attrs) {
            AttributeInfo itemInfo = AttributeInfo.forLocalItem(thing, schemaInfo.getSchema(),
                                                                xmlSchemaCollection,
                                                                prefixAccumulator,
                                                                type.getQName());
            constructOneItem(type, elementPrefix, typeObjectName, itemInfo);
        }
        
        code.append("}\n\n");
        code.append(accessors.toString());
    }

    private void constructOneItem(XmlSchemaComplexType type, 
                                  final String elementPrefix, 
                                  String typeObjectName, 
                                  ItemInfo itemInfo) {
            
        String accessorSuffix = StringUtils.capitalize(itemInfo.getJavascriptName());

        String accessorName = typeObjectName + "_get" + accessorSuffix;
        String getFunctionProperty = typeObjectName + ".prototype.get" + accessorSuffix;
        String setFunctionProperty = typeObjectName + ".prototype.set" + accessorSuffix;
        accessors.append("//\n");
        accessors.append("// accessor is " + getFunctionProperty + "\n");
        accessors.append("// element get for " + itemInfo.getJavascriptName() + "\n");
        if (itemInfo.isAny()) {
            accessors.append("// - xs:any\n");
        } else {
            if (itemInfo.getType() != null) {
                accessors.append("// - element type is " + itemInfo.getType().getQName() + "\n");
            }
        }

        if (itemInfo.isOptional()) {
            accessors.append("// - optional element\n");
        } else {
            accessors.append("// - required element\n");

        }

        if (itemInfo.isArray()) {
            accessors.append("// - array\n");

        }

        if (itemInfo.isNillable()) {
            accessors.append("// - nillable\n");
        }

        accessors.append("//\n");
        accessors.append("// element set for " + itemInfo.getJavascriptName() + "\n");
        accessors.append("// setter function is is " + setFunctionProperty + "\n");
        accessors.append("//\n");
        accessors.append("function " + accessorName + "() { return this._"
                         + itemInfo.getJavascriptName() + ";}\n\n");
        accessors.append(getFunctionProperty + " = " + accessorName + ";\n\n");
        accessorName = typeObjectName + "_set" + accessorSuffix;
        accessors.append("function " + accessorName + "(value) { this._" 
                         + itemInfo.getJavascriptName()
                         + " = value;}\n\n");
        accessors.append(setFunctionProperty + " = " + accessorName + ";\n");

        if (itemInfo.isOptional() || (itemInfo.isNillable() && !itemInfo.isArray())) {
            utils.appendLine("this._" + itemInfo.getJavascriptName() + " = null;");
        } else if (itemInfo.isArray()) {
            utils.appendLine("this._" + itemInfo.getJavascriptName() + " = [];");
        } else if (itemInfo.isAny() || itemInfo.getType() instanceof XmlSchemaComplexType) {
            // even for required complex elements, we leave them null.
            // otherwise, we could end up in a cycle or otherwise miserable. The
            // application code is responsible for this.
            utils.appendLine("this._" + itemInfo.getJavascriptName() + " = null;");
        } else {

            if (itemInfo.getDefaultValue() == null) {
                itemInfo.setDefaultValue(utils.getDefaultValueForSimpleType(itemInfo.getType()));
            }
            utils.appendLine("this._" + itemInfo.getJavascriptName() + " = "
                             + itemInfo.getDefaultValue() + ";");
        }
    }

    /**
     * Produce a serializer function for a type. These functions emit the
     * surrounding element XML if the caller supplies an XML element name. It's
     * not quite as simple as that, though. The element name may need namespace
     * qualification, and this function will add more namespace prefixes as
     * needed.
     * 
     * @param type
     * @return
     */
    public void complexTypeSerializerFunction(QName name, XmlSchemaComplexType type) {

        StringBuilder bodyCode = new StringBuilder();
        JavascriptUtils bodyUtils = new JavascriptUtils(bodyCode);
        bodyUtils.setXmlStringAccumulator("xml");

        complexTypeSerializerBody(type, "this._", bodyUtils);

        utils = new JavascriptUtils(code);
        String functionName = nameManager.getJavascriptName(name) + "_" + "serialize";
        code.append("//\n");
        code.append("// Serialize " + name + "\n");
        code.append("//\n");
        code.append("function " + functionName + "(cxfjsutils, elementName, extraNamespaces) {\n");
        utils.startXmlStringAccumulator("xml");
        utils.startIf("elementName != null");
        utils.appendString("<");
        utils.appendExpression("elementName");
        // now add any accumulated namespaces.
        String moreNamespaces = prefixAccumulator.getAttributes();
        if (moreNamespaces.length() > 0) {
            utils.appendString(" ");
            utils.appendString(moreNamespaces);
        }
        utils.startIf("extraNamespaces");
        utils.appendExpression("' ' + extraNamespaces");
        utils.endBlock();
        // attributes
        complexTypeSerializeAttributes(type, "this._");
        utils.appendString(">");
        utils.endBlock();
        code.append(bodyCode);
        utils.startIf("elementName != null");
        utils.appendString("</");
        utils.appendExpression("elementName");
        utils.appendString(">");
        utils.endBlock();
        utils.appendLine("return xml;");
        code.append("}\n\n");
        code.append(nameManager.getJavascriptName(name) + ".prototype.serialize = " + functionName + ";\n\n");
    }

    private void complexTypeSerializeAttributes(XmlSchemaComplexType type, String string) {
        XmlSchemaObjectCollection attributes = type.getAttributes();
        for (int ax = 0; ax < attributes.getCount(); ax++) {
            @SuppressWarnings("unused") // work in progress.
            XmlSchemaAttribute attribute = (XmlSchemaAttribute)attributes.getItem(ax);
        }
    }

    /**
     * Build the serialization code for a complex type. At the top level, this
     * operates on single items, so it does not pay attention to minOccurs and
     * maxOccurs. However, as it works through the sequence, it manages optional
     * elements and arrays.
     * 
     * @param type
     * @param elementPrefix
     * @param bodyNamespaceURIs
     * @return
     */
    protected void complexTypeSerializerBody(XmlSchemaComplexType type, String elementPrefix,
                                             JavascriptUtils bodyUtils) {
        List<XmlSchemaObject> items = XmlSchemaUtils.getContentElements(type, xmlSchemaCollection);
        for (XmlSchemaObject sequenceItem : items) {
            ParticleInfo itemInfo = ParticleInfo.forLocalItem(sequenceItem, 
                                                              schemaInfo.getSchema(), 
                                                              xmlSchemaCollection, 
                                                              prefixAccumulator, 
                                                              type.getQName()); 
                
            // If the item is 'any', it could be ANY of our top-level elements.
            if (itemInfo.isAny()) {
                serializeAny(itemInfo, bodyUtils);
            } else {
                bodyUtils.generateCodeToSerializeElement(itemInfo, "this._", xmlSchemaCollection);
            }
        }
    }

    private void serializeAny(ParticleInfo itemInfo, JavascriptUtils bodyUtils) {
        String prefix = "cxfjsany" + anyPrefixCounter;
        anyPrefixCounter++;
        bodyUtils.generateCodeToSerializeAny(itemInfo, prefix, xmlSchemaCollection);
    }

    /**
     * Generate a JavaScript function that takes an element for a complex type
     * and walks through its children using them to fill in the values for a
     * JavaScript object.
     * 
     * @param type schema type for the process
     * @return the string contents of the JavaScript.
     */
    public void domDeserializerFunction(QName name, XmlSchemaComplexType type) {
        utils = new JavascriptUtils(code);
        
        List<XmlSchemaObject> contentElements = XmlSchemaUtils.getContentElements(type, xmlSchemaCollection);
        String typeObjectName = nameManager.getJavascriptName(name);
        code.append("function " + typeObjectName + "_deserialize (cxfjsutils, element) {\n");
        // create the object we are deserializing into.
        utils.appendLine("var newobject = new " + typeObjectName + "();");
        utils.appendLine("cxfjsutils.trace('element: ' + cxfjsutils.traceElementName(element));");
        utils.appendLine("var curElement = cxfjsutils.getFirstElementChild(element);");

        utils.appendLine("var item;");

        int nContentElements = contentElements.size();
        for (int i = 0; i < contentElements.size(); i++) {
            XmlSchemaObject contentElement = contentElements.get(i);
            utils.appendLine("cxfjsutils.trace('curElement: ' + cxfjsutils.traceElementName(curElement));");
            ParticleInfo itemInfo = ParticleInfo.forLocalItem(contentElement,
                                                              schemaInfo.getSchema(),
                                                              xmlSchemaCollection, 
                                                              prefixAccumulator, 
                                                              type.getQName()); 
            if (itemInfo.isAny()) {
                ParticleInfo nextItem = null;
                if (i != nContentElements - 1) {
                    XmlSchemaObject nextThing = contentElements.get(i + 1);
                    nextItem = ParticleInfo.forLocalItem(nextThing, 
                                                         schemaInfo.getSchema(), 
                                                         xmlSchemaCollection, 
                                                         prefixAccumulator, 
                                                         type.getQName()); 
                    // theoretically, you could have two anys with different
                    // namespaces.
                    if (nextItem.isAny()) {
                        unsupportedConstruct("MULTIPLE_ANY", type.getQName());
                    }
                }
                deserializeAny(type, itemInfo, nextItem);
            } else {
                deserializeElement(type, contentElement);
            }
        }
        utils.appendLine("return newobject;");
        code.append("}\n\n");
    }
    
    private String buildNamespaceList(String anyNamespaceSpec) {
        StringBuilder nslist = new StringBuilder();
        String[] namespaces = anyNamespaceSpec.split("\\s");
        nslist.append("[ ");
        for (int x = 0; x < namespaces.length; x++) {
            String ns = namespaces[x];
            nslist.append("'");
            if ("##targetNamespace".equals(ns)) {
                nslist.append(schemaInfo.getNamespaceURI());
            } else if ("##local".equals(ns)) {
                // nothing, empty string
            } else {
                nslist.append(ns);
            }
            nslist.append("'");
            if (x < namespaces.length - 1) {
                nslist.append(",");
            }
        }
        nslist.append("]");
        return nslist.toString();
    }

    private void deserializeAny(XmlSchemaComplexType type, 
                                ParticleInfo itemInfo, 
                                ParticleInfo nextItem) {
        XmlSchemaAny any = (XmlSchemaAny)itemInfo.getParticle();
        
        boolean array = XmlSchemaUtils.isParticleArray(any);
        boolean optional = XmlSchemaUtils.isParticleOptional(any);
        
        if (array) {
            utils.appendLine("var anyObject = [];");
        } else {
            utils.appendLine("var anyObject = null;");

        }

        String anyNamespaceSpec = any.getNamespace();
        // we aren't dealing with any-after-any.
        XmlSchemaElement nextElement = null;
        if (nextItem != null) {
            nextElement = (XmlSchemaElement)nextItem.getParticle();
        }
        String matchType;
        String namespaceList = "[]";
        
        if (anyNamespaceSpec == null 
            || "##any".equals(anyNamespaceSpec) 
            || "".equals(anyNamespaceSpec)) {
            matchType = "org_apache_cxf_any_ns_matcher.ANY";
        } else if ("##other".equals(anyNamespaceSpec)) {
            matchType = "org_apache_cxf_any_ns_matcher.OTHER";
        } else if ("##local".equals(anyNamespaceSpec)) {
            matchType = "org_apache_cxf_any_ns_matcher.LOCAL";
        } else {
            matchType = "org_apache_cxf_any_ns_matcher.LISTED";
            namespaceList = buildNamespaceList(anyNamespaceSpec);
        }
        
        String nextLocalPartConstant = "null";
        if (nextElement != null) {
            nextLocalPartConstant = "'" + nextElement.getQName().getLocalPart() + "'";
        }
        
        utils.appendLine("var matcher = new org_apache_cxf_any_ns_matcher("
                         + matchType
                         + ", '" + schemaInfo.getNamespaceURI() + "'"
                         + ", " + namespaceList
                         + ", " + nextLocalPartConstant
                         + ");");
        
        if (array) {
            utils.appendLine("var anyNeeded = " + any.getMinOccurs() + ";");
            utils.appendLine("var anyAllowed = " + any.getMaxOccurs() + ";");
        } else if (optional) {
            utils.appendLine("var anyNeeded = 0;");
            utils.appendLine("var anyAllowed = 1;");
        } else {
            utils.appendLine("var anyNeeded = 1;");
            utils.appendLine("var anyAllowed = 1;");
        }
        
        utils.startWhile("anyNeeded > 0 || anyAllowed > 0");

        utils.appendLine("var anyURI;");
        utils.appendLine("var anyLocalPart;");
        utils.appendLine("var anyMatched = false;");
        utils.startIf("curElement");
        
        utils.appendLine("anyURI = cxfjsutils.getElementNamespaceURI(curElement);");
        utils.appendLine("anyLocalPart = cxfjsutils.getNodeLocalName(curElement);");
        utils.appendLine("var anyQName = '{' + anyURI + '}' + anyLocalPart;");
        utils.appendLine("cxfjsutils.trace('any match: ' + anyQName);"); 
        utils.appendLine("anyMatched = matcher.match(anyURI, anyLocalPart)");
        utils.appendLine("cxfjsutils.trace(' --> ' + anyMatched);"); 

        utils.endBlock(); // curElement != null
        
        utils.startIf("anyMatched"); // if match
        utils.appendLine("anyDeserializer = "
                         + "cxfjsutils.interfaceObject.globalElementDeserializers[anyQName];");
        utils.appendLine("cxfjsutils.trace(' deserializer: ' + anyDeserializer);");
        utils.startIf("anyDeserializer"); // if complex/serializer function
        utils.appendLine("var anyValue = anyDeserializer(cxfjsutils, curElement);");
        utils.appendElse(); // else complex/serializer function
        // TODO: for simple types we really need a dictionary of the simple type qnames.
        utils.appendLine("var anyValue = curElement.nodeValue;");
        utils.endBlock(); // complex/serializer function

        if (array) {
            utils.appendLine("anyObject.push(anyValue);");
        } else {
            utils.appendLine("anyObject = anyValue;");
        }
        
        utils.appendLine("anyNeeded--;");
        utils.appendLine("anyAllowed--;");
        // if we consumed the element, we advance. 
        utils.appendLine("curElement = cxfjsutils.getNextElementSibling(curElement);");
        utils.appendElse(); // match
        // non-matching case
        utils.startIf("anyNeeded > 0");
        utils.appendLine("throw 'not enough ws:any elements';");
        utils.endBlock(); // non-match+required
        utils.endBlock(); // match/non-match.
        utils.endBlock(); // while

        utils.appendLine("var anyHolder = new org_apache_cxf_any_holder(anyURI, anyLocalPart, anyValue);");
        utils.appendLine("newobject.setAny(anyHolder);");
    }

    private void deserializeElement(XmlSchemaComplexType type, XmlSchemaObject thing) {
        ParticleInfo itemInfo = ParticleInfo.forLocalItem(thing, 
                                                          schemaInfo.getSchema(),
                                                          xmlSchemaCollection, 
                                                          prefixAccumulator, 
                                                          type.getQName());
        XmlSchemaType itemType = itemInfo.getType();
        boolean simple = itemType instanceof XmlSchemaSimpleType
            || JavascriptUtils.notVeryComplexType(itemType);
        boolean mtomCandidate = JavascriptUtils.mtomCandidateType(itemType);
        String accessorName = "set" + StringUtils.capitalize(itemInfo.getJavascriptName());
        utils.appendLine("cxfjsutils.trace('processing " + itemInfo.getJavascriptName() + "');");
        XmlSchemaElement element = (XmlSchemaElement) itemInfo.getParticle();
        QName elementQName = XmlSchemaUtils.getElementQualifiedName(element, schemaInfo.getSchema()); 
        String elementNamespaceURI = elementQName.getNamespaceURI();
        boolean elementNoNamespace = "".equals(elementNamespaceURI);
        XmlSchema elementSchema = null;
        if (!elementNoNamespace) {
            elementSchema = xmlSchemaCollection.getSchemaByTargetNamespace(elementNamespaceURI);
        }
        boolean qualified = !elementNoNamespace
                            && XmlSchemaUtils.isElementQualified(element, 
                                                                 itemInfo.isGlobal(), 
                                                                 schemaInfo.getSchema(),
                                                                 elementSchema);

        if (!qualified) {
            elementNamespaceURI = "";
        }
        
        String localName = elementQName.getLocalPart();
        String valueTarget = "item";

        if (itemInfo.isOptional() || itemInfo.isArray()) {
            utils.startIf("curElement != null && cxfjsutils.isNodeNamedNS(curElement, '"
                          + elementNamespaceURI + "', '" + localName + "')");
            if (itemInfo.isArray()) {
                utils.appendLine("item = [];");
                utils.startDo();
                valueTarget = "arrayItem";
                utils.appendLine("var arrayItem;");
            }
        }

        utils.appendLine("var value = null;");
        utils.startIf("!cxfjsutils.isElementNil(curElement)");
        if (itemInfo.isAnyType()) {
            // use our utility
            utils.appendLine(valueTarget + " = org_apache_cxf_deserialize_anyType(cxfjsutils, curElement);");
        } else if (simple) {
            if (mtomCandidate) {
                utils.appendLine(valueTarget + " = cxfjsutils.deserializeBase64orMom(curElement);");
            } else {
                utils.appendLine("value = cxfjsutils.getNodeText(curElement);");
                utils.appendLine(valueTarget 
                                 + " = " 
                                 + utils.javascriptParseExpression(itemType, "value") + ";");
            }
        } else {
            XmlSchemaComplexType complexType = (XmlSchemaComplexType)itemType;
            QName baseQName = complexType.getQName();
            if (baseQName == null) {
                baseQName = element.getQName();
            }

            String elTypeJsName = nameManager.getJavascriptName(baseQName);
            utils.appendLine(valueTarget 
                             + " = " 
                             + elTypeJsName 
                             + "_deserialize(cxfjsutils, curElement);");
        }

        utils.endBlock(); // the if for the nil.
        if (itemInfo.isArray()) {
            utils.appendLine("item.push(arrayItem);");
            utils.appendLine("curElement = cxfjsutils.getNextElementSibling(curElement);");
            utils.endBlock();
            utils.appendLine("  while(curElement != null && cxfjsutils.isNodeNamedNS(curElement, '"
                             + elementNamespaceURI + "', '" 
                             + localName + "'));");
        }
        utils.appendLine("newobject." + accessorName + "(item);");
        if (!itemInfo.isArray()) {
            utils.startIf("curElement != null");
            utils.appendLine("curElement = cxfjsutils.getNextElementSibling(curElement);");
            utils.endBlock();
        }
        if (itemInfo.isOptional() || itemInfo.isArray()) {
            utils.endBlock();
        }
    }

    private void unsupportedConstruct(String messageKey, Object... args) {
        Message message = new Message(messageKey, LOG, args);
        throw new UnsupportedConstruct(message);
    }
}
