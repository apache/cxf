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
package org.apache.cxf.sts.rest.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.sts.rest.token.realm.ExtRealmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Optional.ofNullable;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static org.apache.cxf.jaxrs.utils.HttpUtils.getPathToMatch;
import static org.apache.cxf.jaxrs.utils.HttpUtils.getProtocolHeader;
import static org.apache.cxf.message.Message.HTTP_REQUEST_METHOD;
import static org.apache.cxf.message.Message.PROTOCOL_HEADERS;
import static org.springframework.util.CollectionUtils.isEmpty;

@PreMatching
@Priority(AUTHENTICATION)
public class RealmSecurityConfigurationFilter implements ContainerRequestFilter {
    public static final String REALM_NAME_PARAM = "realm";
    private static final Logger LOG = LoggerFactory.getLogger(RealmSecurityConfigurationFilter.class);

    private Map<String, Object> realmMap;

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        final Message message = JAXRSUtils.getCurrentMessage();
        final String realmName = getPathParams(message).getFirst(REALM_NAME_PARAM);

        ofNullable(realmName)
                .map(n -> realmMap.get(n.toUpperCase()))
                .map(o -> (ExtRealmProperties) o)
                .map(extRealmProperties -> extRealmProperties.getRsSecurityProperties())
                .ifPresent(map -> setRsSecurityProperties(message, map, realmName));
    }

    private void setRsSecurityProperties(final Message message, final Map<String, String> properties, final String realmName) {
        properties.entrySet().forEach(entry -> message.put(entry.getKey(), entry.getValue()));
        JAXRSUtils.getCurrentMessage().put(REALM_NAME_PARAM.toUpperCase(), realmName);
    }

    private MultivaluedMap<String, String> getPathParams(final Message message) {
        MultivaluedMap<String, String> pathParams = new MetadataMap<>();
        try {
            final List<ClassResourceInfo> resources = JAXRSUtils.getRootResources(message);
            final String rawPath = getPathToMatch(message, true);
            final Map<ClassResourceInfo, MultivaluedMap<String, String>> matchedResources = JAXRSUtils
                .selectResourceClass(resources, rawPath, message);
            final Map<String, List<String>> protocolHeaders = CastUtils.cast((Map<?, ?>) message.get(PROTOCOL_HEADERS));
            final String httpMethod = getProtocolHeader(message, HTTP_REQUEST_METHOD, POST, false);
            final List<MediaType> acceptContentTypes = JAXRSUtils.sortMediaTypes(
                getMessageHeader(message, protocolHeaders, ACCEPT), "q");
            final String requestContentType = getMessageHeader(message, protocolHeaders, CONTENT_TYPE);

            JAXRSUtils.findTargetMethod(matchedResources, message, httpMethod, pathParams, requestContentType,
                acceptContentTypes, true, true);
        } catch (Exception e) {
            LOG.warn("Exception is occurred during getting request path params", e);
        }
        return pathParams;
    }

    private String getMessageHeader(final Message message, final Map<String, List<String>> protocolHeaders,
        final String headerName) {
        String acceptTypes = null;
        if (!isEmpty(protocolHeaders.get(headerName))) {
            acceptTypes = protocolHeaders.get(headerName).get(0);
        }

        return ofNullable(acceptTypes)
                .orElse(
                        ofNullable(getProtocolHeader(message, headerName, null))
                                .orElse(WILDCARD)
                );
    }

    public Map<String, Object> getRealmMap() {
        return realmMap;
    }

    public void setRealmMap(Map<String, Object> realms) {
        this.realmMap = realms;
    }
}
