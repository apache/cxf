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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.ws.policy.AssertionBuilderLoader;
import org.apache.cxf.ws.policy.AssertionBuilderRegistry;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderLoader;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderRegistry;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertionBuilder;
import org.apache.cxf.ws.security.policy.interceptors.HttpsTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.IssuedTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.KerberosTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.SamlTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.SecureConversationTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.SpnegoTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.UsernameTokenInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.WSSecurityInterceptorProvider;
import org.apache.cxf.ws.security.policy.interceptors.WSSecurityPolicyInterceptorProvider;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.xml.XMLPrimitiveAssertionBuilder;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SP13Constants;
import org.apache.wss4j.policy.builders.AlgorithmSuiteBuilder;
import org.apache.wss4j.policy.builders.AsymmetricBindingBuilder;
import org.apache.wss4j.policy.builders.ContentEncryptedElementsBuilder;
import org.apache.wss4j.policy.builders.EncryptedElementsBuilder;
import org.apache.wss4j.policy.builders.EncryptedPartsBuilder;
import org.apache.wss4j.policy.builders.HttpsTokenBuilder;
import org.apache.wss4j.policy.builders.InitiatorEncryptionTokenBuilder;
import org.apache.wss4j.policy.builders.InitiatorSignatureTokenBuilder;
import org.apache.wss4j.policy.builders.InitiatorTokenBuilder;
import org.apache.wss4j.policy.builders.IssuedTokenBuilder;
import org.apache.wss4j.policy.builders.KerberosTokenBuilder;
import org.apache.wss4j.policy.builders.KeyValueTokenBuilder;
import org.apache.wss4j.policy.builders.LayoutBuilder;
import org.apache.wss4j.policy.builders.ProtectionTokenBuilder;
import org.apache.wss4j.policy.builders.RecipientEncryptionTokenBuilder;
import org.apache.wss4j.policy.builders.RecipientSignatureTokenBuilder;
import org.apache.wss4j.policy.builders.RecipientTokenBuilder;
import org.apache.wss4j.policy.builders.RequiredElementsBuilder;
import org.apache.wss4j.policy.builders.RequiredPartsBuilder;
import org.apache.wss4j.policy.builders.SamlTokenBuilder;
import org.apache.wss4j.policy.builders.SecureConversationTokenBuilder;
import org.apache.wss4j.policy.builders.SecurityContextTokenBuilder;
import org.apache.wss4j.policy.builders.SignedElementsBuilder;
import org.apache.wss4j.policy.builders.SignedPartsBuilder;
import org.apache.wss4j.policy.builders.SpnegoContextTokenBuilder;
import org.apache.wss4j.policy.builders.SupportingTokensBuilder;
import org.apache.wss4j.policy.builders.SymmetricBindingBuilder;
import org.apache.wss4j.policy.builders.TransportBindingBuilder;
import org.apache.wss4j.policy.builders.TransportTokenBuilder;
import org.apache.wss4j.policy.builders.Trust10Builder;
import org.apache.wss4j.policy.builders.Trust13Builder;
import org.apache.wss4j.policy.builders.UsernameTokenBuilder;
import org.apache.wss4j.policy.builders.WSS10Builder;
import org.apache.wss4j.policy.builders.WSS11Builder;
import org.apache.wss4j.policy.builders.X509TokenBuilder;
import org.apache.wss4j.policy.model.AlgorithmSuite;

@NoJSR250Annotations
public final class WSSecurityPolicyLoader implements PolicyInterceptorProviderLoader, AssertionBuilderLoader {
    Bus bus;
    
