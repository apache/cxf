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
package org.apache.cxf.rt.security.saml.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.interceptor.security.SecureAnnotationsInterceptor;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.saml.claims.ClaimBean;
import org.apache.cxf.rt.security.saml.claims.SAMLClaim;
import org.apache.cxf.rt.security.saml.claims.SAMLSecurityContext;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.security.claims.authorization.Claim;
import org.apache.cxf.security.claims.authorization.ClaimMode;
import org.apache.cxf.security.claims.authorization.Claims;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class ClaimsAuthorizingInterceptorTest extends Assert {

    private ClaimsAuthorizingInterceptor interceptor;
    
    @Before
    public void setUp() {
        interceptor = new ClaimsAuthorizingInterceptor();
        interceptor.setNameAliases(
            Collections.singletonMap("authentication", "http://authentication"));
        interceptor.setFormatAliases(
                Collections.singletonMap("claims", "http://claims"));
        interceptor.setSecuredObject(new TestService());
        
    }
    
    @Test
    public void testClaimDefaultNameAndFormat() throws Exception {
        doTestClaims("claimWithDefaultNameAndFormat",
                createDefaultClaim("admin", "user"), 
                createClaim("http://authentication", "http://claims", "password"));
        try {
            doTestClaims("claimWithDefaultNameAndFormat",
                    createDefaultClaim("user"), 
                    createClaim("http://authentication", "http://claims", "password"));
            fail("AccessDeniedException expected");
        } catch (AccessDeniedException ex) {
            // expected
        }
    }
    
    @Test
    public void testClaimMatchAll() throws Exception {
        doTestClaims("claimMatchAll",
                createDefaultClaim("admin", "manager"), 
                createClaim("http://authentication", "http://claims", "password"));
        try {
            doTestClaims("claimMatchAll",
                    createDefaultClaim("admin"), 
                    createClaim("http://authentication", "http://claims", "password"));
            doTestClaims("claimMatchAll",
                    createDefaultClaim("manager"), 
                    createClaim("http://authentication", "http://claims", "password"));
            fail("AccessDeniedException expected");
        } catch (AccessDeniedException ex) {
            // expected
        }
    }
    
    @Test
    public void testMissingExpectedClaim() throws Exception {
        doTestClaims("claimWithDefaultNameAndFormat",
                createDefaultClaim("admin"), 
                createClaim("http://authentication", "http://claims", "password"));
        try {
            doTestClaims("claimWithDefaultNameAndFormat",
                    createDefaultClaim("admin"));
            fail("AccessDeniedException expected");
        } catch (AccessDeniedException ex) {
            // expected
        }
    }
    
    @Test
    public void testExtraNonExpectedClaim() throws Exception {
        doTestClaims("claimWithDefaultNameAndFormat",
                     createDefaultClaim("admin", "user"), 
                     createClaim("http://authentication", "http://claims", "password"),
                     createClaim("http://extra/claims", "http://claims", "claim"));
    }
    
    @Test
    public void testClaimSpecificNameAndFormat() throws Exception {
        doTestClaims("claimWithSpecificNameAndFormat",
                createClaim("http://cxf/roles", "http://claims", "admin", "user"), 
                createClaim("http://authentication", "http://claims", "password"));
        try {
            doTestClaims("claimWithSpecificNameAndFormat",
                    createDefaultClaim("admin", "user"), 
                    createClaim("http://authentication", "http://claims", "password"));
            fail("AccessDeniedException expected");
        } catch (AccessDeniedException ex) {
            // expected
        }
    }
    
    @Test
    public void testClaimLaxMode() throws Exception {
        doTestClaims("claimLaxMode",
                createClaim("http://authentication", "http://claims", "password"));
        doTestClaims("claimLaxMode");
        try {
            doTestClaims("claimLaxMode",
                         createClaim("http://authentication", "http://claims", "smartcard"));
            fail("AccessDeniedException expected");
        } catch (AccessDeniedException ex) {
            // expected
        }
    }
    
    @Test
    public void testMultipleClaims() throws Exception {
        doTestClaims("multipleClaims", 
                     createDefaultClaim("admin"),
                     createClaim("http://authentication", "http://claims", "smartcard"),
                     createClaim("http://location", "http://claims", "UK"));
        doTestClaims("multipleClaims", 
                createDefaultClaim("admin"),
                createClaim("http://authentication", "http://claims", "password"),
                createClaim("http://location", "http://claims", "USA"));
        try {
            doTestClaims("multipleClaims", 
                    createDefaultClaim("admin"),
                    createClaim("http://authentication", "http://claims", "unsecuretransport"),
                    createClaim("http://location", "http://claims", "UK"));
            fail("AccessDeniedException expected");
        } catch (AccessDeniedException ex) {
            // expected
        }
    }
    
    @Test
    public void testUserInRoleAndClaims() throws Exception {
        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor();
        in.setAnnotationClassName(SecureRole.class.getName());
        in.setSecuredObject(new TestService2());
        
        Message m = prepareMessage(TestService2.class, "test", 
                createDefaultClaim("admin"),
                createClaim("a", "b", "c"));
        
        in.handleMessage(m);
        
        ClaimsAuthorizingInterceptor in2 = new ClaimsAuthorizingInterceptor();
        org.apache.cxf.rt.security.saml.claims.SAMLClaim claim =
            new org.apache.cxf.rt.security.saml.claims.SAMLClaim();
        claim.setNameFormat("a");
        claim.setName("b");
        claim.addValue("c");
        in2.setClaims(Collections.singletonMap("test", 
                Collections.singletonList(
                   new ClaimBean(claim))));
        in2.handleMessage(m);
        
        try {
            in.handleMessage(prepareMessage(TestService2.class, "test", 
                    createDefaultClaim("user")));
            fail("AccessDeniedException expected");
        } catch (AccessDeniedException ex) {
            // expected
        }
    }
    
    
    private void doTestClaims(String methodName,
            org.apache.cxf.rt.security.claims.Claim... claim) 
        throws Exception {
        Message m = prepareMessage(TestService.class, methodName, claim);
        interceptor.handleMessage(m);
    }
    
    private Message prepareMessage(Class<?> cls,
            String methodName,
            org.apache.cxf.rt.security.claims.Claim... claim) 
        throws Exception {
        ClaimCollection claims = new ClaimCollection();
        claims.addAll(Arrays.asList(claim));
        
        Set<Principal> roles = 
            SAMLUtils.parseRolesFromClaims(claims, SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT, 
                                           SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED);
        
        SecurityContext sc = new SAMLSecurityContext(new SimplePrincipal("user"), roles, claims);
        Message m = new MessageImpl();
        m.setExchange(new ExchangeImpl());
        m.put(SecurityContext.class, sc);
        m.put("org.apache.cxf.resource.method", 
               cls.getMethod(methodName, new Class[]{}));
        return m;
    }
    
    private org.apache.cxf.rt.security.claims.Claim createDefaultClaim(
            Object... values) {
        return createClaim(SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT,
                           SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED,
                           values);
    }
    
    private org.apache.cxf.rt.security.claims.Claim createClaim(
            String name, String format, Object... values) {
        SAMLClaim claim = new SAMLClaim();
        claim.setName(name);
        claim.setNameFormat(format);
        claim.setValues(Arrays.asList(values));
        return claim;
    }
    
    @Claim(name = "authentication", format = "claims", 
           value = "password")
    public static class TestService {
        // default name and format are used
        @Claim({"admin", "manager" })
        public void claimWithDefaultNameAndFormat() {
            
        }
        
        // explicit name and format
        @Claim(name = "http://cxf/roles", format = "http://claims", 
               value = {"admin", "manager" })
        public void claimWithSpecificNameAndFormat() {
            
        }
        
        @Claim(name = "http://authentication", format = "http://claims", 
               value = "password", mode = ClaimMode.LAX)
        public void claimLaxMode() {
             
        }
        
        @Claims({
            @Claim(name = "http://location", format = "http://claims", 
                    value = {"UK", "USA" }),
            @Claim(value = {"admin", "manager" }),
            @Claim(name = "authentication", format = "claims", 
                           value = {"password", "smartcard" })
        })
        public void multipleClaims() {
             
        }
        
        // user must have both admin and manager roles, default is 'or'
        @Claim(value = {"admin", "manager" },
               matchAll = true)
        public void claimMatchAll() {
            
        }
    }
    public static class TestService2 {
        @SecureRole("admin")
        public void test() {
            
        }
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SecureRole {
        String[] value();
    }
}
