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
package org.apache.cxf.interceptor.security.app;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.security.Principal;

import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.interceptor.security.SecureAnnotationsInterceptor;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;

import org.junit.Test;

import static org.junit.Assert.assertThrows;

/**
 * A role annotation which is not public, in a different package from the interceptor.
 */
public class PackagePrivateAnnotationTest {

    @Test
    public void testPackagePrivateAnnotation() throws Exception {
        SecureAnnotationsInterceptor in = new SecureAnnotationsInterceptor();
        in.setAnnotationClassName(AppRoles.class.getName());
        in.setSecuredObject(new AppService());

        in.handleMessage(createMessage("admin"));
        assertThrows(AccessDeniedException.class, () -> in.handleMessage(createMessage("user")));
    }

    private static Message createMessage(String role) throws Exception {
        Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put("org.apache.cxf.resource.method", AppService.class.getMethod("update"));
        message.put(SecurityContext.class, new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> "bob";
            }

            @Override
            public boolean isUserInRole(String r) {
                return role.equals(r);
            }
        });
        return message;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD })
    @interface AppRoles {
        String[] value();
    }

    public static class AppService {
        @AppRoles("admin")
        public void update() {
        }
    }
}
