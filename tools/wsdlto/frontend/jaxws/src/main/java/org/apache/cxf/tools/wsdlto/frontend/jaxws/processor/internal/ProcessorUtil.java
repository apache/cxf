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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.model.DefaultValueWriter;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.util.NameUtil;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaType;

public final class ProcessorUtil {
    private static final String KEYWORDS_PREFIX = "_";

    private ProcessorUtil() {
    }

    public static String resolvePartName(MessagePartInfo part) {
        return NameUtil.mangleNameToVariableName(part.getName().getLocalPart());
    }

    public static String getPartType(MessagePartInfo part) {
        return part.getConcreteName().getLocalPart();
    }

    public static String resolvePartType(MessagePartInfo part) {
        return NameUtil.mangleNameToClassName(getPartType(part));
    }

    public static String getType(MessagePartInfo part, ToolContext context, boolean fullname) {
        String type;
        DataBindingProfile dataBinding = context.get(DataBindingProfile.class);
        if (part.isElement()) {
            type = dataBinding.getType(getElementName(part), true);
        } else {
            type = dataBinding.getType(part.getTypeQName(), false);
        }
        if (type == null) {
            type = resolvePartType(part);
        }
        return type;
    }
    public static DefaultValueWriter getDefaultValueWriter(MessagePartInfo part,
                                                             ToolContext context) {
        DataBindingProfile dataBinding = context.get(DataBindingProfile.class);
        if (part.isElement()) {
            return dataBinding.createDefaultValueWriter(getElementName(part), true);
        }
        return dataBinding.createDefaultValueWriter(part.getTypeQName(), false);
    }
    public static DefaultValueWriter getDefaultValueWriterForWrappedElement(MessagePartInfo part,
                                                           ToolContext context,
                                                           QName subElement) {
        DataBindingProfile dataBinding = context.get(DataBindingProfile.class);
        return dataBinding.createDefaultValueWriterForWrappedElement(part.getElementQName(), subElement);
    }

    public static QName getElementName(MessagePartInfo part) {
        return part == null ? null : part.getConcreteName();
    }

    //
    // support multiple -p options
    // if user change the package name through -p namespace=package name
    //
    public static QName getMappedElementName(MessagePartInfo part, ToolContext env) {
        QName origin = getElementName(part);
        if (origin == null) {
            return null;
        }
        if (!env.hasNamespace(origin.getNamespaceURI())) {
            return origin;
        }
        return new QName(env.getCustomizedNS(origin.getNamespaceURI()), origin.getLocalPart());
    }

    public static String resolvePartType(MessagePartInfo part, ToolContext env) {
        if (env != null) {
            return resolvePartType(part, env, false);
        }
        return resolvePartType(part);
    }

    public static String resolvePartType(MessagePartInfo part, ToolContext context, boolean fullName) {
        DataBindingProfile dataBinding = context.get(DataBindingProfile.class);
        if (dataBinding == null) {
            String primitiveType = JAXBUtils.builtInTypeToJavaType(part.getTypeQName().getLocalPart());
            if (part.getTypeQName() != null &&  primitiveType != null) {
                return primitiveType;
            }
            return resolvePartType(part);
        }
        String name;
        if (part.isElement()) {
            name = dataBinding.getType(getElementName(part), true);
        } else {
            name = dataBinding.getType(part.getTypeQName(), false);
        }
        if (name == null) {
            String namespace = resolvePartNamespace(part);
            if ("http://www.w3.org/2005/08/addressing".equals(namespace)) {
                //The ws-addressing stuff isn't mapped in jaxb as jax-ws specifies they
                //need to be mapped differently
                String pn = part.getConcreteName().getLocalPart();
                if ("EndpointReference".equals(pn)
                    || "ReplyTo".equals(pn)
                    || "From".equals(pn)
                    || "FaultTo".equals(pn)) {

                    name = "jakarta.xml.ws.wsaddressing.W3CEndpointReference";
                }
            }

        }
        return name;
    }

    public static String resolvePartNamespace(MessagePartInfo part) {
        return part.getConcreteName().getNamespaceURI();
    }

    public static String mangleNameToVariableName(String vName) {
        String result = NameUtil.mangleNameToVariableName(vName);
        if (JavaUtils.isJavaKeyword(result)) {
            return KEYWORDS_PREFIX + result;
        }
        return result;
    }

    public static String parsePackageName(String namespace, String defaultPackageName) {
        return PackageUtils.parsePackageName(namespace, defaultPackageName);
    }

