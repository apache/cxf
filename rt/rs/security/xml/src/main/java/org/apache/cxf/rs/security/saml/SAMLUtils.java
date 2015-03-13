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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.common.SecurityUtils;
import org.apache.cxf.rs.security.saml.assertion.Subject;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.opensaml.saml.saml2.core.NameID;

public final class SAMLUtils {
    private static final Logger LOG = 
        LogUtils.getL7dLogger(SAMLUtils.class);
    
    private SAMLUtils() {
        
    }
    
    public static Subject getSubject(Message message, SamlAssertionWrapper assertionW) {
        org.opensaml.saml.saml2.core.Subject s = assertionW.getSaml2().getSubject();
        Subject subject = new Subject();
        NameID nameId = s.getNameID();
        subject.setNameQualifier(nameId.getNameQualifier());
        // if format is transient then we may need to use STSClient
        // to request an alternate name from IDP
        subject.setNameFormat(nameId.getFormat());
        
        subject.setName(nameId.getValue());
        subject.setSpId(nameId.getSPProvidedID());
        subject.setSpQualifier(nameId.getSPNameQualifier());
        return subject;
    }
    
    public static SamlAssertionWrapper createAssertion(Message message) throws Fault {
        CallbackHandler handler = SecurityUtils.getCallbackHandler(
            message, SAMLUtils.class, SecurityConstants.SAML_CALLBACK_HANDLER);
        return createAssertion(message, handler);
    }
    
    public static SamlAssertionWrapper createAssertion(Message message,
                                                   CallbackHandler handler) throws Fault {
            
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(handler, samlCallback);
        
        try {
            SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);
            if (samlCallback.isSignAssertion()) {
                //--- This code will be moved to a common utility class
                Crypto crypto = new CryptoLoader().getCrypto(message, 
                                          SecurityConstants.SIGNATURE_CRYPTO,
                                          SecurityConstants.SIGNATURE_PROPERTIES);
                
                String user = 
                    SecurityUtils.getUserName(message, crypto, SecurityConstants.SIGNATURE_USERNAME);
                if (StringUtils.isEmpty(user)) {
                    return assertion;
                }
        
                String password = 
                    SecurityUtils.getPassword(message, user, WSPasswordCallback.SIGNATURE, 
                            SAMLUtils.class);
                
                assertion.signAssertion(user, password, crypto, false);
            }
            return assertion;
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            LOG.warning(sw.toString());
            throw new Fault(new RuntimeException(ex.getMessage() + ", stacktrace: " + sw.toString()));
        }
        
    }
    
    public static SamlAssertionWrapper createAssertion(CallbackHandler handler,
                                                   SelfSignInfo info) throws Fault {
            
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(handler, samlCallback);
        
        try {
            SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);
            assertion.signAssertion(info.getUser(), 
                                    info.getPassword(), 
                                    info.getCrypto(), 
                                    false);
            return assertion;
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            LOG.warning(sw.toString());
            throw new Fault(new RuntimeException(ex.getMessage() + ", stacktrace: " + sw.toString()));
        }
        
    }
    
    public static class SelfSignInfo {
        private Crypto crypto;
        private String user;
        private String password;
        
        public SelfSignInfo(Crypto crypto, String user, String password) {
            this.crypto = crypto;
            this.user = user;
            this.password = password;
        }
        
        public Crypto getCrypto() {
            return crypto;
        }
        public String getUser() {
            return user;
        }
        public String getPassword() {
            return password;
        }
        public void setPassword(String password) {
            this.password = password;
        }
    }
}
