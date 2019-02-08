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

package org.apache.cxf.helpers;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;


public class XPathUtils {

    private static XPathFactory xpathFactory = XPathFactory.newInstance();

    static {
        try {
            xpathFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        } catch (javax.xml.xpath.XPathFactoryConfigurationException ex) {
            // ignore
        }
    }

    private XPath xpath;

    public XPathUtils() {
        xpath = xpathFactory.newXPath();
    }

    public XPathUtils(final Map<String, String> ns) {
        this();

        if (ns != null) {
            xpath.setNamespaceContext(new MapNamespaceContext(ns));
        }
    }

    public XPathUtils(final NamespaceContext ctx) {
        this();
        xpath.setNamespaceContext(ctx);
    }

    public Object getValue(String xpathExpression, Node node, QName type) {
        ClassLoaderHolder loader
            = ClassLoaderUtils.setThreadContextClassloader(getClassLoader(xpath.getClass()));
        try {
            return xpath.evaluate(xpathExpression, node, type);
        } catch (Exception e) {
            return null;
        } finally {
            if (loader != null) {
                loader.reset();
            }
        }
    }
    public NodeList getValueList(String xpathExpression, Node node) {
        return (NodeList)getValue(xpathExpression, node, XPathConstants.NODESET);
    }
    public String getValueString(String xpathExpression, Node node) {
        return (String)getValue(xpathExpression, node, XPathConstants.STRING);
    }
    public Node getValueNode(String xpathExpression, Node node) {
        return (Node)getValue(xpathExpression, node, XPathConstants.NODE);
    }

    public boolean isExist(String xpathExpression, Node node, QName type) {
        return getValue(xpathExpression, node, type) != null;
    }

    private static ClassLoader getClassLoader(final Class<?> clazz) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return clazz.getClassLoader();
                }
            });
        }
        return clazz.getClassLoader();
    }

}
