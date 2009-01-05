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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;

import org.apache.cxf.aegis.type.mtom.AbstractXOPType;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.cxf.common.xmlschema.XmlSchemaUtils;
import org.apache.cxf.databinding.source.mime.MimeAttribute;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;

/**
 * A set of functions that assist in JavaScript generation. This includes
 * functions for appending strings of JavaScript to a buffer as well as some
 * type utilities.
 */
public class JavascriptUtils {
    private static final String NL = "\n";
    private static int anyTypePrefixCounter;
    private StringBuilder code;
    private Stack<String> prefixStack;
    private String xmlStringAccumulatorVariable;
    private Map<String, String> defaultValueForSimpleType;
    private Set<String> nonStringSimpleTypes;
    private Set<String> intTypes;
    private Set<String> floatTypes;

    public JavascriptUtils(StringBuilder code) {
        this.code = code;
        defaultValueForSimpleType = new HashMap<String, String>();
        defaultValueForSimpleType.put("int", "0");
        defaultValueForSimpleType.put("unsignedInt", "0");
        defaultValueForSimpleType.put("long", "0");
        defaultValueForSimpleType.put("unsignedLong", "0");
        defaultValueForSimpleType.put("float", "0.0");
        defaultValueForSimpleType.put("double", "0.0");
        nonStringSimpleTypes = new HashSet<String>();
        nonStringSimpleTypes.add("int");
        nonStringSimpleTypes.add("long");
        nonStringSimpleTypes.add("unsignedInt");
        nonStringSimpleTypes.add("unsignedLong");
        nonStringSimpleTypes.add("float");
        nonStringSimpleTypes.add("double");

        intTypes = new HashSet<String>();
        intTypes.add("int");
        intTypes.add("long");
        intTypes.add("unsignedInt");
        intTypes.add("unsignedLong");
        floatTypes = new HashSet<String>();
        floatTypes.add("float");
        floatTypes.add("double");

        prefixStack = new Stack<String>();
        prefixStack.push("    ");
    }

    public String getDefaultValueForSimpleType(XmlSchemaType type) {
        String val = defaultValueForSimpleType.get(type.getName());
        if (val == null) { // ints and such return the appropriate 0.
            return "''";
        } else {
            return val;
        }
    }

    public boolean isStringSimpleType(QName typeName) {
        return !(WSDLConstants.NS_SCHEMA_XSD.equals(typeName.getNamespaceURI()) && nonStringSimpleTypes
            .contains(typeName.getLocalPart()));
    }

    public void setXmlStringAccumulator(String variableName) {
        xmlStringAccumulatorVariable = variableName;
    }

    public void startXmlStringAccumulator(String variableName) {
        xmlStringAccumulatorVariable = variableName;
        code.append(prefix());
        code.append("var ");
        code.append(variableName);
        code.append(" = '';" + NL);
    }

    public static String protectSingleQuotes(String value) {
        return value.replaceAll("'", "\\'");
    }

    public String escapeStringQuotes(String data) {
        return data.replace("'", "\\'");
    }

    /**
     * emit javascript to append a value to the accumulator.
     * 
     * @param value
     */
    public void appendString(String value) {
        code.append(prefix());
        code.append(xmlStringAccumulatorVariable + " = " + xmlStringAccumulatorVariable + " + '");
        code.append(escapeStringQuotes(value));
        code.append("';" + NL);
    }

    public void appendExpression(String value) {
        code.append(prefix());
        code.append(xmlStringAccumulatorVariable + " = " + xmlStringAccumulatorVariable + " + ");
        code.append(value);
        code.append(";" + NL);
    }

    private String prefix() {
        return prefixStack.peek();
    }

    public void appendLine(String line) {
        code.append(prefix());
        code.append(line);
        code.append(NL);
    }

    public void startIf(String test) {
        code.append(prefix());
        code.append("if (" + test + ") {" + NL);
        prefixStack.push(prefix() + " ");
    }

    public void startBlock() {
        code.append(prefix());
        code.append("{" + NL);
        prefixStack.push(prefix() + " ");
    }

