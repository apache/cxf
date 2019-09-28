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

package org.apache.cxf.tools.util;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

import javax.xml.XMLConstants;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.ToolConstants;

public final class JAXBUtils {
    private JAXBUtils() {
    }

    private static Node innerJaxbBinding(Element schema) {
        String schemaNamespace = schema.getNamespaceURI();
        List<Element> annoList = DOMUtils.findAllElementsByTagNameNS(schema, schemaNamespace, "annotation");
        Element annotation = null;
        if (!annoList.isEmpty()) {
            annotation = annoList.get(0);
        } else {
            annotation = schema.getOwnerDocument().createElementNS(schemaNamespace, "annotation");
        }
        List<Element> appList = DOMUtils.findAllElementsByTagNameNS(annotation,
                                                                    schemaNamespace,
                                                                    "appinfo");
        Element appInfo = null;
        if (!appList.isEmpty()) {
            appInfo = appList.get(0);
        } else {
            appInfo = schema.getOwnerDocument().createElementNS(schemaNamespace, "appinfo");
            annotation.appendChild(appInfo);
        }

        Element jaxbBindings = null;
        List<Element> jaxbList = DOMUtils.findAllElementsByTagNameNS(schema,
                                                                     ToolConstants.NS_JAXB_BINDINGS,
                                                                     "schemaBindings");
        if (!jaxbList.isEmpty()) {
            jaxbBindings = jaxbList.get(0);
        } else {
            jaxbBindings = schema.getOwnerDocument().createElementNS(ToolConstants.NS_JAXB_BINDINGS,
                                                                     "schemaBindings");
            appInfo.appendChild(jaxbBindings);
        }
        return jaxbBindings;

    }

    public static Node innerJaxbPackageBinding(Element schema, String packagevalue) {
        Document doc = schema.getOwnerDocument();

        if (!DOMUtils.hasAttribute(schema, ToolConstants.NS_JAXB_BINDINGS)) {
            Attr attr =
                schema.getOwnerDocument().createAttributeNS(ToolConstants.NS_JAXB_BINDINGS,
                                                            "version");
            attr.setValue("2.0");
            schema.setAttributeNodeNS(attr);
        }

        Node schemaBindings = innerJaxbBinding(schema);

        List<Element> pkgList = DOMUtils.findAllElementsByTagNameNS(schema,
                                                                    ToolConstants.NS_JAXB_BINDINGS,
                                                                    "package");
        Element packagename = null;
        if (!pkgList.isEmpty()) {
            packagename = pkgList.get(0);
        } else {
            packagename = doc.createElementNS(ToolConstants.NS_JAXB_BINDINGS, "package");
        }
        packagename.setAttributeNS(null, "name", packagevalue);

        schemaBindings.appendChild(packagename);

        return schemaBindings.getParentNode().getParentNode();
    }

    /**
     * Create the jaxb binding file to customize namespace to package mapping
     *
     * @param namespace
     * @param pkgName
     * @return file
     */
    public static File getPackageMappingSchemaBindingFile(String namespace, String pkgName) {
        Document doc = DOMUtils.getEmptyDocument();
        Element rootElement = doc.createElementNS(ToolConstants.SCHEMA_URI, "schema");
        rootElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", ToolConstants.SCHEMA_URI);
        rootElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:jaxb", ToolConstants.NS_JAXB_BINDINGS);
        rootElement.setAttributeNS(ToolConstants.NS_JAXB_BINDINGS, "jaxb:version", "2.0");
        rootElement.setAttributeNS(null, "targetNamespace", namespace);
        Element annoElement = doc.createElementNS(ToolConstants.SCHEMA_URI, "annotation");
        Element appInfo = doc.createElementNS(ToolConstants.SCHEMA_URI, "appinfo");
        Element schemaBindings = doc.createElementNS(ToolConstants.NS_JAXB_BINDINGS, "jaxb:schemaBindings");
        Element pkgElement = doc.createElementNS(ToolConstants.NS_JAXB_BINDINGS, "jaxb:package");
        pkgElement.setAttributeNS(null, "name", pkgName);
        annoElement.appendChild(appInfo);
        appInfo.appendChild(schemaBindings);
        schemaBindings.appendChild(pkgElement);
        rootElement.appendChild(annoElement);
        File tmpFile = null;
        try {
            tmpFile = FileUtils.createTempFile("customzied", ".xsd");
            try (OutputStream out = Files.newOutputStream(tmpFile.toPath())) {
                StaxUtils.writeTo(rootElement, out);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tmpFile;
    }
}
