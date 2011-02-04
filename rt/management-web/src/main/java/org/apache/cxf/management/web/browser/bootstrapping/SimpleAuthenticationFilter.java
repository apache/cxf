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

package org.apache.cxf.management.web.browser.bootstrapping;

import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.Validate;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;

@Provider
public class SimpleAuthenticationFilter extends AbstractAuthenticationFilter {
    private static final Logger LOGGER = LogUtils.getL7dLogger(SimpleAuthenticationFilter.class);

    private Map<String, String> authData;

    public SimpleAuthenticationFilter(final Map<String, String> authData) {
        Validate.notNull(authData, "authData is null");
        this.authData = authData;
    }

    @Override
    protected boolean authenticate(Message m, ClassResourceInfo resourceClass) {
        assert authData != null;
        AuthorizationPolicy policy = (AuthorizationPolicy) m.get(AuthorizationPolicy.class);
        if (policy == null) {
            LOGGER.fine("No authentication data'");
            return false;
        } else if (isValid(policy)) {
            LOGGER.fine(String.format("Successful authentication, username='%s'", policy.getUserName()));
            return true;
        } else {
            LOGGER.fine(String.format("Failed authentication, username='%s'", policy.getUserName()));
            return false;
        }
    }

    private boolean isValid(final AuthorizationPolicy policy) {
        return authData.containsKey(policy.getUserName())
            && authData.get(policy.getUserName()) != null
            && authData.get(policy.getUserName()).equals(policy.getPassword());
    }
}
