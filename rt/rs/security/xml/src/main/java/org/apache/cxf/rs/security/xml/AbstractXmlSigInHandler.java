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

package org.apache.cxf.rs.security.xml;

import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.common.TrustValidator;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.signature.Reference;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transform;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;

public class AbstractXmlSigInHandler extends AbstractXmlSecInHandler {
    
    private boolean removeSignature = true;
    private boolean persistSignature = true;
    
    public void setRemoveSignature(boolean remove) {
        this.removeSignature = remove;
    }
    
    public void setPersistSignature(boolean persist) {
        this.persistSignature = persist;
    }
    
    protected void checkSignature(Message message) {
        
        Document doc = getDocument(message);
        if (doc == null) {
            return;
        }

        Element root = doc.getDocumentElement();
        Element signatureElement = getSignatureElement(root);
        if (signatureElement == null) {
            throwFault("XML Signature is not available", null);
        }
        
        Crypto crypto = null;
        try {
            CryptoLoader loader = new CryptoLoader();
            crypto = loader.getCrypto(message, 
                               SecurityConstants.SIGNATURE_CRYPTO,
                               SecurityConstants.SIGNATURE_PROPERTIES);
            if (crypto == null) {
                crypto = loader.getCrypto(message, 
                                   SecurityConstants.ENCRYPT_CRYPTO,
                                   SecurityConstants.ENCRYPT_PROPERTIES);
            }
        } catch (Exception ex) {
            throwFault("Crypto can not be loaded", ex);
        }
        boolean valid = false;
        Reference ref = null;
        try {
            XMLSignature signature = new XMLSignature(signatureElement, "");    
            // See also WSS4J SAMLUtil.getCredentialFromKeyInfo 
            KeyInfo keyInfo = signature.getKeyInfo();
            
            X509Certificate cert = keyInfo.getX509Certificate();
            if (cert != null) {
                valid = signature.checkSignatureValue(cert);
            } else {
                PublicKey pk = keyInfo.getPublicKey();
                if (pk != null) {
                    valid = signature.checkSignatureValue(pk);
                }
            }
            // is this call redundant given that signature.checkSignatureValue uses References ?
            ref = getReference(signature);
            Element signedElement = validateReference(root, ref);
            
            // validate trust 
            new TrustValidator().validateTrust(crypto, cert, keyInfo.getPublicKey());
            
            if (persistSignature) {
                message.setContent(XMLSignature.class, signature);
                message.setContent(Element.class, signedElement);
            }
        } catch (Exception ex) {
            throwFault("Signature validation failed", ex);
        }
        if (!valid) {
            throwFault("Signature validation failed", null);
        }
        if (removeSignature) {
            if (!isEnveloping(root)) {
                Element signedEl = getSignedElement(root, ref);
                signedEl.removeAttribute("ID");
                root.removeChild(signatureElement);
            } else {
                Element actualBody = getActualBody(root);
                Document newDoc = DOMUtils.createDocument();
                newDoc.adoptNode(actualBody);
                root = actualBody;
            }
        }
        message.setContent(XMLStreamReader.class, 
                           new W3CDOMStreamReader(root));
        message.setContent(InputStream.class, null);
        
    }
    
    private Element getActualBody(Element envelopingSigElement) {
        Element objectNode = getNode(envelopingSigElement, Constants.SignatureSpecNS, "Object", 0);
        if (objectNode == null) {
            throwFault("Object envelope is not available", null);
        }
        Element node = DOMUtils.getFirstElement(objectNode);
        if (node == null) {
            throwFault("No signed data is found", null);
        }
        return node;
       
    }
    
    private Element getSignatureElement(Element sigParentElement) {
        if (isEnveloping(sigParentElement)) {    
            return sigParentElement;
        }
        return DOMUtils.getFirstChildWithName(sigParentElement, Constants.SignatureSpecNS, "Signature");
    }
    
    protected boolean isEnveloping(Element root) {
        return Constants.SignatureSpecNS.equals(root.getNamespaceURI())
                && "Signature".equals(root.getLocalName());
    }
    
    protected Reference getReference(XMLSignature sig) {
        int count = sig.getSignedInfo().getLength();
        if (count != 1) {
            throwFault("Multiple Signature Reference are not currently supported", null);
        }
        try {
            return sig.getSignedInfo().item(0);
        } catch (XMLSecurityException ex) {
            throwFault("Signature Reference is not available", ex);
        }
        return null;
    }
    
    protected Element validateReference(Element root, Reference ref) {
        boolean enveloped = false;
        
        String refId = ref.getURI();
        
        if (!refId.startsWith("#") || refId.length() <= 1) {
            throwFault("Only local Signature References are supported", null);
        }
        
        Element signedEl = getSignedElement(root, ref);
        if (signedEl != null) {
            enveloped = signedEl == root;
        } else {
            throwFault("Signature Reference ID is invalid", null);
        }
        
        
        Transforms transforms = null;
        try {
            transforms = ref.getTransforms();
        } catch (XMLSecurityException ex) {
            throwFault("Signature transforms can not be obtained", ex);
        }
        if (enveloped) {
            boolean isEnveloped = false;
            for (int i = 0; i < transforms.getLength(); i++) {
                try {
                    Transform tr = transforms.item(i);
                    if (Transforms.TRANSFORM_ENVELOPED_SIGNATURE.equals(tr.getURI())) {
                        isEnveloped = true;
                        break;
                    }
                } catch (Exception ex) {
                    throwFault("Problem accessing Transform instance", ex);    
                }
            }
            if (!isEnveloped) {
                throwFault("Only enveloped signatures are currently supported", null);
            }
        }
        return signedEl;
    }
    
    private Element getSignedElement(Element root, Reference ref) {
        String rootId = root.getAttribute("ID");
        String expectedID = ref.getURI().substring(1);
        
        if (!expectedID.equals(rootId)) {
            return (Element)DOMUtils.findChildWithAtt(root, null, "ID", expectedID);
        } else {
            return root;
        }
    }
}
