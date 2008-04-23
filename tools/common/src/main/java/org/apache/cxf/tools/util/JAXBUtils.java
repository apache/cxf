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
import java.io.FileOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.tools.common.ToolConstants;

public final class JAXBUtils {
    private JAXBUtils() {
    }

    private static Node innerJaxbBinding(Element schema) {
        String schemaNamespace = schema.getNamespaceURI();
        NodeList annoList = schema.getElementsByTagNameNS(schemaNamespace, "annotation");
        Element annotation = null;
        if (annoList.getLength() > 0) {
            annotation = (Element)annoList.item(0);
        } else {
            annotation = schema.getOwnerDocument().createElementNS(schemaNamespace, "annotation");
        }

        NodeList appList = annotation.getElementsByTagNameNS(schemaNamespace, "appinfo");
        Element appInfo = null;
        if (appList.getLength() > 0) {
            appInfo = (Element)appList.item(0);
        } else {
            appInfo = schema.getOwnerDocument().createElementNS(schemaNamespace, "appinfo");
            annotation.appendChild(appInfo);
        }

        Element jaxbBindings = null;
        NodeList jaxbList = schema.getElementsByTagNameNS(ToolConstants.NS_JAXB_BINDINGS, "schemaBindings");
        if (jaxbList.getLength() > 0) {
            jaxbBindings = (Element)jaxbList.item(0);
        } else {
            jaxbBindings = schema.getOwnerDocument().createElementNS(ToolConstants.NS_JAXB_BINDINGS, 
                                                                     "schemaBindings");
            appInfo.appendChild(jaxbBindings);
        }
        return jaxbBindings;

    }

    public static Node innerJaxbPackageBinding(Element schema, String packagevalue) {
        Document doc = schema.getOwnerDocument();

        if (!XMLUtils.hasAttribute(schema, ToolConstants.NS_JAXB_BINDINGS)) {
            schema.setAttributeNS(ToolConstants.NS_JAXB_BINDINGS, "version", "2.0");
        }

        Node schemaBindings = innerJaxbBinding(schema);

        NodeList pkgList = doc.getElementsByTagNameNS(ToolConstants.NS_JAXB_BINDINGS,
                                                      "package");
        Element packagename = null;
        if (pkgList.getLength() > 0) {
            packagename = (Element)pkgList.item(0);
        } else {
            packagename = doc.createElementNS(ToolConstants.NS_JAXB_BINDINGS, "package");
        }
        packagename.setAttribute("name", packagevalue);

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
        Document doc = DOMUtils.createDocument();
        Element rootElement = doc.createElement("schema");
        rootElement.setAttribute("xmlns", ToolConstants.SCHEMA_URI);
        rootElement.setAttribute("xmlns:jaxb", ToolConstants.NS_JAXB_BINDINGS);
        rootElement.setAttribute("jaxb:version", "2.0");
        rootElement.setAttribute("targetNamespace", namespace);
        Element annoElement = doc.createElement("annotation");
        Element appInfo = doc.createElement("appinfo");
        Element schemaBindings = doc.createElement("jaxb:schemaBindings");
        Element pkgElement = doc.createElement("jaxb:package");
        pkgElement.setAttribute("name", pkgName);
        annoElement.appendChild(appInfo);
        appInfo.appendChild(schemaBindings);
        schemaBindings.appendChild(pkgElement);
        rootElement.appendChild(annoElement);
        File tmpFile = null;
        try {
            tmpFile = FileUtils.createTempFile("customzied", ".xsd");
            FileOutputStream fout = new FileOutputStream(tmpFile);
            DOMUtils.writeXml(rootElement, fout);
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tmpFile;
    }
}