    public void appendElse() {
        prefixStack.pop();
        code.append(prefix());
        code.append("} else {" + NL);
        prefixStack.push(prefix() + " ");
    }

    public void endBlock() {
        prefixStack.pop();
        code.append(prefix());
        code.append("}" + NL);
    }

    public void startFor(String start, String test, String increment) {
        code.append(prefix());
        code.append("for (" + start + ";" + test + ";" + increment + ") {" + NL);
        prefixStack.push(prefix() + " ");
    }

    public void startForIn(String var, String collection) {
        code.append(prefix());
        code.append("for (var " + var + " in " + collection + ") {" + NL);
        prefixStack.push(prefix() + " ");
    }

    public void startWhile(String test) {
        code.append(prefix());
        code.append("while (" + test + ") {" + NL);
        prefixStack.push(prefix() + " ");
    }

    public void startDo() {
        code.append(prefix());
        code.append("do  {" + NL);
        prefixStack.push(prefix() + " ");
    }

    // Given a js variable and a simple type object, correctly set the variables
    // simple type
    public String javascriptParseExpression(XmlSchemaType type, String value) {
        if (!(type instanceof XmlSchemaSimpleType)) {
            return value;
        }
        String name = type.getName();
        if (intTypes.contains(name)) {
            return "parseInt(" + value + ")";
        } else if (floatTypes.contains(name)) {
            return "parseFloat(" + value + ")";
        } else if ("boolean".equals(name)) {
            return "(" + value + " == 'true')";
        } else {
            return value;
        }
    }

    public static String javaScriptNameToken(String token) {
        return token;
    }
    
    /**
     * We really don't want to take the attitude that 'all base64Binary elements are candidates for MTOM'.
     * So we look for clues.
     * @param schemaObject
     * @return
     */
    private boolean treatAsMtom(XmlSchemaObject schemaObject) {
        if (schemaObject == null) {
            return false;
        }
        
        Map metaInfoMap = schemaObject.getMetaInfoMap();
        if (metaInfoMap != null) {
            Map attribMap = (Map)metaInfoMap.get(Constants.MetaDataConstants.EXTERNAL_ATTRIBUTES);
            Attr ctAttr = (Attr)attribMap.get(MimeAttribute.MIME_QNAME);
            if (ctAttr != null) {
                return true;
            }
        }
        
        if (schemaObject instanceof XmlSchemaElement) {
            XmlSchemaElement element = (XmlSchemaElement) schemaObject;
            if (element.getSchemaType() == null) {
                return false;
            }
            QName typeName = element.getSchemaType().getQName();
            // We could do something much more complex in terms of evaluating whether the type
            // permits the contentType attribute. This, however, is enough to clue us in for what Aegis
            // does.
            if (AbstractXOPType.XML_MIME_BASE64.equals(typeName)) {
                return true;
            }
            
        }
        
        return false;
    }
    
    /**
     * We don't want to generate Javascript overhead for complex types with simple content models,
     * at least until or unless we decide to cope with attributes in a general way.
     * @param type
     * @return
     */
    public static boolean notVeryComplexType(XmlSchemaType type) {
        return type instanceof XmlSchemaSimpleType 
               || (type instanceof XmlSchemaComplexType 
                   && ((XmlSchemaComplexType)type).getContentModel() instanceof XmlSchemaSimpleContent);
    }

