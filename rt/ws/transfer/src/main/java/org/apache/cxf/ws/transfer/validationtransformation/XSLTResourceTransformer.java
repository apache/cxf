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

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.apache.cxf.feature.transform.XSLTUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.transfer.Representation;

/**
 * Implementation of the ResourceTransformer for the XSL transformation.
 */
public class XSLTResourceTransformer implements ResourceTransformer {
    
    protected Templates templates;
    
    protected ResourceValidator validator;

    public XSLTResourceTransformer(Source xsl) {
        this(xsl, null);
    }

    public XSLTResourceTransformer(Source xsl, ResourceValidator validator) {
        this.validator = validator;
        try {
            templates = TransformerFactory.newInstance().newTemplates(xsl);
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResourceValidator transform(Representation newRepresentation, Representation oldRepresentation) {
        Document doc = DOMUtils.createDocument();
        Node representation = (Node) newRepresentation.getAny();
        Node importedNode = doc.importNode(representation, true);
        doc.appendChild(importedNode);
        Document result = XSLTUtils.transform(templates, doc);
        newRepresentation.setAny(result.getDocumentElement());
        return validator;
    }
    
}
