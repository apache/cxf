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
package org.apache.cxf.rs.security.oauth2.filters;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.ForbiddenException;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.common.OAuthContext;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;

import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OAuthScopesFilterTest {

    @Test
    public void testOverloadedSubclassMethodKeepsItsOwnScopes() throws Exception {
        Method adminMethod = OverloadingService.class.getMethod("operation", String.class);
        Method readMethod = OverloadingService.class.getMethod("operation");

        assertAllowed(new OverloadingService(), adminMethod, "admin");
        assertForbidden(new OverloadingService(), adminMethod, "read");

        assertAllowed(new OverloadingService(), readMethod, "read");
        assertForbidden(new OverloadingService(), readMethod, "admin");
    }

    @Test
    public void testOverloadedMethodsInSameClassKeepTheirOwnScopes() throws Exception {
        Method readMethod = SameClassOverloadingService.class.getMethod("operation");
        Method adminMethod = SameClassOverloadingService.class.getMethod("operation", String.class);

        assertAllowed(new SameClassOverloadingService(), readMethod, "read");
        assertForbidden(new SameClassOverloadingService(), readMethod, "admin");

        assertAllowed(new SameClassOverloadingService(), adminMethod, "admin");
        assertForbidden(new SameClassOverloadingService(), adminMethod, "read");
    }

    @Test
    public void testOverridingMethodScopesApplyTogetherWithSuperclassScopes() throws Exception {
        Method method = OverridingService.class.getMethod("operation");

        assertAllowed(new OverridingService(), method, "read", "admin");
        assertForbidden(new OverridingService(), method, "read");
        assertForbidden(new OverridingService(), method, "admin");
    }

    @Test
    public void testOverridingMethodCannotWeakenSuperclassScopes() throws Exception {
        Method method = ReadOverridingService.class.getMethod("operation");

        assertAllowed(new ReadOverridingService(), method, "read", "admin");
        assertForbidden(new ReadOverridingService(), method, "read");
    }

    @Test
    public void testOverridingMethodScopesReplaceInterfaceClassScopes() throws Exception {
        Method method = InterfaceOverridingService.class.getMethod("operation");

        assertAllowed(new InterfaceOverridingService(), method, "admin");
        assertForbidden(new InterfaceOverridingService(), method, "read");
    }

    @Test
    public void testOverridingMethodScopesReplaceSuperclassClassScopes() throws Exception {
        Method method = OverridingSubResource.class.getMethod("operation");

        assertAllowed(new OverridingSubResource(), method, "admin");
        assertForbidden(new OverridingSubResource(), method, "read");
    }

    @Test
    public void testMatchAllIsResolvedPerSignature() throws Exception {
        Method matchAllMethod = MatchAllService.class.getMethod("operation", String.class);
        Method matchAnyMethod = MatchAllService.class.getMethod("operation");

        assertAllowed(new MatchAllService(), matchAllMethod, "read", "write");
        assertForbidden(new MatchAllService(), matchAllMethod, "read");

        assertAllowed(new MatchAllService(), matchAnyMethod, "read");
    }

    @Test
    public void testInheritedMethodScopesStillApply() throws Exception {
        Method method = OverridingService.class.getMethod("inherited");

        assertAllowed(new OverridingService(), method, "read");
        assertForbidden(new OverridingService(), method, "write");
    }

    @Test
    public void testUnannotatedOverrideInheritsSuperclassScopes() throws Exception {
        Method method = UnannotatedOverridingService.class.getMethod("operation");

        assertAllowed(new UnannotatedOverridingService(), method, "read");
        assertForbidden(new UnannotatedOverridingService(), method, "write");
    }

    @Test
    public void testScopesConfiguredByMethodName() throws Exception {
        Method method = UnsecuredService.class.getMethod("operation", String.class);

        OAuthScopesFilter filter = new OAuthScopesFilter();
        filter.setScopesStringMap(Collections.singletonMap("operation", "admin"));
        filter(filter, method, "admin");
        assertThrows(ForbiddenException.class, () -> filter(filter, method, "read"));
    }

    @Test
    public void testMethodScopesTakePrecedenceOverInheritedClassScopes() throws Exception {
        Method method = InheritedClassScopesService.class.getMethod("operation");

        assertAllowed(new InheritedClassScopesService(), method, "admin");
        assertForbidden(new InheritedClassScopesService(), method, "read");
    }

    @Test
    public void testGenericInterfaceMethodScopesAreEnforced() throws Exception {
        Method updateMethod = StringRepository.class.getMethod("update", String.class);

        // update(String) overrides the erased update(Object), but not the update() overload
        assertAllowed(new StringRepository(), updateMethod, "admin");
        assertForbidden(new StringRepository(), updateMethod, "read");

        Method readMethod = StringRepository.class.getMethod("update");
        assertAllowed(new StringRepository(), readMethod, "read");
        assertForbidden(new StringRepository(), readMethod, "admin");
    }

    @Test
    public void testGenericInterfaceMethodScopesApplyTogetherWithClassScopes() throws Exception {
        Method method = ClassScopesStringRepository.class.getMethod("update", String.class);

        assertAllowed(new ClassScopesStringRepository(), method, "user", "admin");
        assertForbidden(new ClassScopesStringRepository(), method, "user");
        assertForbidden(new ClassScopesStringRepository(), method, "admin");
    }

    @Test
    public void testSubclassScopesApplyTogetherWithSuperclassMethodScopes() throws Exception {
        Method method = AdminSubService.class.getMethod("operation");

        assertAllowed(new AdminSubService(), method, "read", "admin");
        assertForbidden(new AdminSubService(), method, "read");
        assertForbidden(new AdminSubService(), method, "admin");
    }

    @Test
    public void testGenericInterfaceMethodScopesApplyToInheritedImplementation() throws Exception {
        Method method = InheritingStringRepository.class.getMethod("update", String.class);

        assertAllowed(new InheritingStringRepository(), method, "admin");
        assertForbidden(new InheritingStringRepository(), method, "read");
    }

    @Test
    public void testMethodScopesFromUnrelatedSupertypesApplyTogether() throws Exception {
        Method method = AdminApiReadService.class.getMethod("operation");

        assertAllowed(new AdminApiReadService(), method, "read", "admin");
        assertForbidden(new AdminApiReadService(), method, "read");
        assertForbidden(new AdminApiReadService(), method, "admin");
    }

    @Test
    public void testMethodScopesReplaceInheritedClassScopes() throws Exception {
        Method method = InheritingAdminService.class.getMethod("adminOperation");
        assertAllowed(new InheritingAdminService(), method, "admin");
        assertForbidden(new InheritingAdminService(), method, "read");

        Method redeclaredMethod = RedeclaringAdminService.class.getMethod("adminOperation");
        assertAllowed(new RedeclaringAdminService(), redeclaredMethod, "admin");
        assertForbidden(new RedeclaringAdminService(), redeclaredMethod, "read");
    }

    @Test
    public void testUnrelatedOverloadIsNotLinkedToGenericInterfaceMethod() throws Exception {
        Method integerMethod = OverloadingStringRepository.class.getMethod("update", Integer.class);
        assertAllowed(new OverloadingStringRepository(), integerMethod, "read");
        assertForbidden(new OverloadingStringRepository(), integerMethod, "admin");

        Method stringMethod = OverloadingStringRepository.class.getMethod("update", String.class);
        assertAllowed(new OverloadingStringRepository(), stringMethod, "admin");
        assertForbidden(new OverloadingStringRepository(), stringMethod, "read");
    }

    @Test
    public void testGenericInterfaceMethodScopesApplyThroughTypeVariables() throws Exception {
        Method method = TypedStringRepository.class.getMethod("update", String.class);

        assertAllowed(new TypedStringRepository(), method, "admin");
        assertForbidden(new TypedStringRepository(), method, "read");
    }

    @Test
    public void testUnannotatedOverloadIsNotSecuredByOtherOverloads() throws Exception {
        Method method = UnannotatedOverloadService.class.getMethod("delete");

        assertAllowed(new UnannotatedOverloadService(), method, "user");
        assertForbidden(new UnannotatedOverloadService(),
                        UnannotatedOverloadService.class.getMethod("delete", String.class), "user");
    }

    @Test
    public void testInheritedClassScopesApplyToSubclassMethods() throws Exception {
        Method method = InheritedClassScopesService.class.getMethod("subclassOperation");

        assertAllowed(new InheritedClassScopesService(), method, "read");
        assertForbidden(new InheritedClassScopesService(), method, "write");
    }

    @Test
    public void testConfiguredScopesAreEnforcedTogetherWithAnnotations() throws Exception {
        Method method = BaseService.class.getMethod("operation");

        OAuthScopesFilter filter = createFilter(new BaseService());
        filter.setScopesStringMap(Collections.singletonMap("operation", "admin"));
        filter(filter, method, "read", "admin");
        assertThrows(ForbiddenException.class, () -> filter(filter, method, "read"));
        assertThrows(ForbiddenException.class, () -> filter(filter, method, "admin"));
    }

    private static void assertAllowed(Object service, Method method, String... scopes) throws Exception {
        filter(createFilter(service), method, scopes);
    }

    private static void assertForbidden(Object service, Method method, String... scopes) throws Exception {
        OAuthScopesFilter filter = createFilter(service);
        assertThrows(ForbiddenException.class, () -> filter(filter, method, scopes));
    }

    private static OAuthScopesFilter createFilter(Object service) {
        OAuthScopesFilter filter = new OAuthScopesFilter();
        filter.setSecuredObject(service);
        return filter;
    }

    private static void filter(OAuthScopesFilter filter, Method method, String... scopes) throws Exception {
        List<OAuthPermission> permissions = new ArrayList<>();
        for (String scope : scopes) {
            permissions.add(new OAuthPermission(scope));
        }
        OAuthContext context = new OAuthContext(new UserSubject("alice"), null, permissions, "code");

        MessageContext mc = mock(MessageContext.class);
        when(mc.get("org.apache.cxf.resource.method")).thenReturn(method);
        when(mc.getContent(OAuthContext.class)).thenReturn(context);

        Field field = OAuthScopesFilter.class.getDeclaredField("mc");
        field.setAccessible(true);
        field.set(filter, mc);

        filter.filter(null);
    }

    public static class BaseService {
        @Scopes("read")
        public void operation() {
        }

        @Scopes("read")
        public void inherited() {
        }
    }

    public static class OverloadingService extends BaseService {
        @Scopes("admin")
        public void operation(String value) {
        }
    }

    public static class SameClassOverloadingService {
        @Scopes("read")
        public void operation() {
        }

        @Scopes("admin")
        public void operation(String value) {
        }
    }

    public static class OverridingService extends BaseService {
        @Override
        @Scopes("admin")
        public void operation() {
        }
    }

    public static class AdminBaseService {
        @Scopes("admin")
        public void operation() {
        }
    }

    public static class ReadOverridingService extends AdminBaseService {
        @Override
        @Scopes("read")
        public void operation() {
        }
    }

    public static class UnannotatedOverridingService extends BaseService {
        @Override
        public void operation() {
        }
    }

    @Scopes("read")
    public interface ReadService {
        void operation();
    }

    public static class InterfaceOverridingService implements ReadService {
        @Override
        @Scopes("admin")
        public void operation() {
        }
    }

    public static class MatchAllService extends BaseService {
        @Scopes(value = {"read", "write" }, matchAll = true)
        public void operation(String value) {
        }
    }

    @Scopes("read")
    public static class InheritedClassScopesBaseService {
        @Scopes("admin")
        public void operation() {
        }
    }

    public static class InheritedClassScopesService extends InheritedClassScopesBaseService {
        @Override
        public void operation() {
        }

        public void subclassOperation() {
        }
    }

    public interface Repository<T> {
        @Scopes("admin")
        void update(T value);

        @Scopes("read")
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

    @Scopes("user")
    public static class ClassScopesStringRepository implements Repository<String> {
        @Override
        public void update(String value) {
        }

        @Override
        public void update() {
        }
    }

    @Scopes("admin")
    public static class AdminSubService extends BaseService {
        @Override
        public void operation() {
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

    public interface AdminApi {
        @Scopes("admin")
        void operation();
    }

    public static class AdminApiReadService extends BaseService implements AdminApi {
        @Override
        public void operation() {
        }
    }

    @Scopes("read")
    public static class ClassScopesBaseResource {
        public void operation() {
        }
    }

    public static class OverridingSubResource extends ClassScopesBaseResource {
        @Override
        @Scopes("admin")
        public void operation() {
        }
    }

    public static class InheritingAdminService extends InheritedClassScopesBaseService {
        @Scopes("admin")
        public void adminOperation() {
        }
    }

    @Scopes("read")
    public static class RedeclaringAdminService extends InheritedClassScopesBaseService {
        @Scopes("admin")
        public void adminOperation() {
        }
    }

    public static class OverloadingStringRepository implements Repository<String> {
        @Override
        public void update(String value) {
        }

        @Override
        public void update() {
        }

        @Scopes("read")
        public void update(Integer value) {
        }
    }

    public abstract static class AbstractRepository<T> implements Repository<T> { //NOPMD
    }

    public static class TypedStringRepository extends AbstractRepository<String> {
        @Override
        public void update(String value) {
        }

        @Override
        public void update() {
        }
    }

    public static class UnannotatedOverloadService {
        @Scopes("admin")
        public void delete(String id) {
        }

        public void delete() {
        }
    }

    public static class UnsecuredService {
        public void operation(String value) {
        }
    }
}
