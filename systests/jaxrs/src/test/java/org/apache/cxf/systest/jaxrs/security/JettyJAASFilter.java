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

import java.lang.reflect.InvocationTargetException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.interceptor.security.NamePasswordCallbackHandler;
import org.apache.cxf.jaxrs.security.JAASAuthenticationFilter;

public class JettyJAASFilter extends JAASAuthenticationFilter {
    @Override
    protected CallbackHandler getCallbackHandler(final String name, final String password) {
        return new NamePasswordCallbackHandler(name, password) {
            protected boolean handleCallback(Callback c) {
                if ("ObjectCallback".equals(c.getClass().getSimpleName())) {
                    try {
                        c.getClass().getMethod("setObject", Object.class).invoke(c, password);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                }
                return false;
            }
        };
    }

}