    public static String getAbsolutePath(String location) throws IOException {
        if (location.startsWith("http://")) {
            return location;
        }
        return new File(location).getAbsolutePath().replace('\\', '/');

    }

    public static URL getWSDLURL(String location) throws Exception {
        if (location.startsWith("http://")) {
            return new URL(location);
        }
        return new File(getAbsolutePath(location)).toURI().toURL();
    }

    public static String classNameToFilePath(String className) {
        String str;
        if (className.indexOf('.') < 0) {
            return className;
        }
        str = className.replaceAll("\\.", "/");
        return str;
    }

    //
    // the non-wrapper style will get the type info from the part directly
    //
    public static String getFullClzName(MessagePartInfo part, ToolContext context, boolean primitiveType) {
        DataBindingProfile dataBinding = context.get(DataBindingProfile.class);
        String jtype = null;
        QName xmlTypeName = getElementName(part);

        // if this flag  is true , mapping to java Type first;
        // if not found , findd the primitive type : int ,long
        // if not found,  find in the generated class


        if (!primitiveType && dataBinding != null) {
            jtype = dataBinding.getType(xmlTypeName, true);
        }

        if (!primitiveType && dataBinding == null) {
            Class<?> holderClass = JAXBUtils.holderClass(xmlTypeName.getLocalPart());
            jtype = holderClass == null ? null : holderClass.getName();
            if (jtype == null) {
                jtype = JAXBUtils.builtInTypeToJavaType(xmlTypeName.getLocalPart());
            }
        }

        if (primitiveType) {
            jtype = JAXBUtils.builtInTypeToJavaType(xmlTypeName.getLocalPart());
        }

        String namespace = xmlTypeName.getNamespaceURI();
        String type = resolvePartType(part, context, true);
        String userPackage = context.mapPackageName(namespace);

        ClassCollector collector = context.get(ClassCollector.class);
        if (jtype == null) {
            jtype = collector.getTypesFullClassName(parsePackageName(namespace, userPackage), type);
        }

        if (jtype == null) {
            if (!resolvePartType(part).equals(type)) {
                jtype = resolvePartType(part, context, true);
            } else {
                jtype = parsePackageName(namespace, userPackage) + "." + type;
            }
        }


        return jtype;
    }



    public static String getFileOrURLName(String fileOrURL) {
        try {
            try {
                return escapeSpace(new URL(fileOrURL).toExternalForm());
            } catch (MalformedURLException e) {
                return new File(fileOrURL).getCanonicalFile().toURI().toURL().toExternalForm();
            }
        } catch (Exception e) {
            return fileOrURL;
        }
    }

