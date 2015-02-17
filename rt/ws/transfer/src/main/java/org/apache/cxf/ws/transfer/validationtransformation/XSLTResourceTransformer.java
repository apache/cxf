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

package org.apache.cxf.ws.transfer.validationtransformation;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.apache.cxf.ws.transfer.Representation;

/**
 * Implementation of the ResourceTransformer for the XSL transformation.
 */
public class XSLTResourceTransformer implements ResourceTransformer {
    
    protected Transformer transformer;
    
    protected DocumentBuilder documentBuilder;

    protected ResourceValidator validator;

    public XSLTResourceTransformer(Source xsl) {
        this(xsl, null);
    }

    public XSLTResourceTransformer(Source xsl, ResourceValidator validator) {
        this.validator = validator;
        try {
            transformer = TransformerFactory.newInstance().newTransformer(xsl);
        } catch (TransformerConfigurationException ex) {
            throw new IllegalArgumentException("Error occured during creating the Transformer.", ex);
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            documentBuilder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new IllegalArgumentException(
                    "Exception occured during creation of the DocumentBuilder instance.", ex);
        }
    }

    @Override
    public ResourceValidator transform(Representation newRepresentation, Representation oldRepresentation) {
        try {
            Document result = documentBuilder.newDocument();
            transformer.transform(new DOMSource((Node) newRepresentation.getAny()),
                    new DOMResult((Node) result));
            newRepresentation.setAny(result.getDocumentElement());
            return validator;
        } catch (TransformerException ex) {
            throw new RuntimeException("Error occured during transformation.", ex);
        }
    }
    
}
