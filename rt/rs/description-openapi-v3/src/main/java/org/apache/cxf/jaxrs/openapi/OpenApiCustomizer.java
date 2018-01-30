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
package org.apache.cxf.jaxrs.openapi;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.doc.DocumentationProvider;
import org.apache.cxf.jaxrs.model.doc.JavaDocProvider;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

public class OpenApiCustomizer {

    protected boolean dynamicBasePath;

    protected boolean replaceTags;

    protected DocumentationProvider javadocProvider;

    protected List<ClassResourceInfo> cris;

    public OpenAPIConfiguration customize(final OpenAPIConfiguration configuration) {
        if (configuration == null) {
            return configuration;
        }

        if (dynamicBasePath) {
            final MessageContext ctx = createMessageContext();
            final String url = StringUtils.substringBeforeLast(ctx.getUriInfo().getRequestUri().toString(), "/");

            final Collection<Server> servers = configuration.getOpenAPI().getServers();
            if (servers == null || servers.stream().noneMatch(s -> s.getUrl().equalsIgnoreCase(url))) {
                configuration.getOpenAPI().setServers(Collections.singletonList(new Server().url(url)));
            }
        }

        return configuration;
    }

    protected MessageContext createMessageContext() {
        return JAXRSUtils.createContextValue(JAXRSUtils.getCurrentMessage(), null, MessageContext.class);
    }

    public void customize(final OpenAPI oas) {
        if (replaceTags || javadocProvider != null) {
            Map<String, ClassResourceInfo> operations = new HashMap<>();
            Map<Pair<String, String>, OperationResourceInfo> methods = new HashMap<>();
            cris.forEach(cri -> {
                cri.getMethodDispatcher().getOperationResourceInfos().forEach(ori -> {
                    String normalizedPath = getNormalizedPath(
                            cri.getURITemplate().getValue(), ori.getURITemplate().getValue());

                    operations.put(normalizedPath, cri);
                    methods.put(Pair.of(ori.getHttpMethod(), normalizedPath), ori);
                });
            });

            List<Tag> tags = new ArrayList<>();
            oas.getPaths().entrySet().forEach(entry -> {
                Tag tag = null;
                if (replaceTags && operations.containsKey(entry.getKey())) {
                    ClassResourceInfo cri = operations.get(entry.getKey());

                    tag = new Tag();
                    tag.setName(cri.getURITemplate().getValue().replaceAll("/", "_"));
                    if (javadocProvider != null) {
                        tag.setDescription(javadocProvider.getClassDoc(cri));
                    }

                    if (!tags.contains(tag)) {
                        tags.add(tag);
                    }
                }

                for (Map.Entry<HttpMethod, Operation> subentry : entry.getValue().readOperationsMap().entrySet()) {
                    if (replaceTags && tag != null) {
                        subentry.getValue().setTags(Collections.singletonList(tag.getName()));
                    }

                    Pair<String, String> key = Pair.of(subentry.getKey().name(), entry.getKey());
                    if (methods.containsKey(key) && javadocProvider != null) {
                        OperationResourceInfo ori = methods.get(key);

                        subentry.getValue().setSummary(javadocProvider.getMethodDoc(ori));
                        if (subentry.getValue().getParameters() == null) {
                            List<Parameter> parameters = new ArrayList<>();
                            addParameters(parameters);
                            subentry.getValue().setParameters(parameters);
                        } else {
                            for (int i = 0; i < subentry.getValue().getParameters().size(); i++) {
                                subentry.getValue().getParameters().get(i).
                                        setDescription(javadocProvider.getMethodParameterDoc(ori, i));
                            }
                            addParameters(subentry.getValue().getParameters());
                        }

                        if (subentry.getValue().getResponses() != null
                                && !subentry.getValue().getResponses().isEmpty()) {

                            subentry.getValue().getResponses().entrySet().iterator().next().getValue().
                                    setDescription(javadocProvider.getMethodResponseDoc(ori));
                        }
                    }
                }
            });
            if (replaceTags && oas.getTags() != null) {
                oas.setTags(tags);
            }
        }
    }

    protected String getNormalizedPath(String classResourcePath, String operationResourcePath) {
        StringBuilder normalizedPath = new StringBuilder();

        String[] segments = StringUtils.split(classResourcePath + operationResourcePath, "/");
        for (String segment : segments) {
            if (!StringUtils.isEmpty(segment)) {
                normalizedPath.append("/").append(segment);
            }
        }
        // Adapt to Swagger's path expression
        if (normalizedPath.toString().endsWith(":.*}")) {
            normalizedPath.setLength(normalizedPath.length() - 4);
            normalizedPath.append('}');
        }
        return StringUtils.EMPTY.equals(normalizedPath.toString()) ? "/" : normalizedPath.toString();
    }

    /**
     * Allows to add parameters to the list, related to an {@link Operation} instance; the method is invoked
     * for all instances available.
     *
     * @param parameters list of parameters defined for an {@link Operation}
     * @see io.swagger.v3.oas.models.parameters.HeaderParameter
     * @see io.swagger.v3.oas.models.parameters.CookieParameter
     * @see io.swagger.v3.oas.models.parameters.PathParameter
     * @see io.swagger.v3.oas.models.parameters.QueryParameter
     */
    protected void addParameters(final List<Parameter> parameters) {
        // does nothing by default
    }

    public void setDynamicBasePath(final boolean dynamicBasePath) {
        this.dynamicBasePath = dynamicBasePath;
    }

    public void setReplaceTags(final boolean replaceTags) {
        this.replaceTags = replaceTags;
    }

    public void setJavadocProvider(final DocumentationProvider javadocProvider) {
        this.javadocProvider = javadocProvider;
    }

    public void setClassResourceInfos(final List<ClassResourceInfo> classResourceInfos) {
        this.cris = classResourceInfos;
    }

    public void setJavaDocPath(final String javaDocPath) throws Exception {
        this.javadocProvider = new JavaDocProvider(javaDocPath);
    }

    public void setJavaDocPaths(final String... javaDocPaths) throws Exception {
        this.javadocProvider = new JavaDocProvider(javaDocPaths);
    }

    public void setJavaDocURLs(final URL[] javaDocURLs) {
        this.javadocProvider = new JavaDocProvider(javaDocURLs);
    }

}
