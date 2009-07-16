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
//import org.w3c.dom.Node;
//import org.w3c.dom.Text;

/**
 * 
 */
public class BeanWithDOM {
    private Document document;
//    private Node node;
    
    
    public void fillWithSomeData() {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        Document doc = db.newDocument();
        Element rootElement = doc.createElement("carrot");
        rootElement.appendChild(doc.createTextNode("Is a root vegetable"));
        doc.appendChild(rootElement);
        document = doc;
        /*
        doc = db.newDocument();
        rootElement = doc.createElement("beet");
        doc.appendChild(rootElement);
        Text beetText = doc.createTextNode("Is a root vegetable.");
        rootElement.appendChild(beetText);
        node = beetText;
        */
    }
    
    public Document getDocument() {
        return document;
    }
    public void setDocument(Document document) {
        this.document = document;
    }
    
    /*
    public Node getNode() {
        return node;
    }
    public void setNode(Node node) {
        this.node = node;
    }
    */

}