    /**
     * Return true for xsd:base64Binary or simple restrictions of it, as in the xmime stock type.
     * @param type
     * @return
     */
    public static boolean mtomCandidateType(XmlSchemaType type) {
        if (type == null) {
            return false;
        }
        if (XmlSchemaConstants.BASE64BINARY_QNAME.equals(type.getQName())) {
            return true;
        }
        // there could be some disagreement whether the following is a good enough test.
        // what if 'base64binary' was extended in some crazy way? At runtime, either it has
        // an xop:Include or it doesn't.
        if (type instanceof XmlSchemaComplexType) {
            XmlSchemaComplexType complexType = (XmlSchemaComplexType)type; 
            if (complexType.getContentModel() instanceof XmlSchemaSimpleContent) {
                XmlSchemaSimpleContent content = (XmlSchemaSimpleContent)complexType.getContentModel();
                if (content.getContent() instanceof XmlSchemaSimpleContentExtension) {
                    XmlSchemaSimpleContentExtension extension = 
                        (XmlSchemaSimpleContentExtension)content.getContent();
                    if (XmlSchemaConstants.BASE64BINARY_QNAME.equals(extension.getBaseTypeName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Given an element, generate the serialization code.
     * 
     * @param elementInfo description of the element we are serializing
     * @param referencePrefix prefix to the Javascript variable. Nothing for
     *                args, this._ for members.
     * @param schemaCollection caller's schema collection.
     */
    public void generateCodeToSerializeElement(ParticleInfo elementInfo,
                                               String referencePrefix,
                                               SchemaCollection schemaCollection) {
        XmlSchemaType type = elementInfo.getType();
        boolean nillable = elementInfo.isNillable();
        boolean optional = elementInfo.isOptional();
        boolean array = elementInfo.isArray();
        boolean mtom = treatAsMtom(elementInfo.getParticle());
        String jsVar = referencePrefix + elementInfo.getJavascriptName();
        appendLine("// block for local variables");
        startBlock(); // allow local variables.
        // first question: optional?
        if (optional) {
            startIf(jsVar + " != null");
        } 
        
        // nillable and optional would be very strange together.
        // and nillable in the array case applies to the elements.
        if (nillable && !array) {
            startIf(jsVar + " == null");
            appendString("<" + elementInfo.getXmlName() + " " + XmlSchemaUtils.XSI_NIL + "/>");
            appendElse();
        }
        
        if (array) {
            // protected against null in arrays.
            startIf(jsVar + " != null");
            startFor("var ax = 0", "ax < " +  jsVar + ".length", "ax ++");
            jsVar = jsVar + "[ax]";
            // we need an extra level of 'nil' testing here. Or do we, depending
            // on the type structure?
            // Recode and fiddle appropriately.
            startIf(jsVar + " == null");
            if (nillable) {
                appendString("<" + elementInfo.getXmlName() 
                             + " " + XmlSchemaUtils.XSI_NIL + "/>");
            } else {
                appendString("<" + elementInfo.getXmlName() + "/>");                    
            }
            appendElse();
        }
        
        if (elementInfo.isAnyType()) {
            serializeAnyTypeElement(elementInfo, jsVar);
            // mtom can be turned on for the special complex type that is really a basic type with 
            // a content-type attribute.
        } else if (!mtom && type instanceof XmlSchemaComplexType) {
            // it has a value
            // pass the extra null in the slot for the 'extra namespaces' needed
            // by 'any'.
            appendExpression(jsVar 
                             + ".serialize(cxfjsutils, '" 
                             + elementInfo.getXmlName() + "', null)");
        } else { // simple type
            appendString("<" + elementInfo.getXmlName() + ">");
            if (mtom) {
                appendExpression("cxfjsutils.packageMtom(" + jsVar + ")");
            } else {
                appendExpression("cxfjsutils.escapeXmlEntities(" + jsVar + ")");
            }
            appendString("</" + elementInfo.getXmlName() + ">");
        }
        
        if (array) {
            endBlock(); // for the extra level of nil checking, which might be
                        // wrong.
            endBlock(); // for the for loop.
            endBlock(); // the null protection.
        }
        
        if (nillable && !array) {
            endBlock();
        }
        
        if (optional) {
            endBlock();
        }
        endBlock(); // local variables
    }

    private void serializeAnyTypeElement(ParticleInfo elementInfo, String jsVar) {
        // name a variable for convenience.
        appendLine("var anyHolder = " + jsVar + ";");
        appendLine("var anySerializer;");
        appendLine("var typeAttr = '';");
        // we look in the global array for a serializer.
        startIf("anyHolder != null");
        startIf("!anyHolder.raw"); // no serializer for raw.
        // In anyType, the QName is for the type, not an element.
        appendLine("anySerializer = "
                   + "cxfjsutils.interfaceObject.globalElementSerializers[anyHolder.qname];");
        endBlock();
        startIf("anyHolder.xsiType");
        appendLine("var typePrefix = 'cxfjst" + anyTypePrefixCounter + "';");
        anyTypePrefixCounter++;
        appendLine("var typeAttr = 'xmlns:' + typePrefix + '=\\\''"
                   + " + anyHolder.namespaceURI + '\\\'';");
        appendLine("typeAttr = typeAttr + ' xsi:type=\\\'' + typePrefix + ':' "
                   + "+ anyHolder.localName + '\\\'';");
        endBlock();
        startIf("anySerializer");
        appendExpression(jsVar 
                         + ".serialize(cxfjsutils, '" 
                         + elementInfo.getXmlName() + "', typeAttr)");
        appendElse(); // simple type or raw
        appendExpression("'<" + elementInfo.getXmlName() + " ' + typeAttr + " + "'>'");
        startIf("!anyHolder.raw");
        appendExpression("cxfjsutils.escapeXmlEntities(" + jsVar + ")");
        appendElse();
        appendExpression("anyHolder.xml");
        endBlock();
        appendString("</" + elementInfo.getXmlName() + ">");
        endBlock();
        appendElse(); // nil (from null holder)
        appendString("<" + elementInfo.getXmlName() 
                     + " " + XmlSchemaUtils.XSI_NIL + "/>");
        endBlock();
    }

    /**
     * Generate code to serialize an xs:any. There is too much duplicate code
     * with the element serializer; fix that some day.
     * 
     * @param elementInfo
     * @param schemaCollection
     */
    public void generateCodeToSerializeAny(ParticleInfo itemInfo, String prefix,
                                           SchemaCollection schemaCollection) {
        boolean optional = XmlSchemaUtils.isParticleOptional(itemInfo.getParticle());
        boolean array = XmlSchemaUtils.isParticleArray(itemInfo.getParticle());

        appendLine("var anyHolder = this._" + itemInfo.getJavascriptName() + ";");
        appendLine("var anySerializer = null;");
        appendLine("var anyXmlTag = null;");
        appendLine("var anyXmlNsDef = null;");
        appendLine("var anyData = null;");
        appendLine("var anyStartTag;");

        startIf("anyHolder != null && !anyHolder.raw");
        appendLine("anySerializer = "
                   + "cxfjsutils.interfaceObject.globalElementSerializers[anyHolder.qname];");
        appendLine("anyXmlTag = '" + prefix + ":' + anyHolder.localName;");
        appendLine("anyXmlNsDef = 'xmlns:" + prefix + "=\\'' + anyHolder.namespaceURI" + " + '\\'';");
        appendLine("anyStartTag = '<' + anyXmlTag + ' ' + anyXmlNsDef + '>';");
        appendLine("anyEndTag = '</' + anyXmlTag + '>';");
        appendLine("anyEmptyTag = '<' + anyXmlTag + ' ' + anyXmlNsDef + '/>';");
        appendLine("anyData = anyHolder.object;");
        endBlock();
        startIf("anyHolder != null && anyHolder.raw");
        appendExpression("anyHolder.xml");
        appendElse();
        // first question: optional?
        if (optional) {
            startIf("anyHolder != null && anyData != null");
        } else {
            startIf("anyHolder == null || anyData == null");
            appendLine("throw 'null value for required any item';");
            endBlock();
        }

        String varRef = "anyData";

        if (array) {
            startFor("var ax = 0", "ax < anyData.length", "ax ++");
            varRef = "anyData[ax]";
            // we need an extra level of 'nil' testing here. Or do we, depending
            // on the type structure?
            // Recode and fiddle appropriately.
            startIf(varRef + " == null");
            appendExpression("anyEmptyTag");
            appendElse();
        }

        startIf("anySerializer"); // if no constructor, a simple type.
        // it has a value
        appendExpression("anySerializer.call(" + varRef + ", cxfjsutils, anyXmlTag, anyXmlNsDef)");
        appendElse();
        appendExpression("anyStartTag");
        appendExpression("cxfjsutils.escapeXmlEntities(" + varRef + ")");
        appendExpression("anyEndTag");
        endBlock();
        if (array) {
            endBlock(); 
            endBlock(); 
        }

        if (optional) {
            endBlock();
        }
        endBlock(); // for raw
    }

}
