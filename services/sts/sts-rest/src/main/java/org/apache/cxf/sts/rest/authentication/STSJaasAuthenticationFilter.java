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
package org.apache.cxf.sts.rest.authentication;

import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.jaxrs.security.JAASAuthenticationFilter;
import org.apache.cxf.sts.rest.token.realm.ExtRealmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Optional.ofNullable;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static javax.ws.rs.core.SecurityContext.BASIC_AUTH;
import static org.apache.cxf.jaxrs.utils.JAXRSUtils.getCurrentMessage;
import static org.apache.cxf.sts.rest.impl.RealmSecurityConfigurationFilter.REALM_NAME_PARAM;

@PreMatching
@Priority(AUTHENTICATION)
public class JaasAuthenticationFilter extends JAASAuthenticationFilter {
    private static final Logger LOG = LoggerFactory.getLogger(JaasAuthenticationFilter.class);
    private static final String JAAS_CONTEXT_NAME_PARAM = "rs.security.auth.jaas.context.name";
    private Map<String, Object> realmMap;

    @Override
    public void filter(final ContainerRequestContext context) {
        SecurityContext securityContext = context.getSecurityContext();
        if (securityContext.getUserPrincipal() != null) {
            LOG.debug("User principal is already set, pass filter without authentication processing");
            return;
        }

        if (!BASIC_AUTH.equals(securityContext.getAuthenticationScheme())) {
            LOG.debug("Authorization schema is not BASIC, pass filter without authentication processing");
            return;
        }

        final String realmName = (String)getCurrentMessage().get(REALM_NAME_PARAM.toUpperCase());
        final String contextName = ofNullable(realmName)
                .map(n -> realmMap.get(n.toUpperCase()))
                .map(o -> (ExtRealmProperties) o)
                .map(extRealmProperties -> (String)extRealmProperties.getRsSecurityProperty(JAAS_CONTEXT_NAME_PARAM))
                .orElse(null);

        if (ofNullable(contextName).isPresent()) {
            setRealmName(realmName);
            setContextName(contextName);
            super.filter(context);
        } else {
            LOG.debug("There is not JAAS context configured, pass filter without authentication processing");
        }
    }

    public Map<String, Object> getRealmMap() {
        return realmMap;
    }

    public void setRealmMap(Map<String, Object> realms) {
        this.realmMap = realms;
    }
}
