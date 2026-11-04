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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import static org.junit.Assert.assertThrows;
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
        // As when the annotation class is set first: the annotation's roles and the configured
        // method name entry both apply
        assertEquals(Arrays.asList("secureRole", "admin"), in.getExpectedRoles(echo));
        assertEquals(Collections.singletonList("admin"), in.methodRolesMap.get("echo"));
    }

    @Test
    public void testConfiguredMethodRolesMatchAnnotationClassSetFirst() throws Exception {
        Method echo = MixedService.class.getMethod("echo");
        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor();
        in.setAnnotationClassName(SecureRolesAllowed.class.getName());
        in.setSecuredObject(new MixedService());
        in.setMethodRolesMap(Collections.singletonMap("echo", "admin"));

        assertEquals(Arrays.asList("secureRole", "admin"), in.getExpectedRoles(echo));
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

    @Test
    public void testOverloadedSubclassMethodKeepsItsOwnRoles() throws Exception {
        Method adminMethod = OverloadingService.class.getMethod("operation", String.class);
        Method userMethod = OverloadingService.class.getMethod("operation");

        assertAllowed(new OverloadingService(), adminMethod, "admin");
        assertDenied(new OverloadingService(), adminMethod, "user");
        assertAllowed(new OverloadingService(), userMethod, "user");
        assertDenied(new OverloadingService(), userMethod, "admin");
    }

    @Test
    public void testOverloadedMethodsInSameClassKeepTheirOwnRoles() throws Exception {
        Method userMethod = SameClassOverloadingService.class.getMethod("operation");
        Method adminMethod = SameClassOverloadingService.class.getMethod("operation", String.class);

        assertAllowed(new SameClassOverloadingService(), userMethod, "user");
        assertDenied(new SameClassOverloadingService(), userMethod, "admin");
        assertAllowed(new SameClassOverloadingService(), adminMethod, "admin");
        assertDenied(new SameClassOverloadingService(), adminMethod, "user");
    }

    @Test
    public void testOverridingMethodRolesApplyTogetherWithSuperclassRoles() throws Exception {
        Method target = OverridingService.class.getMethod("operation");

        assertAllowed(new OverridingService(), target, "user", "admin");
        assertDenied(new OverridingService(), target, "user");
        assertDenied(new OverridingService(), target, "admin");
    }

    @Test
    public void testOverridingMethodCannotWeakenSuperclassRoles() throws Exception {
        Method target = UserOverridingService.class.getMethod("operation");

        assertAllowed(new UserOverridingService(), target, "user", "admin");
        assertDenied(new UserOverridingService(), target, "user");
    }

    @Test
    public void testOverridingMethodRolesReplaceInterfaceClassRoles() throws Exception {
        Method target = UserApiAdminService.class.getMethod("operation");

        assertAllowed(new UserApiAdminService(), target, "admin");
        assertDenied(new UserApiAdminService(), target, "user");
    }

    @Test
    public void testOverridingMethodRolesReplaceSuperclassClassRoles() throws Exception {
        Method target = OverridingSubResource.class.getMethod("operation");

        assertAllowed(new OverridingSubResource(), target, "admin");
        assertDenied(new OverridingSubResource(), target, "user");
    }

    @Test
    public void testInheritedMethodRolesStillApply() throws Exception {
        Method target = InheritingService.class.getMethod("operation");

        assertAllowed(new InheritingService(), target, "user");
        assertDenied(new InheritingService(), target, "admin");
    }

    @Test
    public void testUnannotatedOverrideInheritsSuperclassRoles() throws Exception {
        Method target = UnannotatedOverridingService.class.getMethod("operation");

        assertAllowed(new UnannotatedOverridingService(), target, "user");
        assertDenied(new UnannotatedOverridingService(), target, "admin");
    }

    @Test
    public void testUnannotatedOverrideKeepsSuperclassRolesWhenSubclassHasOtherRoles() throws Exception {
        Method target = UnannotatedOverridingAnnotatedService.class.getMethod("operation");

        assertAllowed(new UnannotatedOverridingAnnotatedService(), target, "user");
        assertDenied(new UnannotatedOverridingAnnotatedService(), target, "admin");
        assertDenied(new UnannotatedOverridingAnnotatedService(), target);
    }

    @Test
    public void testInterfaceRolesApplyWhenImplementationHasOtherRoles() throws Exception {
        Method target = AdminApiService.class.getMethod("delete");

        assertAllowed(new AdminApiService(), target, "admin");
        assertDenied(new AdminApiService(), target, "user");
        assertDenied(new AdminApiService(), target);
    }

    @Test
    public void testSuperclassClassRolesApplyWhenSubclassHasOtherRoles() throws Exception {
        Method target = AnnotatedSubResource.class.getMethod("operation");

        assertAllowed(new AnnotatedSubResource(), target, "user");
        assertDenied(new AnnotatedSubResource(), target, "admin");
        assertDenied(new AnnotatedSubResource(), target);
    }

    @Test
    public void testSuperclassClassRolesDoNotApplyToSubclassMethods() throws Exception {
        // @RolesAllowed is not @Inherited, so a class-level @RolesAllowed only applies to the
        // methods of the class which declares it
        Method target = AnnotatedSubResource.class.getMethod("adminOperation");

        assertAllowed(new AnnotatedSubResource(), target, "admin");
        assertDenied(new AnnotatedSubResource(), target, "user");
    }

    @Test
    public void testInheritedClassRolesApplyToSubclassMethods() throws Exception {
        Method target = InheritedRolesSubService.class.getMethod("subclassOperation");

        assertAllowed(createInterceptor(new InheritedRolesSubService(), InheritedRoles.class),
                      target, "user");
        assertDenied(createInterceptor(new InheritedRolesSubService(), InheritedRoles.class),
                     target, "admin");
    }

    @Test
    public void testMethodRolesReplaceInheritedClassRoles() throws Exception {
        Method target = InheritedRolesSubService.class.getMethod("adminOperation");

        assertAllowed(createInterceptor(new InheritedRolesSubService(), InheritedRoles.class),
                      target, "admin");
        assertDenied(createInterceptor(new InheritedRolesSubService(), InheritedRoles.class),
                     target, "user");

        Method redeclaredMethod = RedeclaringInheritedRolesService.class.getMethod("adminOperation");
        assertAllowed(createInterceptor(new RedeclaringInheritedRolesService(), InheritedRoles.class),
                      redeclaredMethod, "admin");
        assertDenied(createInterceptor(new RedeclaringInheritedRolesService(), InheritedRoles.class),
                     redeclaredMethod, "user");
    }

    @Test
    public void testMethodRolesTakePrecedenceOverInheritedClassRoles() throws Exception {
        Method target = InheritedRolesOverridingService.class.getMethod("operation");

        assertAllowed(createInterceptor(new InheritedRolesOverridingService(), InheritedRoles.class),
                      target, "admin");
        assertDenied(createInterceptor(new InheritedRolesOverridingService(), InheritedRoles.class),
                     target, "user");
    }

    @Test
    public void testSubclassRolesApplyTogetherWithSuperclassMethodRoles() throws Exception {
        Method target = AdminSubService.class.getMethod("operation");

        assertAllowed(new AdminSubService(), target, "user", "admin");
        assertDenied(new AdminSubService(), target, "user");
        assertDenied(new AdminSubService(), target, "admin");
    }

    @Test
    public void testSubclassClassRolesDoNotApplyToInheritedAnnotatedMethods() throws Exception {
        // As in JSR-250, the inherited method keeps the roles of the superclass which declares it
        Method target = AdminInheritingService.class.getMethod("operation");

        assertAllowed(new AdminInheritingService(), target, "user");
        assertDenied(new AdminInheritingService(), target, "admin");
        assertDenied(new AdminInheritingService(), target);
    }

    @Test
    public void testSubclassClassRolesApplyToInheritedUnannotatedMethods() throws Exception {
        Method target = AdminInheritingService.class.getMethod("unannotatedOperation");

        assertAllowed(new AdminInheritingService(), target, "admin");
        assertDenied(new AdminInheritingService(), target, "user");
        assertDenied(new AdminInheritingService(), target);
    }

    @Test
    public void testMethodRolesFromUnrelatedSupertypesApplyTogether() throws Exception {
        Method target = AdminApiUserService.class.getMethod("operation");

        assertAllowed(new AdminApiUserService(), target, "user", "admin");
        assertDenied(new AdminApiUserService(), target, "user");
        assertDenied(new AdminApiUserService(), target, "admin");
    }

    @Test
    public void testGenericInterfaceMethodRolesAreEnforced() throws Exception {
        // update(String) overrides the erased update(Object), but not the update() overload
        Method target = StringRepository.class.getMethod("update", String.class);
        assertAllowed(new StringRepository(), target, "admin");
        assertDenied(new StringRepository(), target, "user");
        assertDenied(new StringRepository(), target);

        Method userMethod = StringRepository.class.getMethod("update");
        assertAllowed(new StringRepository(), userMethod, "user");
        assertDenied(new StringRepository(), userMethod, "admin");
    }

    @Test
    public void testGenericInterfaceMethodRolesApplyTogetherWithClassRoles() throws Exception {
        Method target = ClassRolesStringRepository.class.getMethod("update", String.class);

        assertAllowed(new ClassRolesStringRepository(), target, "user", "admin");
        assertDenied(new ClassRolesStringRepository(), target, "user");
        assertDenied(new ClassRolesStringRepository(), target, "admin");
    }

    @Test
    public void testGenericInterfaceMethodRolesApplyToInheritedImplementation() throws Exception {
        Method target = InheritingStringRepository.class.getMethod("update", String.class);

        assertAllowed(new InheritingStringRepository(), target, "admin");
        assertDenied(new InheritingStringRepository(), target, "user");
    }

    @Test
    public void testGenericInterfaceMethodRolesApplyThroughTypeVariables() throws Exception {
        Method target = TypedStringRepository.class.getMethod("update", String.class);

        assertAllowed(new TypedStringRepository(), target, "admin");
        assertDenied(new TypedStringRepository(), target, "user");
    }

    @Test
    public void testUnrelatedOverloadIsNotLinkedToGenericInterfaceMethod() throws Exception {
        Method integerMethod = OverloadingStringRepository.class.getMethod("update", Integer.class);
        assertAllowed(new OverloadingStringRepository(), integerMethod, "user");
        assertDenied(new OverloadingStringRepository(), integerMethod, "admin");

        Method stringMethod = OverloadingStringRepository.class.getMethod("update", String.class);
        assertAllowed(new OverloadingStringRepository(), stringMethod, "admin");
        assertDenied(new OverloadingStringRepository(), stringMethod, "user");
    }

    @Test
    public void testUnannotatedOverloadIsNotSecuredByOtherOverloads() throws Exception {
        Method target = UnannotatedOverloadService.class.getMethod("delete");

        assertAllowed(new UnannotatedOverloadService(), target, "user");
        assertDenied(new UnannotatedOverloadService(),
                     UnannotatedOverloadService.class.getMethod("delete", String.class), "user");
    }

    @Test
    public void testRolesConfiguredByMethodName() throws Exception {
        Method target = UnsecuredService.class.getMethod("delete");

        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor();
        in.setMethodRolesMap(Collections.singletonMap("delete", "admin"));
        assertAllowed(in, target, "admin");
        assertDenied(in, target, "user");
    }

    @Test
    public void testConfiguredRolesAreEnforcedTogetherWithAnnotations() throws Exception {
        Method target = BaseService.class.getMethod("operation");

        SecureAnnotationsInterceptor in = createInterceptor(new BaseService(), RolesAllowed.class);
        in.setMethodRolesMap(Collections.singletonMap("operation", "admin"));
        assertAllowed(in, target, "user", "admin");
        assertDenied(in, target, "user");
        assertDenied(in, target, "admin");
    }

    @Test
    public void testRolesAddedToRolesMapBySubclassAreEnforced() throws Exception {
        Method target = UnsecuredService.class.getMethod("delete");

        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor() {
            @Override
            protected void findRoles(Class<?> cls, Map<String, String> rolesMap) {
                super.findRoles(cls, rolesMap);
                rolesMap.put("delete", "admin");
            }
        };
        in.setSecuredObject(new UnsecuredService());
        assertAllowed(in, target, "admin");
        assertDenied(in, target, "user");
    }

    private static SecureAnnotationsInterceptor createInterceptor(Object service,
                                                                  Class<?> annotationClass) {
        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor();
        in.setAnnotationClassName(annotationClass.getName());
        in.setSecuredObject(service);
        return in;
    }

    private static void assertAllowed(Object service, Method method, String... roles) {
        assertAllowed(createInterceptor(service, RolesAllowed.class), method, roles);
    }

    private static void assertDenied(Object service, Method method, String... roles) {
        assertDenied(createInterceptor(service, RolesAllowed.class), method, roles);
    }

    private static void assertAllowed(SecureAnnotationsInterceptor in, Method method, String... roles) {
        in.handleMessage(createMessage(method, roles));
    }

    private static void assertDenied(SecureAnnotationsInterceptor in, Method method, String... roles) {
        assertThrows(AccessDeniedException.class, () -> in.handleMessage(createMessage(method, roles)));
    }

    // A message from an anonymous user when no roles are given
    private static Message createMessage(Method method, String... roles) {
        Message m = new MessageImpl();
        m.setExchange(new ExchangeImpl());
        m.put("org.apache.cxf.resource.method", method);
        if (roles.length > 0) {
            Set<String> userRoles = new HashSet<>(Arrays.asList(roles));
            m.put(SecurityContext.class, new SecurityContext() {
                public Principal getUserPrincipal() {
                    return new SimplePrincipal("user");
                }

                public boolean isUserInRole(String role) {
                    return userRoles.contains(role);
                }
            });
        }
        return m;
    }

    @Retention (RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD })
    @Inherited
    public @interface InheritedRoles {
        String[] value();
    }

    public static class BaseService {
        @RolesAllowed("user")
        public void operation() {
        }
    }

    public static class OverloadingService extends BaseService {
        @RolesAllowed("admin")
        public void operation(String value) {
        }
    }

    public static class SameClassOverloadingService {
        @RolesAllowed("user")
        public void operation() {
        }

        @RolesAllowed("admin")
        public void operation(String value) {
        }
    }

    public static class OverridingService extends BaseService {
        @Override
        @RolesAllowed("admin")
        public void operation() {
        }
    }

    public static class AdminBaseService {
        @RolesAllowed("admin")
        public void operation() {
        }
    }

    public static class UserOverridingService extends AdminBaseService {
        @Override
        @RolesAllowed("user")
        public void operation() {
        }
    }

    @RolesAllowed("user")
    public interface UserApi {
        void operation();
    }

    public static class UserApiAdminService implements UserApi {
        @Override
        @RolesAllowed("admin")
        public void operation() {
        }
    }

    @RolesAllowed("user")
    public static class ClassRolesBaseResource {
        public void operation() {
        }
    }

    public static class OverridingSubResource extends ClassRolesBaseResource {
        @Override
        @RolesAllowed("admin")
        public void operation() {
        }
    }

    public static class AnnotatedSubResource extends ClassRolesBaseResource {
        @RolesAllowed("admin")
        public void adminOperation() {
        }
    }

    public static class InheritingService extends BaseService {
    }

    public static class UnannotatedOverridingService extends BaseService {
        @Override
        public void operation() {
        }
    }

    public static class UnannotatedOverridingAnnotatedService extends BaseService {
        @Override
        public void operation() {
        }

        @RolesAllowed("admin")
        public void adminOperation() {
        }
    }

    public interface AdminApi {
        @RolesAllowed("admin")
        void delete();
    }

    public static class AdminApiService implements AdminApi {
        @Override
        public void delete() {
        }

        @RolesAllowed("user")
        public void list() {
        }
    }

    @InheritedRoles("user")
    public static class InheritedRolesBaseService {
        @InheritedRoles("admin")
        public void operation() {
        }
    }

    public static class InheritedRolesOverridingService extends InheritedRolesBaseService {
        @Override
        public void operation() {
        }
    }

    public static class InheritedRolesSubService extends InheritedRolesBaseService {
        public void subclassOperation() {
        }

        @InheritedRoles("admin")
        public void adminOperation() {
        }
    }

    @InheritedRoles("user")
    public static class RedeclaringInheritedRolesService extends InheritedRolesBaseService {
        @InheritedRoles("admin")
        public void adminOperation() {
        }
    }

    @RolesAllowed("admin")
    public static class AdminSubService extends BaseService {
        @Override
        public void operation() {
        }
    }

    public static class UnannotatedMethodBaseService extends BaseService {
        public void unannotatedOperation() {
        }
    }

    @RolesAllowed("admin")
    public static class AdminInheritingService extends UnannotatedMethodBaseService {
    }

    public interface AdminOperationApi {
        @RolesAllowed("admin")
        void operation();
    }

    public static class AdminApiUserService extends BaseService implements AdminOperationApi {
        @Override
        public void operation() {
        }
    }

    public interface Repository<T> {
        @RolesAllowed("admin")
        void update(T value);

        @RolesAllowed("user")
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

    @RolesAllowed("user")
    public static class ClassRolesStringRepository implements Repository<String> {
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

    public static class OverloadingStringRepository implements Repository<String> {
        @Override
        public void update(String value) {
        }

        @Override
        public void update() {
        }

        @RolesAllowed("user")
        public void update(Integer value) {
        }
    }

    public static class UnannotatedOverloadService {
        @RolesAllowed("admin")
        public void delete(String id) {
        }

        public void delete() {
        }
    }

    public static class UnsecuredService {
        public void delete() {
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
