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

import java.io.InputStream;
import java.io.InputStreamReader;

import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.W3CDOMStreamReader;

public class SamlEnvelopedInHandler extends AbstractSamlInHandler {

    private static final String SAML2_NS = "urn:oasis:names:tc:SAML:2.0:assertion";
    private static final String SAML1_NS = "urn:oasis:names:tc:SAML:1.0:assertion";
    private static final String SAML_ASSERTION = "Assertion";
    
    private boolean bodyIsRoot;
    
    public SamlEnvelopedInHandler() {
    }
    
    public Response handleRequest(Message message, ClassResourceInfo resourceClass) {
        String method = (String)message.get(Message.HTTP_REQUEST_METHOD);
        if ("GET".equals(method)) {
            return null;
        }
        
        Document doc = null;
        InputStream is = message.getContent(InputStream.class);
        if (is != null) {
            try {
                doc = DOMUtils.readXml(new InputStreamReader(is, "UTF-8"));
            } catch (Exception ex) {
                throwFault("Invalid XML payload", ex);
            }
        } else {
            XMLStreamReader reader = message.getContent(XMLStreamReader.class);
            if (reader instanceof W3CDOMStreamReader) {
                doc = ((W3CDOMStreamReader)reader).getDocument();
            }
        }
        if (doc == null) {
            throwFault("No payload is available", null);
        }
        Element samlElement = getNode(doc.getDocumentElement(),
                SAML2_NS, SAML_ASSERTION);
        if (samlElement == null) {
            samlElement = getNode(doc.getDocumentElement(),
                    SAML1_NS, SAML_ASSERTION);
        }
        if (samlElement == null) {
            throwFault("SAML Assertion is not available", null);
        }
        validateToken(message, samlElement);
        
        doc.getDocumentElement().removeChild(samlElement);
        if (bodyIsRoot) {
            message.setContent(XMLStreamReader.class, 
                               new W3CDOMStreamReader(doc));
            message.setContent(InputStream.class, null);
        } else {
            Element actualBody = getActualBody(doc.getDocumentElement());
            if (actualBody != null) {
                Document newDoc = DOMUtils.createDocument();
                newDoc.adoptNode(actualBody);
                message.setContent(XMLStreamReader.class, 
                        new W3CDOMStreamReader(actualBody));
                message.setContent(InputStream.class, null);
            }
        }
        
        return null;
    }
    
    private Element getActualBody(Element root) {
        Element node = DOMUtils.getFirstElement(root);
        if (node != null) {
            root.removeChild(node);
        }
        return node;
    }
    
    protected Element getNode(Element parent, String ns, String name) {
        NodeList list = parent.getElementsByTagNameNS(ns, name);
        if (list != null && list.getLength() == 1) {
            return (Element)list.item(0);
        } 
        return null;
    }

    public void setBodyIsRoot(boolean bodyIsRoot) {
        this.bodyIsRoot = bodyIsRoot;
    }
}
