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
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.ApplicationPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.doc.DocumentationProvider;
import org.apache.cxf.jaxrs.model.doc.JavaDocProvider;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

public class OpenApiCustomizer {

    protected boolean dynamicBasePath;

    protected boolean replaceTags;

    protected DocumentationProvider javadocProvider;

    protected List<ClassResourceInfo> cris;

    protected String applicationPath;

    public OpenAPIConfiguration customize(final OpenAPIConfiguration configuration) {
        if (configuration == null) {
            return configuration;
        }

        if (dynamicBasePath) {
            final MessageContext ctx = createMessageContext();

            // If the JAX-RS application with custom path is defined, it might be present twice, in the 
            // request URI as well as in each resource operation URI. To properly represent server URL, 
            // the application path should be removed from it.
            final String url = StringUtils.removeEnd(
                    StringUtils.substringBeforeLast(ctx.getUriInfo().getRequestUri().toString(), "/"),
                    applicationPath);

            final Collection<Server> servers = configuration.getOpenAPI().getServers();
            if (servers == null || servers.stream().noneMatch(s -> s.getUrl().equalsIgnoreCase(url))) {
                configuration.getOpenAPI().setServers(Collections.singletonList(new Server().url(url)));
            }
        }

        return configuration;
    }

    protected String extractJavadoc(final Operation operation, final OperationResourceInfo ori, final int paramIdx) {
        String javadoc = null;
        if (operation.getParameters().size() == ori.getParameters().size()) {
            javadoc = javadocProvider.getMethodParameterDoc(ori, paramIdx);
        } else {
            for (int j = 0; j < ori.getParameters().size(); j++) {
                if (Objects.equals(
                        operation.getParameters().get(paramIdx).getName(),
                        ori.getParameters().get(j).getName())) {

                    javadoc = javadocProvider.getMethodParameterDoc(ori, j);
                }
            }
        }
        return javadoc;
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
            oas.getPaths().forEach((pathKey, pathItem) -> {
                Optional<Tag> tag;
                if (replaceTags && operations.containsKey(pathKey)) {
                    ClassResourceInfo cri = operations.get(pathKey);

                    tag = Optional.of(new Tag());
                    tag.get().setName(cri.getURITemplate().getValue().replaceAll("/", "_"));
                    if (javadocProvider != null) {
                        tag.get().setDescription(javadocProvider.getClassDoc(cri));
                    }

                    if (!tags.contains(tag.get())) {
                        tags.add(tag.get());
                    }
                } else {
                    tag = Optional.empty();
                }

                pathItem.readOperationsMap().forEach((method, operation) -> {
                    if (replaceTags && tag.isPresent()) {
                        operation.setTags(Collections.singletonList(tag.get().getName()));
                    }

                    Pair<String, String> key = Pair.of(method.name(), pathKey);
                    if (methods.containsKey(key) && javadocProvider != null) {
                        OperationResourceInfo ori = methods.get(key);

                        if (StringUtils.isBlank(operation.getSummary())) {
                            operation.setSummary(javadocProvider.getMethodDoc(ori));
                        }

                        if (operation.getParameters() == null) {
                            List<Parameter> parameters = new ArrayList<>();
                            addParameters(parameters);
                            operation.setParameters(parameters);
                        }

                        for (int i = 0; i < operation.getParameters().size(); i++) {
                            if (StringUtils.isBlank(operation.getParameters().get(i).getDescription())) {
                                operation.getParameters().get(i).setDescription(extractJavadoc(operation, ori, i));
                            }
                        }

                        addParameters(operation.getParameters());

                        customizeResponses(operation, ori);
                    }
                });
            });
            if (replaceTags && oas.getTags() != null) {
                oas.setTags(tags);
            }
        }
    }

    protected String getNormalizedPath(String classResourcePath, String operationResourcePath) {
        StringBuilder normalizedPath = new StringBuilder();

        String[] segments = (classResourcePath + operationResourcePath).split("/");
        for (String segment : segments) {
            if (!StringUtils.isEmpty(segment)) {
                normalizedPath.append('/').append(segment);
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

    /**
     * Allows to customize the responses of the given {@link Operation} instance; the method is invoked
     * for all instances available.
     *
     * @param operation operation instance
     * @param ori CXF data about the given operation instance
     */
    protected void customizeResponses(final Operation operation, final OperationResourceInfo ori) {
        if (operation.getResponses() != null && !operation.getResponses().isEmpty()) {
            ApiResponse response = operation.getResponses().entrySet().iterator().next().getValue();
            if (StringUtils.isBlank(response.getDescription())
                    || (StringUtils.isNotBlank(javadocProvider.getMethodResponseDoc(ori))
                    && Reader.DEFAULT_DESCRIPTION.equals(response.getDescription()))) {

                response.setDescription(javadocProvider.getMethodResponseDoc(ori));
            }
        }
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

    public void setApplicationInfo(ApplicationInfo application) {
        if (application != null && application.getProvider() != null) {
            final Class<?> clazz = application.getProvider().getClass();
            final ApplicationPath path = ResourceUtils.locateApplicationPath(clazz);

            if (path != null) {
                applicationPath = path.value();

                if (!applicationPath.startsWith("/")) {
                    applicationPath = "/" + applicationPath;
                }

                if (applicationPath.endsWith("/")) {
                    applicationPath = applicationPath.substring(0, applicationPath.lastIndexOf('/'));
                }
            }
        }
    }

    protected MessageContext createMessageContext() {
        return JAXRSUtils.createContextValue(JAXRSUtils.getCurrentMessage(), null, MessageContext.class);
    }
}
