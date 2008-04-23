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
package org.apache.cxf.tools.wsdlto.frontend.jaxws.customization;

import java.util.HashMap;
import java.util.Map;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.tools.common.ToolConstants;

public final class CustomNodeSelector {
    private static final Map<String, String> BINDING_NS_MAP = new HashMap<String, String>();

    private MapNamespaceContext context = new MapNamespaceContext();

    static {
        BINDING_NS_MAP.put("jaxws", ToolConstants.NS_JAXWS_BINDINGS);
        BINDING_NS_MAP.put("jaxb", ToolConstants.NS_JAXB_BINDINGS);
        BINDING_NS_MAP.put("wsdl", ToolConstants.WSDL_NAMESPACE_URI);
        BINDING_NS_MAP.put("xsd", ToolConstants.SCHEMA_URI);
    }

    public CustomNodeSelector() {
        context.addNamespaces(BINDING_NS_MAP);
    }

    public MapNamespaceContext getNamespaceContext() {
        return context;
    }

    public void addNamespaces(final Node targetNode) {
        NamedNodeMap attributes = targetNode.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node a = attributes.item(i);
            String prefix = a.getLocalName();
            String ns = a.getNodeValue();
            if (prefix != null
                && !context.getUsedNamespaces().containsKey(prefix)
                && targetNode.lookupPrefix(ns) != null) {
                if ("xmlns".equals(prefix)) {
                    continue;
                }
                context.addNamespace(prefix, ns);
            }
        }
    }

    public Node queryNode(final Node target, final String expression) {
        XPathUtils xpath = new XPathUtils(context);
       
        Node node = (Node) xpath.getValue(expression, target, XPathConstants.NODE);

        return node;
    }
    
    public NodeList queryNodes(final Node target, final String expression) {
        XPathUtils xpath = new XPathUtils(context);
       
        NodeList nodeList = (NodeList) xpath.getValue(expression, target, XPathConstants.NODESET);

        return nodeList;
    }
    
}