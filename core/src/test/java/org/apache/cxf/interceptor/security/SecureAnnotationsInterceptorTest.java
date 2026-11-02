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
package org.apache.cxf.interceptor.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Collections;
import java.util.List;

import javax.annotation.security.RolesAllowed;

import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.BindingOperationInfo;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecureAnnotationsInterceptorTest {

    private Method method;
    private Message message = new MessageImpl();

    @Before
    public void setUp() throws Exception {
        method = TestService.class.getMethod("echo", new Class[]{});
        message.put(SecurityContext.class, new TestSecurityContext());
        Exchange ex = new ExchangeImpl();
        message.setExchange(ex);

        Service service = mock(Service.class);
        ex.put(Service.class, service);
        MethodDispatcher md = mock(MethodDispatcher.class);
        when(service.get(MethodDispatcher.class.getName())).thenReturn(md);

        BindingOperationInfo boi = mock(BindingOperationInfo.class);
        ex.put(BindingOperationInfo.class, boi);
        when(md.getMethod(boi)).thenReturn(method);
    }

    @Test
    public void testPermitWithNoRoles() {
        new SecureAnnotationsInterceptor().handleMessage(message);
    }

    @Test
    public void testPermitWithMethodRoles() {
        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor();
        in.setAnnotationClassName(SecureRolesAllowed.class.getName());
        in.setSecuredObject(new TestService());
        in.handleMessage(message);
    }

    @Test(expected = AccessDeniedException.class)
    public void testAccessDeniedMethodRoles() {
        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor();
        in.setAnnotationClassName(SecureRolesAllowed.class.getName());
        in.setSecuredObject(new TestService2());
        in.handleMessage(message);
    }

    @Test(expected = AccessDeniedException.class)
    public void testAnnotationClassNameSetAfterSecuredObject() {
        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor();
        in.setSecuredObject(new TestService2());
        in.setAnnotationClassName(SecureRolesAllowed.class.getName());
        in.handleMessage(message);
    }

    @Test
    public void testAnnotationClassNameChangeReplacesAnnotationRoles() throws Exception {
        Method echo = MixedService.class.getMethod("echo");
        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor();
        in.setSecuredObject(new MixedService());
        assertEquals(Collections.singletonList("defaultRole"), in.getExpectedRoles(echo));

        in.setAnnotationClassName(SecureRolesAllowed.class.getName());
        assertEquals(Collections.singletonList("secureRole"), in.getExpectedRoles(echo));
    }

    @Test
    public void testAnnotationClassNameChangeKeepsConfiguredMethodRoles() throws Exception {
        Method echo = MixedService.class.getMethod("echo");
        Method other = MixedService.class.getMethod("other");
        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor();
        in.setSecuredObject(new MixedService());
        in.setMethodRolesMap(Collections.singletonMap("other", "admin"));

        in.setAnnotationClassName(SecureRolesAllowed.class.getName());
        assertEquals(Collections.singletonList("secureRole"), in.getExpectedRoles(echo));
        assertEquals(Collections.singletonList("admin"), in.getExpectedRoles(other));
    }

    @Test
    public void testConfiguredMethodNameRolesKeptAfterAnnotationClassChange() throws Exception {
        Method echo = MixedService.class.getMethod("echo");
        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor();
        in.setSecuredObject(new MixedService());
        in.setMethodRolesMap(Collections.singletonMap("echo", "admin"));

        in.setAnnotationClassName(SecureRolesAllowed.class.getName());
        // As when the annotation class is set first: the annotation's signature key takes
        // precedence, and the configured method name entry is kept
        assertEquals(Collections.singletonList("secureRole"), in.getExpectedRoles(echo));
        assertEquals(Collections.singletonList("admin"), in.methodRolesMap.get("echo"));
    }

    @Test
    public void testConfiguredMethodRolesMatchAnnotationClassSetFirst() throws Exception {
        Method echo = MixedService.class.getMethod("echo");
        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor();
        in.setAnnotationClassName(SecureRolesAllowed.class.getName());
        in.setSecuredObject(new MixedService());
        in.setMethodRolesMap(Collections.singletonMap("echo", "admin"));

        assertEquals(Collections.singletonList("secureRole"), in.getExpectedRoles(echo));
        assertEquals(Collections.singletonList("admin"), in.methodRolesMap.get("echo"));
    }

    @Test
    public void testAnnotationClassNameChangeWithCustomMethodSig() throws Exception {
        Method echo = MixedService.class.getMethod("echo");
        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor() {
            @Override
            protected String createMethodSig(Method m) {
                return "  " + super.createMethodSig(m) + " ";
            }
        };
        in.setSecuredObject(new MixedService());
        in.setAnnotationClassName(SecureRolesAllowed.class.getName());
        assertEquals(Collections.singletonList("secureRole"), in.getExpectedRoles(echo));
        for (List<String> roles : in.methodRolesMap.values()) {
            assertEquals(Collections.singletonList("secureRole"), roles);
        }
    }

    @Retention (RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD })
    public @interface SecureRolesAllowed {
        String[] value();
    }

    private static final class TestService {
        @SecureRolesAllowed("testRole")
        public void echo() {
        }
    }

    private static final class TestService2 {
        @SecureRolesAllowed("baz")
        public void echo() {
        }
    }

    private static final class MixedService {
        @RolesAllowed("defaultRole")
        @SecureRolesAllowed("secureRole")
        public void echo() {
        }

        public void other() {
        }
    }

    private static final class TestSecurityContext implements SecurityContext {

        public Principal getUserPrincipal() {
            return new SimplePrincipal("user");
        }

        public boolean isUserInRole(String role) {
            return "testRole".equals(role);
        }

    }
}
