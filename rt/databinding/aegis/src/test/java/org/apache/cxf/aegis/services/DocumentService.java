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

package org.apache.cxf.aegis.services;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Test for mapping to DOM Document.
 */
public class DocumentService implements IDocumentService {

    private DocumentBuilderFactory documentBuilderFactory;

    public DocumentService() {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
    }

    /** {@inheritDoc}*/
    public Document returnDocument() {
        try {
            DocumentBuilder db = documentBuilderFactory.newDocumentBuilder();
            Document doc = db.newDocument();
            Element rootElement = doc.createElement("carrot");
            rootElement.appendChild(doc.createTextNode("Is a root vegetable"));
            doc.appendChild(rootElement);
            return doc;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public BeanWithDOM getBeanWithDOM() {
        BeanWithDOM bwd = new BeanWithDOM();
        bwd.fillWithSomeData();
        return bwd;
    }

    public String simpleStringReturn() {
        return "simple";
    }
}
