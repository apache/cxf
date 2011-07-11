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

package org.apache.cxf.systest.jaxrs.security.saml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Properties;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.w3c.dom.Document;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.SamlAssertionValidator;
import org.apache.ws.security.validate.Validator;

public abstract class AbstractSamlInHandler implements RequestHandler {

    private static final Logger LOG = 
        LogUtils.getL7dLogger(AbstractSamlInHandler.class);
    
    private Validator samlValidator = new SamlAssertionValidator();
    
    public void setValidator(Validator validator) {
        samlValidator = validator;
    }
    
    public void validateToken(Message message, InputStream tokenStream) {
        
        Document doc = null;
        try {
            doc = DOMUtils.readXml(new InputStreamReader(tokenStream, "UTF-8"));
        } catch (Exception ex) {
            throwFault("Assertion can not be read as XML document", ex);
        }
        
        try {
            AssertionWrapper assertion = new AssertionWrapper(doc.getDocumentElement());
            if (assertion.isSigned()) {
                RequestData data = new RequestData();
                WSSConfig cfg = new WSSConfig(); 
                data.setWssConfig(cfg);
                data.setCallbackHandler(getCallbackHandler(message));
                try {
                    data.setSigCrypto(getCrypto(message, 
                                                SecurityConstants.SIGNATURE_PROPERTIES));
                } catch (IOException ex) {
                    throwFault("Crypto can not be loaded", ex);
                }
                data.setEnableRevocation(MessageUtils.isTrue(
                    message.getContextualProperty(WSHandlerConstants.ENABLE_REVOCATION)));
                assertion.verifySignature(data, null);
                assertion.parseHOKSubject(data, null);
                Credential credential = new Credential();
                credential.setAssertion(assertion);
                if (samlValidator != null) {
                    samlValidator.validate(credential, data);
                }
                
                TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
                Certificate[] tlsCerts = null;
                if (tlsInfo != null) {
                    tlsCerts = tlsInfo.getPeerCertificates();
                }
                // AbstractSamlPolicyValidator:
                //if (!checkHolderOfKey(assertion, null, tlsCerts)) {
                //    return Response.status(401).build();
                //}
                if (!checkSenderVouches(assertion, tlsCerts)) {
                    throwFault("Sender vouchers claim fails", null);
                }
                
            }
        } catch (Exception ex) {
            throwFault("Assertion can not be validated", ex);
        }
    }

    protected void throwFault(String error, Exception ex) {
        // TODO: get bundle resource message once this filter is moved 
        // to rt/rs/security
        LOG.warning(error);
        Response response = Response.status(401).entity(error).build();
        throw ex != null ? new WebApplicationException(ex, response) : new WebApplicationException(response);
    }
    
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
    
    private CallbackHandler getCallbackHandler(Message message) {
        //Then try to get the password from the given callback handler
        Object o = message.getContextualProperty(SecurityConstants.CALLBACK_HANDLER);
    
        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler)o;
        } else if (o instanceof String) {
            try {
                handler = (CallbackHandler)ClassLoaderUtils
                    .loadClass((String)o, this.getClass()).newInstance();
            } catch (Exception e) {
                handler = null;
            }
        }
        return handler;
    }
    
    // SamlTokenPolicyValidator
    /**
     * Check the sender-vouches requirements against the received assertion. The SAML
     * Assertion and the SOAP Body must be signed by the same signature.
     */
    private boolean checkSenderVouches(
        AssertionWrapper assertionWrapper,
        Certificate[] tlsCerts
    ) {
        //
        // If we have a 2-way TLS connection, then we don't have to check that the
        // assertion + SOAP body are signed
        //
        if (tlsCerts != null && tlsCerts.length > 0) {
            return true;
        }
        return false;
//        List<String> confirmationMethods = assertionWrapper.getConfirmationMethods();
//        for (String confirmationMethod : confirmationMethods) {
//            if (OpenSAMLUtil.isMethodSenderVouches(confirmationMethod)) {
//                if (signedResults == null || signedResults.isEmpty()) {
//                    return false;
//                }
//                if (!checkAssertionAndBodyAreSigned(assertionWrapper)) {
//                    return false;
//                }
//            }
//        }
//        return true;
    }
    
}