    public WSSecurityPolicyLoader(Bus b) {
        bus = b;
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
        reg.registerBuilder(new AlgorithmSuiteBuilder());
        reg.registerBuilder(new AsymmetricBindingBuilder());
        reg.registerBuilder(new ContentEncryptedElementsBuilder());
        reg.registerBuilder(new EncryptedElementsBuilder());
        reg.registerBuilder(new EncryptedPartsBuilder());
        reg.registerBuilder(new HttpsTokenBuilder());
        reg.registerBuilder(new InitiatorTokenBuilder());
        reg.registerBuilder(new InitiatorSignatureTokenBuilder());
        reg.registerBuilder(new InitiatorEncryptionTokenBuilder());
        reg.registerBuilder(new IssuedTokenBuilder());
        reg.registerBuilder(new LayoutBuilder());
        reg.registerBuilder(new ProtectionTokenBuilder());
        reg.registerBuilder(new RecipientTokenBuilder());
        reg.registerBuilder(new RecipientSignatureTokenBuilder());
        reg.registerBuilder(new RecipientEncryptionTokenBuilder());
        reg.registerBuilder(new RequiredElementsBuilder());
        reg.registerBuilder(new RequiredPartsBuilder());
        reg.registerBuilder(new SamlTokenBuilder());
        reg.registerBuilder(new KerberosTokenBuilder());
        reg.registerBuilder(new SecureConversationTokenBuilder());
        reg.registerBuilder(new SecurityContextTokenBuilder());
        reg.registerBuilder(new SignedElementsBuilder());
        reg.registerBuilder(new SignedPartsBuilder());
        reg.registerBuilder(new SpnegoContextTokenBuilder());
        reg.registerBuilder(new SupportingTokensBuilder());
        reg.registerBuilder(new SymmetricBindingBuilder());
        reg.registerBuilder(new TransportBindingBuilder());
        reg.registerBuilder(new TransportTokenBuilder());
        reg.registerBuilder(new Trust10Builder());
        reg.registerBuilder(new Trust13Builder());
        reg.registerBuilder(new UsernameTokenBuilder());
        reg.registerBuilder(new KeyValueTokenBuilder());
        reg.registerBuilder(new WSS10Builder());
        reg.registerBuilder(new WSS11Builder());
        reg.registerBuilder(new X509TokenBuilder());
        
        //add generic assertions for these known things to prevent warnings
        List<QName> others = Arrays.asList(new QName[] {
            SP12Constants.INCLUDE_TIMESTAMP, SP11Constants.INCLUDE_TIMESTAMP,
            SP12Constants.ENCRYPT_SIGNATURE, SP11Constants.ENCRYPT_SIGNATURE,
            SP12Constants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY, 
            SP11Constants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY,
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
            SP12Constants.REQUIRE_INTERNAL_REFERENCE,
            SP11Constants.REQUIRE_INTERNAL_REFERENCE,
            SP12Constants.REQUIRE_ISSUER_SERIAL_REFERENCE,
            SP11Constants.REQUIRE_ISSUER_SERIAL_REFERENCE,
            SP12Constants.ENCRYPT_BEFORE_SIGNING,
            SP12Constants.SIGN_BEFORE_ENCRYPTING,
            SP12Constants.REQUIRE_KEY_IDENTIFIER_REFERENCE,
            SP11Constants.REQUIRE_KEY_IDENTIFIER_REFERENCE,
            SP12Constants.PROTECT_TOKENS,
            SP11Constants.PROTECT_TOKENS,
            SP12Constants.RSA_KEY_VALUE,
            
            SP11Constants.LAX, SP11Constants.LAXTSFIRST, SP11Constants.LAXTSLAST,
            SP12Constants.LAX, SP12Constants.LAXTSFIRST, SP12Constants.LAXTSLAST,
            SP11Constants.WSS_USERNAME_TOKEN10, SP12Constants.WSS_USERNAME_TOKEN10,  
            SP11Constants.WSS_USERNAME_TOKEN11, SP12Constants.WSS_USERNAME_TOKEN11,
            
            SP12Constants.HASH_PASSWORD, SP12Constants.NO_PASSWORD,
            SP13Constants.CREATED, SP13Constants.NONCE,    
        });
        final Map<QName, Assertion> assertions = new HashMap<QName, Assertion>();
        for (QName q : others) {
            assertions.put(q, new PrimitiveAssertion(q));
        }
        for (String s : AlgorithmSuite.getSupportedAlgorithmSuiteNames()) {
            QName q = new QName(SP11Constants.SP_NS, s);
            assertions.put(q, new PrimitiveAssertion(q));
            q = new QName(SP12Constants.SP_NS, s);
            assertions.put(q, new PrimitiveAssertion(q));
        }
        reg.registerBuilder(new PrimitiveAssertionBuilder(assertions.keySet()) {
            public Assertion build(Element element, AssertionBuilderFactory fact) {
                if (XMLPrimitiveAssertionBuilder.isOptional(element)
                    || XMLPrimitiveAssertionBuilder.isIgnorable(element)) {
                    return super.build(element, fact);
                }
                QName q = new QName(element.getNamespaceURI(), element.getLocalName());
                return assertions.get(q);
            }            
        });
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
        reg.register(new KerberosTokenInterceptorProvider());
        reg.register(new IssuedTokenInterceptorProvider());
        reg.register(new UsernameTokenInterceptorProvider(bus));
        reg.register(new SamlTokenInterceptorProvider());
        reg.register(new SecureConversationTokenInterceptorProvider());
        reg.register(new SpnegoTokenInterceptorProvider());
    }

}
