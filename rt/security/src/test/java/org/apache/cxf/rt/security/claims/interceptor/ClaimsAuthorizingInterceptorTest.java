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
package org.apache.cxf.rt.security.claims.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.cxf.common.security.SimpleGroup;
import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.interceptor.security.SecureAnnotationsInterceptor;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rt.security.claims.ClaimBean;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.claims.ClaimsSecurityContext;
import org.apache.cxf.rt.security.claims.SAMLClaim;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.security.claims.authorization.Claim;
import org.apache.cxf.security.claims.authorization.ClaimMode;
import org.apache.cxf.security.claims.authorization.Claims;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;


public class ClaimsAuthorizingInterceptorTest {

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
    public void testNonSAMLClaimDefaultNameAndFormat() throws Exception {
        org.apache.cxf.rt.security.claims.Claim claim1 = new org.apache.cxf.rt.security.claims.Claim();
        claim1.setClaimType("role");
        claim1.setValues(Arrays.asList("admin", "user"));
        org.apache.cxf.rt.security.claims.Claim claim2 = new org.apache.cxf.rt.security.claims.Claim();
        claim2.setClaimType("http://authentication");
        claim2.setValues(Arrays.asList("password"));

        Message m = prepareMessage(TestService.class, "claimWithSpecificName", "role", claim1, claim2);
        interceptor.handleMessage(m);

        try {
            claim1.setValues(Arrays.asList("user"));
            m = prepareMessage(TestService.class, "claimWithSpecificName", "role", claim1, claim2);
            interceptor.handleMessage(m);
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
        SAMLClaim claim = new SAMLClaim();
        claim.setNameFormat("a");
        claim.setName("b");
        claim.addValue("c");
        in2.setClaims(Collections.singletonMap("test",
                Collections.singletonList(
                   new ClaimBean(claim, "a", null, false))));
        in2.handleMessage(m);

        try {
            in.handleMessage(prepareMessage(TestService2.class, "test",
                    createDefaultClaim("user")));
            fail("AccessDeniedException expected");
        } catch (AccessDeniedException ex) {
            // expected
        }
    }

    @Test
    public void testOverloadedMethodWithoutClaimsDoesNotRemoveClaims() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new OverloadingService());
        Method adminMethod = OverloadingService.class.getMethod("delete", String.class);
        Method unsecuredMethod = OverloadingService.class.getMethod("delete");

        in.handleMessage(prepareMessage(adminMethod, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(adminMethod, createDefaultClaim("user")));

        // A method without claims of its own is not secured by the claims of its overloads
        in.handleMessage(prepareMessage(unsecuredMethod, createDefaultClaim("user")));
    }