    static String escapeSpace(String url) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < url.length(); i++) {
            if (url.charAt(i) == ' ') {
                buf.append("%20");
            } else {
                buf.append(url.charAt(i));
            }
        }
        return buf.toString();
    }

    public static String absolutize(String name) {
        // absolutize all the system IDs in the input,
        // so that we can map system IDs to DOM trees.
        try {
            URL baseURL = new File(".").getCanonicalFile().toURI().toURL();
            return new URL(baseURL, name.replaceAll(" ", "%20")).toExternalForm();
        } catch (IOException e) {
            // ignore
        }
        return name;
    }

    public static String getHandlerConfigFileName(String name) {
        return name + "_handler";
    }



    public static Node cloneNode(Document document, Node node, boolean deep) throws DOMException {
        if (document == null || node == null) {
            return null;
        }
        int type = node.getNodeType();

        if (node.getOwnerDocument() == document) {
            return node.cloneNode(deep);
        }
        Node clone;
        switch (type) {
        case Node.CDATA_SECTION_NODE:
            clone = document.createCDATASection(node.getNodeValue());
            break;
        case Node.COMMENT_NODE:
            clone = document.createComment(node.getNodeValue());
            break;
        case Node.ENTITY_REFERENCE_NODE:
            clone = document.createEntityReference(node.getNodeName());
            break;
        case Node.ELEMENT_NODE:
            clone = document.createElementNS(node.getNamespaceURI(), node.getNodeName());
            NamedNodeMap attributes = node.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attr = (Attr)attributes.item(i);
                Attr attrnew =
                    ((Element)clone).getOwnerDocument().createAttributeNS(attr.getNamespaceURI(),
                                                                      attr.getNodeName());
                attrnew.setValue(attr.getNodeValue());
                ((Element)clone).setAttributeNodeNS(attrnew);
            }
            break;

        case Node.TEXT_NODE:
            clone = document.createTextNode(node.getNodeValue());
            break;
        default:
            return null;
        }
        if (deep && type == Node.ELEMENT_NODE) {
            Node child = node.getFirstChild();
            while (child != null) {
                clone.appendChild(cloneNode(document, child, true));
                child = child.getNextSibling();
            }
        }
        return clone;
    }

    public static List<QName> getWrappedElementQNames(ToolContext context, QName partElement) {
        List<QName> qnames = new ArrayList<>();
        if (partElement == null) {
            return qnames;
        }
        for (WrapperElement element : getWrappedElement(context, partElement)) {
            qnames.add(element.getElementName());
        }
        return qnames;
    }

    public static List<WrapperElement> getWrappedElement(ToolContext context, QName partElement) {
        ServiceInfo serviceInfo = context.get(ServiceInfo.class);
        SchemaCollection schema = serviceInfo.getXmlSchemaCollection();

        XmlSchemaElement elementByName = schema.getElementByQName(partElement);

        XmlSchemaComplexType type = (XmlSchemaComplexType)elementByName.getSchemaType();

        XmlSchemaSequence seq = (XmlSchemaSequence)type.getParticle();

        List<WrapperElement> qnames = createWrappedElements(seq);

        //If it's extension
        if (seq == null && type.getContentModel() != null) {
            Object configuredMaxStackDepth = context.get(ToolConstants.CFG_MAX_EXTENSION_STACK_DEPTH);
            Integer maxStackDepth = Integer.valueOf(5);
            if (configuredMaxStackDepth instanceof Integer) {
                maxStackDepth = (Integer)configuredMaxStackDepth;
            } else if (configuredMaxStackDepth instanceof String) {
                maxStackDepth = Integer.valueOf((String)configuredMaxStackDepth);
            }
            qnames.addAll(createWrappedElementsFromExtension(schema, type, maxStackDepth));

        }

        return qnames;
    }

    private static List<WrapperElement> createWrappedElementsFromExtension(SchemaCollection schema,
                                                                           XmlSchemaComplexType type,
                                                                           int maxStackDepth) {
        List<WrapperElement> qnames = new ArrayList<>();

        XmlSchemaContent schemaContent = type.getContentModel().getContent();
        if (!(schemaContent instanceof XmlSchemaComplexContentExtension) || maxStackDepth == 0) {
            return qnames;
        }

        XmlSchemaComplexContentExtension extension = (XmlSchemaComplexContentExtension)schemaContent;
        QName baseTypeName = extension.getBaseTypeName();
        XmlSchemaType baseType = schema.getTypeByQName(baseTypeName);

        if (baseType instanceof XmlSchemaComplexType) {
            XmlSchemaComplexType complexBaseType = (XmlSchemaComplexType)baseType;

            if (complexBaseType.getParticle() == null && complexBaseType.getContentModel() != null) {
                // continue up the extension ladder
                qnames.addAll(createWrappedElementsFromExtension(schema, complexBaseType, maxStackDepth - 1));
            } else if (complexBaseType.getParticle() instanceof XmlSchemaSequence) {
                XmlSchemaSequence seq = (XmlSchemaSequence)complexBaseType.getParticle();
                qnames.addAll(createWrappedElements(seq));
            }
        }

        if (extension.getParticle() instanceof XmlSchemaSequence) {
            XmlSchemaSequence xmlSchemaSeq = (XmlSchemaSequence)extension.getParticle();
            qnames.addAll(createWrappedElements(xmlSchemaSeq));
        }

        return qnames;
    }

    private static List<WrapperElement> createWrappedElements(XmlSchemaSequence seq) {

        List<WrapperElement> qnames = new ArrayList<>();
        if (seq != null) {

            List<XmlSchemaSequenceMember> items = seq.getItems();

            for (XmlSchemaSequenceMember seqMember : items) {
                XmlSchemaElement subElement = (XmlSchemaElement)seqMember;

                if (subElement.getQName() != null) {
                    qnames.add(new WrapperElement(subElement.getWireName(), subElement.getSchemaTypeName()));
                } else {
                    qnames.add(new WrapperElement(subElement.getRef().getTargetQName(),
                                                  subElement.getSchemaTypeName()));
                }
            }
        }
        return qnames;
    }

    public static boolean isSchemaFormQualified(ToolContext context, QName partElement) {
        ServiceInfo serviceInfo = context.get(ServiceInfo.class);
        SchemaCollection schemaCol = serviceInfo.getXmlSchemaCollection();
        XmlSchema schema = schemaCol.getSchemaForElement(partElement);
        if (schema != null) {
            return schema.getElementFormDefault() == XmlSchemaForm.QUALIFIED;
        }
        return false;

    }
}
