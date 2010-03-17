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

package org.apache.cxf.ws.security.policy;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.ws.policy.AssertionBuilderRegistry;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderRegistry;
import org.apache.cxf.ws.policy.builder.xml.XMLPrimitiveAssertionBuilder;
import org.apache.cxf.ws.security.policy.builders.AlgorithmSuiteBuilder;
import org.apache.cxf.ws.security.policy.builders.AsymmetricBindingBuilder;
import org.apache.cxf.ws.security.policy.builders.ContentEncryptedElementsBuilder;
import org.apache.cxf.ws.security.policy.builders.EncryptedElementsBuilder;
import org.apache.cxf.ws.security.policy.builders.EncryptedPartsBuilder;
import org.apache.cxf.ws.security.policy.builders.HttpsTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.InitiatorTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.IssuedTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.KeyValueTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.LayoutBuilder;
import org.apache.cxf.ws.security.policy.builders.ProtectionTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.RecipientTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.RequiredElementsBuilder;
import org.apache.cxf.ws.security.policy.builders.RequiredPartsBuilder;
import org.apache.cxf.ws.security.policy.builders.SecureConversationTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.SecurityContextTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.SignedElementsBuilder;
import org.apache.cxf.ws.security.policy.builders.SignedPartsBuilder;
import org.apache.cxf.ws.security.policy.builders.SupportingTokens12Builder;
import org.apache.cxf.ws.security.policy.builders.SupportingTokensBuilder;
import org.apache.cxf.ws.security.policy.builders.SymmetricBindingBuilder;
import org.apache.cxf.ws.security.policy.builders.TransportBindingBuilder;
import org.apache.cxf.ws.security.policy.builders.TransportTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.Trust10Builder;
import org.apache.cxf.ws.security.policy.builders.Trust13Builder;
import org.apache.cxf.ws.security.policy.builders.UsernameTokenBuilder;
import org.apache.cxf.ws.security.policy.builders.WSS10Builder;
import org.apache.cxf.ws.security.policy.builders.WSS11Builder;
import org.apache.cxf.ws.security.policy.builders.X509TokenBuilder;
import org.apache.cxf.ws.security.policy.interceptors.HttpsTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.IssuedTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.SecureConversationTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.UsernameTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.WSSecurityInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.WSSecurityPolicyInterceptorProvider;


public class WSSecurityPolicyLoader {
    Bus bus;
    
    @Resource(name = "cxf")
    public void setBus(Bus b) {
        bus = b;
    }
    
    @PostConstruct
    public void register() {
        registerBuilders();
        try {
            registerProviders();
        } catch (Throwable t) {
            //probably wss4j isn't found or something. We'll ignore this
            //as the policy framework will then not find the providers
            //and error out at that point.  If nothing uses ws-securitypolicy
            //no warnings/errors will display
        }
    }
    public void registerBuilders() {
        AssertionBuilderRegistry reg = bus.getExtension(AssertionBuilderRegistry.class);
        if (reg == null) {
            return;
        }
        PolicyBuilder pbuild = bus.getExtension(PolicyBuilder.class);
        reg.register(new AlgorithmSuiteBuilder());
        reg.register(new AsymmetricBindingBuilder(pbuild));
        reg.register(new ContentEncryptedElementsBuilder());
        reg.register(new EncryptedElementsBuilder());
        reg.register(new EncryptedPartsBuilder());
        reg.register(new HttpsTokenBuilder(pbuild));
        reg.register(new InitiatorTokenBuilder(pbuild));
        reg.register(new IssuedTokenBuilder(pbuild));
        reg.register(new LayoutBuilder());
        reg.register(new ProtectionTokenBuilder(pbuild));
        reg.register(new RecipientTokenBuilder(pbuild));
        reg.register(new RequiredElementsBuilder());
        reg.register(new RequiredPartsBuilder());
        reg.register(new SecureConversationTokenBuilder(pbuild));
        reg.register(new SecurityContextTokenBuilder());
        reg.register(new SignedElementsBuilder());
        reg.register(new SignedPartsBuilder());
        reg.register(new SupportingTokens12Builder(pbuild));
        reg.register(new SupportingTokensBuilder(pbuild));
        reg.register(new SymmetricBindingBuilder(pbuild));
        reg.register(new TransportBindingBuilder(pbuild));
        reg.register(new TransportTokenBuilder(pbuild));
        reg.register(new Trust10Builder());
        reg.register(new Trust13Builder());
        reg.register(new UsernameTokenBuilder(pbuild));
        reg.register(new KeyValueTokenBuilder());
        reg.register(new WSS10Builder());
        reg.register(new WSS11Builder());
        reg.register(new X509TokenBuilder(pbuild));
        
        //add generic assertions for these known things to prevent warnings
        List<QName> others = Arrays.asList(new QName[] {
            SP12Constants.INCLUDE_TIMESTAMP, SP11Constants.INCLUDE_TIMESTAMP,
            SP12Constants.ENCRYPT_SIGNATURE, SP11Constants.ENCRYPT_SIGNATURE,
            SP12Constants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY, 
            new QName(SP11Constants.SP_NS, SP11Constants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY),
            SP12Constants.WSS_X509_V1_TOKEN_10,
            SP12Constants.WSS_X509_V1_TOKEN_11,
            SP12Constants.WSS_X509_V3_TOKEN_10,
            SP12Constants.WSS_X509_V3_TOKEN_11,
            SP11Constants.WSS_X509_V1_TOKEN_10,
            SP11Constants.WSS_X509_V1_TOKEN_11,
            SP11Constants.WSS_X509_V3_TOKEN_10,
            SP11Constants.WSS_X509_V3_TOKEN_11,
            SP12Constants.WSS_X509_PKCS7_TOKEN_11,
            SP12Constants.WSS_X509_PKI_PATH_V1_TOKEN_11,
            SP11Constants.WSS_X509_PKCS7_TOKEN_11,
            SP11Constants.WSS_X509_PKI_PATH_V1_TOKEN_11,
            SP12Constants.REQUIRE_THUMBPRINT_REFERENCE,
            SP11Constants.REQUIRE_THUMBPRINT_REFERENCE,
            SP12Constants.REQUIRE_DERIVED_KEYS,
            SP11Constants.REQUIRE_DERIVED_KEYS,
            new QName(SP12Constants.SP_NS, SP12Constants.ENCRYPT_BEFORE_SIGNING),
            new QName(SP11Constants.SP_NS, SP11Constants.ENCRYPT_BEFORE_SIGNING),
        });
        reg.register(new XMLPrimitiveAssertionBuilder(others));
    }
    
    public void registerProviders() {
        //interceptor providers for all of the above
        PolicyInterceptorProviderRegistry reg = bus.getExtension(PolicyInterceptorProviderRegistry.class);
        if (reg == null) {
            return;
        }
        reg.register(new WSSecurityPolicyInterceptorProvider());
        reg.register(new WSSecurityInterceptorProvider());
        reg.register(new HttpsTokenInterceptorProvider());
        reg.register(new IssuedTokenInterceptorProvider());
        reg.register(new UsernameTokenInterceptorProvider());
        reg.register(new SecureConversationTokenInterceptorProvider());
    }

}
