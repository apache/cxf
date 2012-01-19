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

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptableObject;

/**
 * A Rhino wrapper to define DOMParser.
 */
public class JsSimpleDomParser extends ScriptableObject {

    private DocumentBuilder documentBuilder;

    public JsSimpleDomParser() {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setCoalescing(true);
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void register(ScriptableObject scope) {
        try {
            ScriptableObject.defineClass(scope, JsSimpleDomParser.class);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getClassName() {
        return "DOMParser";
    }

    // CHECKSTYLE:OFF

    public Object jsFunction_parseFromString(String xml, String mimeType) {
        StringReader reader = new StringReader(xml);
        InputSource inputSource = new InputSource(reader);
        Document document;
        try {
            document = documentBuilder.parse(inputSource);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Context context = ContextFactory.getGlobal().enterContext();
        try {
            JsSimpleDomNode domNode = (JsSimpleDomNode)context.newObject(getParentScope(), "Node");
            domNode.initialize(document, null);
            return domNode;
        } finally {
            Context.exit();
        }
    }

    // CHECKSTYLE:ON

}
