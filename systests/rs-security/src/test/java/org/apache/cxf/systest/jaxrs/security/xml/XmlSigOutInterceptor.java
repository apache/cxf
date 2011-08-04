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
package org.apache.cxf.systest.jaxrs.security.xml;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.systest.jaxrs.security.common.CryptoLoader;
import org.apache.cxf.systest.jaxrs.security.common.SecurityUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.ElementProxy;
import org.opensaml.xml.signature.SignatureConstants;


public class XmlSigOutInterceptor extends AbstractXmlSecOutInterceptor {
    private static final Logger LOG = 
        LogUtils.getL7dLogger(XmlSigOutInterceptor.class);
    
    private boolean createReferenceId = true;
    
    public XmlSigOutInterceptor() {
    } 

    public void setCreateReferenceId(boolean create) {
        createReferenceId = create;
    }
    
    protected Document processDocument(Message message, Document doc) 
        throws Exception {
        createEnvelopedSignature(message, doc);
        return doc;
    }
    
    // enveloping & detached sigs will be supported too
    private void createEnvelopedSignature(Message message, Document doc) 
        throws Exception {
        
        String userNameKey = SecurityConstants.SIGNATURE_USERNAME;
        
        CryptoLoader loader = new CryptoLoader();
        Crypto crypto = loader.getCrypto(message, 
                                         SecurityConstants.SIGNATURE_CRYPTO,
                                         SecurityConstants.SIGNATURE_PROPERTIES);
        if (crypto == null) {
            crypto = loader.getCrypto(message, 
                                      SecurityConstants.ENCRYPT_CRYPTO,
                                      SecurityConstants.ENCRYPT_PROPERTIES);
            userNameKey = SecurityConstants.ENCRYPT_USERNAME;
        }
        String user = SecurityUtils.getUserName(message, crypto, userNameKey);
         
        if (StringUtils.isEmpty(user)) {
            return;
        }

        String password = 
            SecurityUtils.getPassword(message, user, WSPasswordCallback.SIGNATURE, this.getClass());
    
        X509Certificate[] issuerCerts = SecurityUtils.getCertificates(crypto, user);
        
        String sigAlgo = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1;
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
        //
        ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, "ds");
        
        String referenceId = "";
        if (createReferenceId) {
            String id = UUID.randomUUID().toString();
            referenceId = "#" + id;
            doc.getDocumentElement().setAttribute("ID", id);    
        }
        
        XMLSignature sig = new XMLSignature(doc, "", sigAlgo);
        doc.getDocumentElement().appendChild(sig.getElement());
        Transforms transforms = new Transforms(doc);
        transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
        
        sig.addDocument("", transforms, Constants.ALGO_ID_DIGEST_SHA1, referenceId, null);
        
        sig.addKeyInfo(issuerCerts[0]);
        sig.addKeyInfo(issuerCerts[0].getPublicKey());
        sig.sign(privateKey);
    }
    
        
}
