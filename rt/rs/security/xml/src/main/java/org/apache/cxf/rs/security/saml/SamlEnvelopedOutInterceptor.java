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
package org.apache.cxf.rs.security.saml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.xml.AbstractXmlSecOutInterceptor;
import org.apache.cxf.rs.security.xml.XmlEncOutInterceptor;
import org.apache.cxf.rs.security.xml.XmlSigOutInterceptor;
import org.apache.ws.security.saml.ext.AssertionWrapper;

public class SamlEnvelopedOutInterceptor extends AbstractXmlSecOutInterceptor {

    private static final String DEFAULT_ENV_NAME = "Envelope";
    private static final String DEFAULT_ENV_NAMESPACE = "http://org.apache.cxf/rs/env";
    private static final String DEFAULT_ENV_PREFIX = "env";
    private String envelopeName = DEFAULT_ENV_NAME;
    private String envelopeNamespace = DEFAULT_ENV_NAMESPACE;
    private String envelopePrefix = DEFAULT_ENV_PREFIX;
    
    public SamlEnvelopedOutInterceptor() {
        // SAML assertions may contain enveloped XML signatures so
        // makes sense to avoid having them signed in the detached mode
        super.addAfter(XmlSigOutInterceptor.class.getName());
        
        super.addBefore(XmlEncOutInterceptor.class.getName());
    } 

    
    protected Document processDocument(Message message, Document doc) 
        throws Exception {
        return createEnvelopedSamlToken(message, doc);
    }
    
    // enveloping & detached sigs will be supported too
    private Document createEnvelopedSamlToken(Message message, Document payloadDoc) 
        throws Exception {
        
        Document newDoc = DOMUtils.createDocument();
        Element root = 
            newDoc.createElementNS(envelopeNamespace, envelopePrefix + ":" + envelopeName);
        newDoc.appendChild(root);
        AssertionWrapper assertion = SAMLUtils.createAssertion(message);
        Element assertionEl = assertion.toDOM(newDoc);
        root.appendChild(assertionEl);
        
        Element docEl = payloadDoc.getDocumentElement();
        payloadDoc.removeChild(docEl);
        newDoc.adoptNode(docEl);
        root.appendChild(docEl);
        return newDoc;
    }


    public void setEnvelopeName(String envelopeName) {
        this.envelopeName = envelopeName;
    }


    public String getEnvelopeName() {
        return envelopeName;
    }


    public void setEnvelopeNamespace(String envelopeNamespace) {
        this.envelopeNamespace = envelopeNamespace;
    }


    public String getEnvelopeNamespace() {
        return envelopeNamespace;
    }


    public void setEnvelopePrefix(String envelopePrefix) {
        this.envelopePrefix = envelopePrefix;
    }


    public String getEnvelopePrefix() {
        return envelopePrefix;
    }
    
}
