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

package org.apache.cxf.ws.transfer.shared;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Helper class for common methods needed in project.
 * 
 * @author Erich Duda
 */
public final class TransferTools {
    
    private static DocumentBuilder documentBuilder;
    
    private static Document document;
    
    private static Transformer transformer;
    
    private TransferTools() {
        
    }
    
    /**
     * Creates new DOM Element.
     * @param name Name of Element.
     * @return DOM Element
     */
    public static Element createElement(String name) {
        return getDocument().createElement(name);
    }
    
    /**
     * Creates new DOM Element with specified namespace.
     * @param namespace Namespace of Element.
     * @param name Name of Element
     * @return DOM Element
     */
    public static Element createElementNS(String namespace, String name) {
        return getDocument().createElementNS(namespace, name);
    }
    
    /**
     * Parse XML document from input and returns Document.
     * @param source
     * @return 
     */
    public static Document parse(InputSource source) {
        try {
            return getDocumentBuilder().parse(source);
        } catch (SAXException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Transforms DOM Document to its String representation.
     * @param source
     * @param result 
     */
    public static void transform(Source source, Result result) {
        try {
            getTransformer().transform(source, result);
        } catch (TransformerException ex) {
            throw new RuntimeException("Exception occured during serialization of the XML.", ex);
        }
    }
    
    private static DocumentBuilder getDocumentBuilder() {
        if (documentBuilder == null) {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                documentBuilder = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException ex) {
                throw new IllegalArgumentException(
                        "Exception occured during creating of DocumentBuilder instance", ex);
            }
        }
        return documentBuilder;
    }
    
    private static Document getDocument() {
        if (document == null) {
            document = getDocumentBuilder().newDocument();
        }
        return document;
    }
    
    private static Transformer getTransformer() {
        if (transformer == null) {
            try {
                TransformerFactory tf = TransformerFactory.newInstance();
                transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            } catch (TransformerConfigurationException ex) {
                throw new IllegalArgumentException(
                        "Exception occurred during creation of the Transformer instance.", ex);
            }
        }
        return transformer;
    }
}
