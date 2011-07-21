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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.SignatureTrustValidator;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.signature.Reference;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transform;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;

public class XmlSigInHandler implements RequestHandler {
    private static final Logger LOG = 
        LogUtils.getL7dLogger(XmlSigInHandler.class);
    
    static {
        WSSConfig.init();
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
                doc = DOMUtils.readXml(is);
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

        Element root = doc.getDocumentElement();
        Element sigElement = getSignatureElement(root);
        if (sigElement == null) {
            throwFault("Enveloped Signature is not available", null);
        }
        
        Crypto crypto = null;
        try {
            crypto = getCrypto(message, SecurityConstants.SIGNATURE_PROPERTIES);
        } catch (Exception ex) {
            throwFault("Crypto can not be loaded", ex);
        }
        boolean valid = false;
        try {
            XMLSignature signature = new XMLSignature(sigElement, "");
            // WSS4J SAMLUtil.getCredentialFromKeyInfo will also handle 
            // the X509IssuerSerial case
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
            validateReference(root, signature);
            
            Credential trustCredential = new Credential();
            trustCredential.setPublicKey(keyInfo.getPublicKey());
            trustCredential.setCertificates(new X509Certificate[]{keyInfo.getX509Certificate()});
            validateTrust(trustCredential, crypto);
        } catch (Exception ex) {
            throwFault("Signature validation failed", ex);
        }
        if (!valid) {
            throwFault("Signature validation failed", null);
        }
        
        root.removeAttribute("ID");
        root.removeChild(sigElement);
        message.setContent(XMLStreamReader.class, 
                           new W3CDOMStreamReader(root));
        message.setContent(InputStream.class, null);
        
        //TODO: If we have a SAML assertion header as well with holder-of-key or
        // sender-vouches claims then we will need to store signature or parts of it
        // to validate that saml assertion and this payload have been signed by the 
        // same key
        
        return null;
    }
    
    private Element getSignatureElement(Element root) {
        NodeList list = root.getElementsByTagNameNS(Constants.SignatureSpecNS, "Signature");
        if (list != null && list.getLength() == 1) {
            return (Element)list.item(0);
        } 
        return null;
    }
    
    private void validateTrust(Credential cred, Crypto crypto) throws Exception {
        SignatureTrustValidator validator = new SignatureTrustValidator();
        RequestData data = new RequestData();
        data.setSigCrypto(crypto);
        validator.validate(cred, data);
    }
    
    protected void throwFault(String error, Exception ex) {
        // TODO: get bundle resource message once this filter is moved 
        // to rt/rs/security
        LOG.warning(error);
        Response response = Response.status(401).entity(error).build();
        throw ex != null ? new WebApplicationException(ex, response) : new WebApplicationException(response);
    }
    
    // this code will be moved to a common utility class
    protected Crypto getCrypto(Message message, String propKey) 
        throws IOException, WSSecurityException {
        
        Object o = message.getContextualProperty(propKey);
        if (o == null) {
            return null;
        }
        
        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            URL url = ClassLoaderUtils.getResource((String)o, this.getClass());
            if (url == null) {
                ResourceManager manager = message.getExchange()
                        .getBus().getExtension(ResourceManager.class);
                ClassLoader loader = manager.resolveResource("", ClassLoader.class);
                if (loader != null) {
                    Thread.currentThread().setContextClassLoader(loader);
                }
                url = manager.resolveResource((String)o, URL.class);
            }
            if (url != null) {
                Properties props = new Properties();
                InputStream in = url.openStream(); 
                props.load(in);
                in.close();
                return CryptoFactory.getInstance(props);
            } else {
                return CryptoFactory.getInstance((String)o);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }
    }
    
    protected void validateReference(Element root, XMLSignature sig) {
        Reference ref = null;
        int count = sig.getSignedInfo().getLength();
        if (count != 1) {
            throwFault("Multiple Signature Reference are not currently supported", null);
        }
        try {
            ref = sig.getSignedInfo().item(0);
        } catch (XMLSecurityException ex) {
            throwFault("Signature Reference is not available", ex);
        }
        String rootId = root.getAttribute("ID");
        String refId = ref.getId();
        if (refId.length() == 0 && rootId.length() == 0) {
            // or fragment must be expected ?
            return;
        }
        if (refId.startsWith("#") && refId.length() > 1 && refId.substring(1).equals(rootId)) {
            return;
        } else {
            throwFault("Signature Reference ID is invalid", null);
        }
        Transforms transforms = null;
        try {
            transforms = ref.getTransforms();
        } catch (XMLSecurityException ex) {
            throwFault("Signature transforms can not be obtained", ex);
        }
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
}
