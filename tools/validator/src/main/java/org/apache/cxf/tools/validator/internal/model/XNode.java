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

package org.apache.cxf.tools.validator.internal.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.util.StringUtils;

public class XNode {

    private String prefix;
    private QName name;
    private String attributeName;
    private String attributeValue;
    private boolean isDefaultAttributeValue;
    private XNode parentNode;

    private XNode failurePoint;

    private Map<String, String> nsMap = new HashMap<>();

    public void setFailurePoint(XNode point) {
        this.failurePoint = point;
    }

    public XNode getFailurePoint() {
        return this.failurePoint;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(final String newPrefix) {
        this.prefix = newPrefix;
    }

    public QName getQName() {
        return name;
    }

    public void setQName(final QName newName) {
        this.name = newName;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(final String newAttributeName) {
        this.attributeName = newAttributeName;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(final String newAttributeValue) {
        this.attributeValue = newAttributeValue;
    }

    public void setDefaultAttributeValue(boolean b) {
        this.isDefaultAttributeValue = b;
    }
    public boolean isDefaultAttributeValue() {
        return this.isDefaultAttributeValue;
    }

    public XNode getParentNode() {
        return parentNode;
    }

    public void setParentNode(final XNode newParentNode) {
        this.parentNode = newParentNode;
    }

    public Map<String, String> getNSMap() {
        return nsMap;
    }

    public String getText() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(name.getLocalPart());
        sb.append(':');
        sb.append(getAttributeValue());
        sb.append(']');
        return sb.toString();
    }

    public String getPlainText() {
        StringBuilder sb = new StringBuilder();
        Deque<XNode> parentNodes = getParentNodes();
        while (!parentNodes.isEmpty()) {
            sb.append(parentNodes.pop().getText());
        }
        sb.append(getText());
        nsMap.put(prefix, name.getNamespaceURI());
        return sb.toString();
    }

    public String getXPath() {
        StringBuilder sb = new StringBuilder();
        sb.append('/');
        sb.append(prefix);
        sb.append(':');
        sb.append(name.getLocalPart());
        if (!StringUtils.isEmpty(attributeName) && !StringUtils.isEmpty(attributeValue)) {
            sb.append('[');
            if (isDefaultAttributeValue) {
                sb.append("not(@");
                sb.append(attributeName);
                sb.append(") or ");
            }
            sb.append('@');
            sb.append(attributeName);
            sb.append("='");
            sb.append(attributeValue);
            sb.append('\'');
            sb.append(']');
        }
        return sb.toString();
    }

    private Deque<XNode> getParentNodes() {
        Deque<XNode> parentNodes = new ArrayDeque<>();

        XNode pNode = getParentNode();
        while (pNode != null) {
            nsMap.put(pNode.getPrefix(),
                      pNode.getQName().getNamespaceURI());
            parentNodes.push(pNode);
            pNode = pNode.getParentNode();
        }
        return parentNodes;
    }

    public String toString() {
        Deque<XNode> parentNodes = getParentNodes();
        StringBuilder sb = new StringBuilder();
        while (!parentNodes.isEmpty()) {
            sb.append(parentNodes.pop().getXPath());
        }
        sb.append(getXPath());
        nsMap.put(prefix, name.getNamespaceURI());
        return sb.toString();
    }


    private boolean matches(Element el) {
        if (el.getLocalName().equals(name.getLocalPart())
            && el.getNamespaceURI().equals(name.getNamespaceURI())) {
            if (!StringUtils.isEmpty(attributeName) && !StringUtils.isEmpty(attributeValue)) {
                String v = el.getAttribute(attributeName);
                if (attributeValue.equals(v) || (StringUtils.isEmpty(v) && isDefaultAttributeValue)) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }
    private boolean matches(Element el, Deque<XNode> stack) {
        if (matches(el)) {
            if (stack.isEmpty()) {
                return true;
            }
            XNode next = stack.pop();
            Node nd = el.getFirstChild();
            while (nd != null) {
                if (nd instanceof Element) {
                    el = (Element)nd;
                    if (next.matches(el, stack)) {
                        return true;
                    }
                }
                nd = nd.getNextSibling();
            }
            stack.push(next);
        }
        return false;
    }

    public boolean matches(Document doc) {
        Deque<XNode> nodes = new ArrayDeque<>();
        nodes.push(this);
        XNode pNode = getParentNode();
        while (pNode != null) {
            nodes.push(pNode);
            pNode = pNode.getParentNode();
        }
        pNode = nodes.pop();
        return pNode.matches(doc.getDocumentElement(), nodes);

    }
}
