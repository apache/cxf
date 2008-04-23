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
package org.apache.cxf.tools.wsdlto.databinding.jaxb;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.SAXException;

public class NamespaceContextImpl implements NamespaceContext {
    private Element element;

    public NamespaceContextImpl(File file) {
        Document document = null;
        DocumentBuilderFactory docFact = DocumentBuilderFactory.newInstance();
        docFact.setNamespaceAware(true);
        try {
            document = docFact.newDocumentBuilder().parse(file);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        element = document.getDocumentElement();
    }

    public String getNamespaceURI(String prefix) {
        return element.lookupNamespaceURI(prefix);

    }

    public String getPrefix(String namespaceURI) {
        return element.lookupPrefix(namespaceURI);
    }

    public Iterator getPrefixes(String namespaceURI) {
        return null;
    }

}
