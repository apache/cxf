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

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.common.SecurityUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.opensaml.xml.signature.SignatureConstants;

//TODO: Make sure that enveloped signatures can be applied to individual
//      child nodes of an envelope root element, a new property such as 
//      targetElementQName will be needed
public class XmlSigOutInterceptor extends AbstractXmlSecOutInterceptor {
    public static final String ENVELOPED_SIG = "enveloped";
    public static final String ENVELOPING_SIG = "enveloping";
    public static final String DETACHED_SIG = "detached";
    
    public static final String DEFAULT_ENV_PREFIX = "env";
    public static final QName DEFAULT_ENV_QNAME = 
        new QName("http://org.apache.cxf/rs/env", "Envelope", DEFAULT_ENV_PREFIX);
    
    private static final Logger LOG = 
        LogUtils.getL7dLogger(XmlSigOutInterceptor.class);
    private static final Set<String> SUPPORTED_STYLES = 
        new HashSet<String>(Arrays.asList(ENVELOPED_SIG, ENVELOPING_SIG, DETACHED_SIG));
    
    private QName envelopeQName = DEFAULT_ENV_QNAME;
    private String sigStyle = ENVELOPED_SIG;
    
    private SignatureProperties sigProps = new SignatureProperties();
    
    public XmlSigOutInterceptor() {
    } 

    public void setSignatureProperties(SignatureProperties props) {
        this.sigProps = props;
    }
    
    public void setStyle(String style) {
        if (!SUPPORTED_STYLES.contains(style)) {
            throw new IllegalArgumentException("Unsupported XML Signature style");
        }
        sigStyle = style;    
    }
    
    public void setSignatureAlgorithm(String algo) {
        sigProps.setSignatureAlgo(algo);
    }
    
    public void setDigestAlgorithm(String algo) {
        sigProps.setSignatureDigestAlgo(algo);
    }
    
    
    protected Document processDocument(Message message, Document doc) 
        throws Exception {
        return createSignature(message, doc);
    }
    
    // enveloping & detached sigs will be supported too
    private Document createSignature(Message message, Document doc) 
        throws Exception {
        
        String userNameKey = SecurityConstants.SIGNATURE_USERNAME;
        
        CryptoLoader loader = new CryptoLoader();
        Crypto crypto = loader.getCrypto(message, 
                                         SecurityConstants.SIGNATURE_CRYPTO,
                                         SecurityConstants.SIGNATURE_PROPERTIES);
        String user = SecurityUtils.getUserName(message, crypto, userNameKey);
         
        if (StringUtils.isEmpty(user) || SecurityUtils.USE_REQUEST_SIGNATURE_CERT.equals(user)) {
            throw new WSSecurityException("User name is not available");
        }

        String password = 
            SecurityUtils.getPassword(message, user, WSPasswordCallback.SIGNATURE, this.getClass());
    
        X509Certificate[] issuerCerts = SecurityUtils.getCertificates(crypto, user);
        
        String sigAlgo = sigProps.getSignatureAlgo() == null 
            ? SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1 : sigProps.getSignatureAlgo();
        
        String pubKeyAlgo = issuerCerts[0].getPublicKey().getAlgorithm();
        if (pubKeyAlgo.equalsIgnoreCase("DSA")) {
            sigAlgo = XMLSignature.ALGO_ID_SIGNATURE_DSA;
        }
        PrivateKey privateKey = null;
        try {
            privateKey = crypto.getPrivateKey(user, password);
        } catch (Exception ex) {
            String errorMessage = "Private key can not be loaded, user:" + user;
            LOG.severe(errorMessage);
            throw new WSSecurityException(errorMessage, ex);
        }
        
        String id = UUID.randomUUID().toString();
        String referenceId = "#" + id;
        
        String digestAlgo = sigProps.getSignatureDigestAlgo() == null 
            ? Constants.ALGO_ID_DIGEST_SHA1 : sigProps.getSignatureDigestAlgo();
        
        XMLSignature sig = null;
        if (ENVELOPING_SIG.equals(sigStyle)) {
            sig = prepareEnvelopingSignature(doc, id, referenceId, sigAlgo, digestAlgo);
        } else if (DETACHED_SIG.equals(sigStyle)) {
            sig = prepareDetachedSignature(doc, id, referenceId, sigAlgo, digestAlgo);
        } else {
            sig = prepareEnvelopedSignature(doc, id, referenceId, sigAlgo, digestAlgo);
        }
        
        
        sig.addKeyInfo(issuerCerts[0]);
        sig.addKeyInfo(issuerCerts[0].getPublicKey());
        sig.sign(privateKey);
        return sig.getElement().getOwnerDocument();
    }
    
