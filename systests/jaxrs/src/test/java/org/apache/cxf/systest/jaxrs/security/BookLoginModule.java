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
package org.apache.cxf.systest.jaxrs.security;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class BookLoginModule implements LoginModule {
    private static final Class<LoginModule> LOGIN_MODULE_C = getLoginModuleClass();

    private LoginModule module;
    private String fileResource;

    public BookLoginModule() {
        try {
            module = LOGIN_MODULE_C.getDeclaredConstructor().newInstance();
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
        fileResource = getClass()
            .getResource("/org/apache/cxf/systest/jaxrs/security/jetty-realm.properties")
            .toString();
    }

    @SuppressWarnings("unchecked")
    private static Class<LoginModule> getLoginModuleClass() {
        Class<?> clz = null;
        try {
            // try the jetty12 version
            clz = Class.forName("org.eclipse.jetty.security.jaas.spi.PropertyFileLoginModule",
                                           true, BookLoginModule.class.getClassLoader());
        } catch (Throwable t) {
            if (clz == null) {
                try {
                    // try the jetty8 version
                    clz = Class.forName("org.eclipse.jetty.plus.jaas.spi.PropertyFileLoginModule",
                                                   true, BookLoginModule.class.getClassLoader());
                } catch (Throwable t2) {
                    // ignore
                }
            }
        }
        return (Class<LoginModule>)clz;
    }

    public boolean abort() throws LoginException {
        return module.abort();
    }

    public boolean commit() throws LoginException {
        return module.commit();
    }

    public void initialize(Subject subject, CallbackHandler handler,
                           Map<String, ? extends Object> sharedState, Map<String, ? extends Object> options) {

        Map<String, String> customOptions = new HashMap<>();
        customOptions.put("file", fileResource);

        // See please https://github.com/eclipse/jetty.project/issues/5486
        BookLoginService.withInstance(() -> module.initialize(subject, handler, sharedState, customOptions));
    }

    public boolean login() throws LoginException {
        return module.login();
    }

    public boolean logout() throws LoginException {
        return module.logout();
    }

}