    @Test
    public void testOverloadedMethodsKeepTheirOwnClaims() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new OverloadingService());
        Method adminMethod = OverloadingService.class.getMethod("update", String.class);
        Method userMethod = OverloadingService.class.getMethod("update");

        in.handleMessage(prepareMessage(adminMethod, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(adminMethod, createDefaultClaim("user")));
        in.handleMessage(prepareMessage(userMethod, createDefaultClaim("user")));
        assertDenied(in, prepareMessage(userMethod, createDefaultClaim("admin")));
    }

    @Test
    public void testOverridingMethodClaimsApplyTogetherWithSuperclassClaims() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new OverridingService());
        Method method = OverridingService.class.getMethod("update");

        in.handleMessage(prepareMessage(method, createDefaultClaim("admin", "user")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("user")));
    }

    @Test
    public void testOverridingMethodCannotWeakenSuperclassClaims() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new UserOverridingService());
        Method method = UserOverridingService.class.getMethod("update");

        in.handleMessage(prepareMessage(method, createDefaultClaim("admin", "user")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("user")));
    }

    @Test
    public void testSubclassClaimsApplyTogetherWithSuperclassMethodClaims() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new AdminSubService());
        Method method = AdminSubService.class.getMethod("update");

        in.handleMessage(prepareMessage(method, createDefaultClaim("admin", "user")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("user")));
    }

    @Test
    public void testMethodClaimsFromUnrelatedSupertypesApplyTogether() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new AdminApiUserService());
        Method method = AdminApiUserService.class.getMethod("update");

        in.handleMessage(prepareMessage(method, createDefaultClaim("admin", "user")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("user")));
    }

    @Test
    public void testInterfaceClaimsAreEnforced() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new InterfaceService());
        Method implMethod = InterfaceService.class.getMethod("delete");
        Method interfaceMethod = SecuredApi.class.getMethod("delete");

        in.handleMessage(prepareMessage(implMethod, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(implMethod, createDefaultClaim("user")));
        assertDenied(in, prepareMessage(interfaceMethod, createDefaultClaim("user")));
    }

    @Test
    public void testGenericInterfaceMethodClaimsAreEnforced() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new StringRepository());
        Method updateMethod = StringRepository.class.getMethod("update", String.class);

        // update(String) overrides the erased update(Object), but not the update() overload
        in.handleMessage(prepareMessage(updateMethod, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(updateMethod, createDefaultClaim("user")));

        Method userMethod = StringRepository.class.getMethod("update");
        in.handleMessage(prepareMessage(userMethod, createDefaultClaim("user")));
        assertDenied(in, prepareMessage(userMethod, createDefaultClaim("admin")));
    }

    @Test
    public void testGenericInterfaceMethodClaimsApplyTogetherWithClassClaims() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new ClassClaimsStringRepository());
        Method method = ClassClaimsStringRepository.class.getMethod("update", String.class);

        in.handleMessage(prepareMessage(method, createDefaultClaim("admin", "user")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("user")));
    }

    @Test
    public void testGenericInterfaceMethodClaimsApplyToInheritedImplementation() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new InheritingStringRepository());
        Method method = InheritingStringRepository.class.getMethod("update", String.class);

        in.handleMessage(prepareMessage(method, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("user")));
    }

    @Test
    public void testMethodWithoutConfiguredClaimsIsDenied() throws Exception {
        ClaimsAuthorizingInterceptor in = new ClaimsAuthorizingInterceptor();
        in.setClaims(Collections.singletonMap("update",
            Collections.singletonList(createDefaultClaimBean("admin"))));

        in.handleMessage(prepareMessage(OverloadingService.class.getMethod("update"),
            createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(OverloadingService.class.getMethod("delete"),
            createDefaultClaim("admin")));
    }

    @Test
    public void testConfiguredClaimsAreEnforcedTogetherWithAnnotations() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new OverloadingService());
        in.setClaims(Collections.singletonMap("update",
            Collections.singletonList(createDefaultClaimBean("manager"))));
        Method method = OverloadingService.class.getMethod("update");

        in.handleMessage(prepareMessage(method, createDefaultClaim("user", "manager")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("user")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("manager")));
    }

    @Test
    public void testOverloadedSubclassMethodKeepsItsOwnClaims() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new OverloadingSubService());
        Method adminMethod = OverloadingSubService.class.getMethod("update", String.class);
        Method userMethod = OverloadingSubService.class.getMethod("update");

        in.handleMessage(prepareMessage(adminMethod, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(adminMethod, createDefaultClaim("user")));
        in.handleMessage(prepareMessage(userMethod, createDefaultClaim("user")));
        assertDenied(in, prepareMessage(userMethod, createDefaultClaim("admin")));
    }

    @Test
    public void testOverridingMethodClaimsReplaceSuperclassClassClaims() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new OverridingSubResource());
        Method method = OverridingSubResource.class.getMethod("update");

        in.handleMessage(prepareMessage(method, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("user")));
    }

    @Test
    public void testOverridingMethodClaimsReplaceInterfaceClassClaims() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new UserApiAdminService());
        Method method = UserApiAdminService.class.getMethod("update");

        in.handleMessage(prepareMessage(method, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("user")));
    }

    @Test
    public void testClassClaimsOfOtherTypesStillApply() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new LocationSubService());
        Method method = LocationSubService.class.getMethod("update");
        org.apache.cxf.rt.security.claims.Claim location = createClaim("http://location",
            "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified", "UK");

        in.handleMessage(prepareMessage(method, createDefaultClaim("admin"), location));
        assertDenied(in, prepareMessage(method, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("user"), location));
    }

    @Test
    public void testMethodClaimsReplaceSuperclassClassClaimsForUnannotatedOverride() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new UnannotatedSubResource());
        Method method = UnannotatedSubResource.class.getMethod("update");

        in.handleMessage(prepareMessage(method, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("user")));
    }

    @Test
    public void testSuperclassClassClaimsDoNotApplyToSubclassMethods() throws Exception {
        // @Claim is not @Inherited, so a class-level @Claim only applies to the methods of
        // the class which declares it
        ClaimsAuthorizingInterceptor in = createInterceptor(new SubclassOnlyService());
        Method adminMethod = SubclassOnlyService.class.getMethod("adminOperation");

        in.handleMessage(prepareMessage(adminMethod, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(adminMethod, createDefaultClaim("user")));
        in.handleMessage(prepareMessage(SubclassOnlyService.class.getMethod("update"),
            createDefaultClaim("user")));
    }

    @Test
    public void testClaimMatchAllIsResolvedPerSignature() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new MatchAllService());
        Method matchAllMethod = MatchAllService.class.getMethod("update", String.class);
        Method matchAnyMethod = MatchAllService.class.getMethod("update");

        in.handleMessage(prepareMessage(matchAllMethod, createDefaultClaim("admin", "manager")));
        assertDenied(in, prepareMessage(matchAllMethod, createDefaultClaim("admin")));
        in.handleMessage(prepareMessage(matchAnyMethod, createDefaultClaim("user")));
    }

    @Test
    public void testInheritedMethodClaimsStillApply() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new InheritingService());
        Method method = InheritingService.class.getMethod("update");

        in.handleMessage(prepareMessage(method, createDefaultClaim("user")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("admin")));
    }

    @Test
    public void testUnannotatedOverrideInheritsSuperclassClaims() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new UnannotatedOverridingService());
        Method method = UnannotatedOverridingService.class.getMethod("update");

        in.handleMessage(prepareMessage(method, createDefaultClaim("user")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("admin")));
    }

    @Test
    public void testUnrelatedOverloadIsNotLinkedToGenericInterfaceMethod() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new OverloadingStringRepository());
        Method integerMethod = OverloadingStringRepository.class.getMethod("update", Integer.class);
        Method stringMethod = OverloadingStringRepository.class.getMethod("update", String.class);

        in.handleMessage(prepareMessage(integerMethod, createDefaultClaim("user")));
        assertDenied(in, prepareMessage(integerMethod, createDefaultClaim("admin")));
        in.handleMessage(prepareMessage(stringMethod, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(stringMethod, createDefaultClaim("user")));
    }

    @Test
    public void testGenericInterfaceMethodClaimsApplyThroughTypeVariables() throws Exception {
        ClaimsAuthorizingInterceptor in = createInterceptor(new TypedStringRepository());
        Method method = TypedStringRepository.class.getMethod("update", String.class);

        in.handleMessage(prepareMessage(method, createDefaultClaim("admin")));
        assertDenied(in, prepareMessage(method, createDefaultClaim("user")));
    }

    @Test
    public void testAliasesSetAfterSecuredObject() throws Exception {
        ClaimsAuthorizingInterceptor in = new ClaimsAuthorizingInterceptor();
        in.setSecuredObject(new AliasService());
        in.setNameAliases(Collections.singletonMap("authentication", "http://authentication"));
        in.setFormatAliases(Collections.singletonMap("claims", "http://claims"));
        Method lax = AliasService.class.getMethod("laxClaim");
        Method strict = AliasService.class.getMethod("strictClaim");

        in.handleMessage(prepareMessage(lax, createClaim("http://authentication", "http://claims", "password")));
        assertDenied(in, prepareMessage(lax,
            createClaim("http://authentication", "http://claims", "smartcard")));
        in.handleMessage(prepareMessage(strict,
            createClaim("http://authentication", "http://claims", "password")));
        assertDenied(in, prepareMessage(strict,
            createClaim("http://authentication", "http://claims", "smartcard")));
    }

    @Test
    public void testAliasesReplacedAfterSecuredObject() throws Exception {
        ClaimsAuthorizingInterceptor in = new ClaimsAuthorizingInterceptor();
        in.setNameAliases(Collections.singletonMap("authentication", "http://old-authentication"));
        in.setFormatAliases(Collections.singletonMap("claims", "http://claims"));
        in.setSecuredObject(new AliasService());
        in.setNameAliases(Collections.singletonMap("authentication", "http://authentication"));
        Method lax = AliasService.class.getMethod("laxClaim");

        assertDenied(in, prepareMessage(lax,
            createClaim("http://authentication", "http://claims", "smartcard")));
        in.handleMessage(prepareMessage(lax,
            createClaim("http://old-authentication", "http://claims", "smartcard")));
    }

    private static ClaimsAuthorizingInterceptor createInterceptor(Object service) {
        ClaimsAuthorizingInterceptor in = new ClaimsAuthorizingInterceptor();
        in.setSecuredObject(service);
        return in;
    }

    private static void assertDenied(ClaimsAuthorizingInterceptor in, Message m) {
        try {
            in.handleMessage(m);
            fail("AccessDeniedException expected");
        } catch (AccessDeniedException ex) {
            // expected
        }
    }

    private static ClaimBean createDefaultClaimBean(String value) {
        org.apache.cxf.rt.security.claims.Claim claim = new org.apache.cxf.rt.security.claims.Claim();
        claim.setClaimType(SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT);
        claim.addValue(value);
        return new ClaimBean(claim, "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified",
                             ClaimMode.STRICT, false);
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
        return prepareMessage(cls, methodName, SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT, claim);
    }

    private Message prepareMessage(Class<?> cls,
                                   String methodName,
                                   String roleName,
                                   org.apache.cxf.rt.security.claims.Claim... claim)
                               throws Exception {
        return prepareMessage(cls.getMethod(methodName, new Class[]{}), roleName, claim);
    }

    private Message prepareMessage(Method method,
                                   org.apache.cxf.rt.security.claims.Claim... claim) {
        return prepareMessage(method, SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT, claim);
    }

    private Message prepareMessage(Method method,
                                   String roleName,
                                   org.apache.cxf.rt.security.claims.Claim... claim) {
        ClaimCollection claims = new ClaimCollection();
        Collections.addAll(claims, claim);

        Set<Principal> roles =
            parseRolesFromClaims(claims, roleName,
                                 "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified");

        ClaimsSecurityContext sc = new ClaimsSecurityContext() {

            private Principal p = new SimplePrincipal("user");

            @Override
            public Principal getUserPrincipal() {
                return p;
            }

            @Override
            public boolean isUserInRole(String role) {
                if (roles == null) {
                    return false;
                }
                for (Principal principalRole : roles) {
                    if (principalRole != p && principalRole.getName().equals(role)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Subject getSubject() {
                return null;
            }

            @Override
            public Set<Principal> getUserRoles() {
                return roles;
            }

            @Override
            public ClaimCollection getClaims() {
                return claims;
            }

        };
        Message m = new MessageImpl();
        m.setExchange(new ExchangeImpl());
        m.put(SecurityContext.class, sc);
        m.put("org.apache.cxf.resource.method", method);
        return m;
    }

    private org.apache.cxf.rt.security.claims.Claim createDefaultClaim(
            Object... values) {
        return createClaim(SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT,
                           "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified",
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

        // explicit name
        @Claim(name = "role", value = {"admin", "manager" })
        public void claimWithSpecificName() {

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
    public static class AliasService {
        @Claim(name = "authentication", format = "claims", value = "password", mode = ClaimMode.LAX)
        public void laxClaim() {
        }

        @Claim(name = "authentication", format = "claims", value = "password")
        public void strictClaim() {
        }
    }

    public static class TestService2 {
        @SecureRole("admin")
        public void test() {

        }
    }
    public static class OverloadingService {
        @Claim("admin")
        public void delete(String id) {

        }

        public void delete() {

        }

        @Claim("admin")
        public void update(String value) {

        }

        @Claim("user")
        public void update() {

        }
    }
    public static class BaseService {
        @Claim("user")
        public void update() {

        }
    }
    public static class OverridingService extends BaseService {
        @Override
        @Claim("admin")
        public void update() {

        }
    }
    public static class AdminBaseService {
        @Claim("admin")
        public void update() {

        }
    }
    public static class UserOverridingService extends AdminBaseService {
        @Override
        @Claim("user")
        public void update() {

        }
    }
    @Claim("admin")
    public static class AdminSubService extends BaseService {
        @Override
        public void update() {

        }
    }
    public interface AdminApi {
        @Claim("admin")
        void update();
    }
    public static class AdminApiUserService extends BaseService implements AdminApi {
        @Override
        public void update() {

        }
    }
    public interface SecuredApi {
        @Claim("admin")
        void delete();
    }
    public static class InterfaceService implements SecuredApi {
        @Override
        public void delete() {

        }
    }
    public interface Repository<T> {
        @Claim("admin")
        void update(T value);

        @Claim("user")
        void update();
    }
    public static class StringRepository implements Repository<String> {
        @Override
        public void update(String value) {

        }

        @Override
        public void update() {

        }
    }
    @Claim("user")
    public static class ClassClaimsStringRepository implements Repository<String> {
        @Override
        public void update(String value) {

        }

        @Override
        public void update() {

        }
    }
    public static class StringUpdater {
        public void update(String value) {

        }

        public void update() {

        }
    }
    public static class InheritingStringRepository extends StringUpdater implements Repository<String> {
    }
    public static class OverloadingSubService extends BaseService {
        @Claim("admin")
        public void update(String value) {

        }
    }
    @Claim("user")
    public static class ClaimsBaseResource {
        public void update() {

        }
    }
    public static class OverridingSubResource extends ClaimsBaseResource {
        @Override
        @Claim("admin")
        public void update() {

        }
    }
    public static class SubclassOnlyService extends ClaimsBaseResource {
        @Override
        public void update() {

        }

        @Claim("admin")
        public void adminOperation() {

        }
    }
    @Claim("user")
    public interface UserApi {
        void update();
    }
    public static class UserApiAdminService implements UserApi {
        @Override
        @Claim("admin")
        public void update() {

        }
    }
    @Claim(name = "http://location", value = "UK")
    public static class LocationBaseService {
        public void update() {

        }
    }
    public static class LocationSubService extends LocationBaseService {
        @Override
        @Claim("admin")
        public void update() {

        }
    }
    @Claim("user")
    public static class ClaimsAndMethodBaseResource {
        @Claim("admin")
        public void update() {

        }
    }
    public static class UnannotatedSubResource extends ClaimsAndMethodBaseResource {
        @Override
        public void update() {

        }
    }
    public static class MatchAllService extends BaseService {
        @Claim(value = {"admin", "manager" }, matchAll = true)
        public void update(String value) {

        }
    }
    public static class InheritingService extends BaseService {
    }
    public static class UnannotatedOverridingService extends BaseService {
        @Override
        public void update() {

        }
    }
    public static class OverloadingStringRepository implements Repository<String> {
        @Override
        public void update(String value) {

        }

        @Override
        public void update() {

        }

        @Claim("user")
        public void update(Integer value) {

        }
    }
    public abstract static class AbstractRepository<T> implements Repository<T> { // NOPMD
    }
    public static class TypedStringRepository extends AbstractRepository<String> {
        @Override
        public void update(String value) {

        }

        @Override
        public void update() {

        }
    }
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SecureRole {
        String[] value();
    }

    private static Set<Principal> parseRolesFromClaims(
                                                       ClaimCollection claims,
                                                       String name,
                                                       String nameFormat
    ) {
        String roleAttributeName = name;
        if (roleAttributeName == null) {
            roleAttributeName = SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT;
        }

        Set<Principal> roles = new HashSet<>();

        for (org.apache.cxf.rt.security.claims.Claim claim : claims) {
            if (claim instanceof SAMLClaim) {
                if (((SAMLClaim)claim).getName().equals(roleAttributeName)
                    && (nameFormat == null || nameFormat.equals(((SAMLClaim)claim).getNameFormat()))) {
                    addValues(roles, claim.getValues());
                    if (claim.getValues().size() > 1) {
                        // Don't search for other attributes with the same name if > 1 claim value
                        break;
                    }
                }
            } else if (claim.getClaimType().equals(roleAttributeName)) {
                addValues(roles, claim.getValues());
                if (claim.getValues().size() > 1) {
                    // Don't search for other attributes with the same name if > 1 claim value
                    break;
                }
            }
        }

        return roles;
    }

    private static void addValues(Set<Principal> roles, List<Object> values) {
        for (Object claimValue : values) {
            if (claimValue instanceof String) {
                roles.add(new SimpleGroup((String)claimValue));
            }
        }
    }
}