    private XMLSignature prepareEnvelopingSignature(Document doc, 
                                                    String id, 
                                                    String referenceId,
                                                    String sigAlgo,
                                                    String digestAlgo) throws Exception {
        Element docEl = doc.getDocumentElement();
        Document newDoc = DOMUtils.createDocument();
        doc.removeChild(docEl);
        newDoc.adoptNode(docEl);
        Element object = newDoc.createElementNS(Constants.SignatureSpecNS, "ds:Object");
        object.appendChild(docEl);
        docEl.setAttributeNS(null, "ID", id);
        docEl.setIdAttributeNS(null, "ID", true);  
        
        XMLSignature sig = new XMLSignature(newDoc, "", sigAlgo);
        newDoc.appendChild(sig.getElement());
        sig.getElement().appendChild(object);
        
        Transforms transforms = new Transforms(newDoc);
        transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
        
        sig.addDocument(referenceId, transforms, digestAlgo);
        return sig;
    }
    
    private XMLSignature prepareDetachedSignature(Document doc, 
            String id, 
            String referenceId,
            String sigAlgo,
            String digestAlgo) throws Exception {
        Element docEl = doc.getDocumentElement();
        Document newDoc = DOMUtils.createDocument();
        doc.removeChild(docEl);
        newDoc.adoptNode(docEl);
        docEl.setAttributeNS(null, "ID", id);
        docEl.setIdAttributeNS(null, "ID", true);  
        
        Element root = newDoc.createElementNS(envelopeQName.getNamespaceURI(), 
                envelopeQName.getPrefix() + ":" + envelopeQName.getLocalPart());
        root.appendChild(docEl);
        newDoc.appendChild(root);
        
        XMLSignature sig = new XMLSignature(newDoc, "", sigAlgo);
        root.appendChild(sig.getElement());
        
        Transforms transforms = new Transforms(newDoc);
        transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
        
        sig.addDocument(referenceId, transforms, digestAlgo);
        return sig;
    }
    
    private XMLSignature prepareEnvelopedSignature(Document doc, 
            String id, 
            String referenceURI,
            String sigAlgo,
            String digestAlgo) throws Exception {
        doc.getDocumentElement().setAttributeNS(null, "ID", id);
        doc.getDocumentElement().setIdAttributeNS(null, "ID", true);    
    
        XMLSignature sig = new XMLSignature(doc, "", sigAlgo);
        doc.getDocumentElement().appendChild(sig.getElement());
        Transforms transforms = new Transforms(doc);
        transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
        
        sig.addDocument(referenceURI, transforms, digestAlgo);
        return sig;
    }
    
    public void setEnvelopeName(String expandedName) {
        setEnvelopeQName(XMLUtils.convertStringToQName(expandedName, DEFAULT_ENV_PREFIX));
    }
    
    public void setEnvelopeQName(QName name) {
        if (name.getPrefix().length() == 0) {
            name = new QName(name.getNamespaceURI(), name.getLocalPart(), DEFAULT_ENV_PREFIX);
        }
        this.envelopeQName = name;
    }
}
