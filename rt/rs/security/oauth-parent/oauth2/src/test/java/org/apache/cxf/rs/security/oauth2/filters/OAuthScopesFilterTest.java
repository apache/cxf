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

import jakarta.ws.rs.ForbiddenException;
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
    public void testOverridingMethodScopesTakePrecedenceOverSuperclass() throws Exception {
        Method method = OverridingService.class.getMethod("operation");

        assertAllowed(new OverridingService(), method, "admin");
        assertForbidden(new OverridingService(), method, "read");
    }

    @Test
    public void testOverridingMethodScopesTakePrecedenceOverInterfaceClassScopes() throws Exception {
        Method method = InterfaceOverridingService.class.getMethod("operation");

        assertAllowed(new InterfaceOverridingService(), method, "admin");
        assertForbidden(new InterfaceOverridingService(), method, "read");
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

        // The erased interface signature does not match, so every rule for the name applies
        assertAllowed(new StringRepository(), updateMethod, "admin", "read");
        assertForbidden(new StringRepository(), updateMethod, "read");
        assertForbidden(new StringRepository(), updateMethod, "admin");

        Method readMethod = StringRepository.class.getMethod("update");
        assertAllowed(new StringRepository(), readMethod, "read");
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

    public static class UnsecuredService {
        public void operation(String value) {
        }
    }
}
