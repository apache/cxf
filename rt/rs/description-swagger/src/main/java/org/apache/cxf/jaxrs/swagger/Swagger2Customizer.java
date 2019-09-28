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
package org.apache.cxf.jaxrs.swagger;

import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.doc.DocumentationProvider;
import org.apache.cxf.jaxrs.model.doc.JavaDocProvider;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.Parameter;

public class Swagger2Customizer {

    protected boolean dynamicBasePath;

    protected boolean replaceTags;

    protected boolean applyDefaultVersion = true;

    protected DocumentationProvider javadocProvider;

    protected List<ClassResourceInfo> cris;

    protected BeanConfig beanConfig;

    public Swagger customize(Swagger data) {

        if (dynamicBasePath) {
            MessageContext ctx = createMessageContext();
            String currentBasePath = StringUtils.substringBeforeLast(ctx.getHttpServletRequest().getRequestURI(), "/");
            data.setBasePath(currentBasePath);
            if (data.getHost() == null) {
                data.setHost(beanConfig.getHost());
            }
            if (data.getInfo() == null) {
                data.setInfo(beanConfig.getInfo());
            }

            if (beanConfig.getSwagger() != null
                && beanConfig.getSwagger().getSecurityDefinitions() != null
                && data.getSecurityDefinitions() == null) {
                data.setSecurityDefinitions(beanConfig.getSwagger().getSecurityDefinitions());
            }
        }

        if (replaceTags || javadocProvider != null) {
            Map<String, ClassResourceInfo> operations = new HashMap<>();
            Map<Pair<String, String>, OperationResourceInfo> methods = new HashMap<>();
            for (ClassResourceInfo cri : cris) {
                for (OperationResourceInfo ori : cri.getMethodDispatcher().getOperationResourceInfos()) {
                    String normalizedPath = getNormalizedPath(
                            cri.getURITemplate().getValue(), ori.getURITemplate().getValue());

                    operations.put(normalizedPath, cri);
                    methods.put(ImmutablePair.of(ori.getHttpMethod(), normalizedPath), ori);
                }
            }

            if (replaceTags && data.getTags() != null) {
                data.getTags().clear();
            }
            for (final Map.Entry<String, Path> entry : data.getPaths().entrySet()) {
                Tag tag = null;
                if (replaceTags && operations.containsKey(entry.getKey())) {
                    ClassResourceInfo cri = operations.get(entry.getKey());

                    tag = new Tag();
                    tag.setName(cri.getURITemplate().getValue().replaceAll("/", "_"));
                    if (javadocProvider != null) {
                        tag.setDescription(javadocProvider.getClassDoc(cri));
                    }

                    data.addTag(tag);
                }

                for (Map.Entry<HttpMethod, Operation> subentry : entry.getValue().getOperationMap().entrySet()) {
                    if (replaceTags && tag != null) {
                        subentry.getValue().setTags(Collections.singletonList(tag.getName()));
                    }

                    Pair<String, String> key = ImmutablePair.of(subentry.getKey().name(), entry.getKey());
                    if (methods.containsKey(key) && javadocProvider != null) {
                        OperationResourceInfo ori = methods.get(key);

                        subentry.getValue().setSummary(javadocProvider.getMethodDoc(ori));
                        for (int i = 0; i < subentry.getValue().getParameters().size(); i++) {
                            subentry.getValue().getParameters().get(i).
                                    setDescription(javadocProvider.getMethodParameterDoc(ori, i));
                        }
                        addParameters(subentry.getValue().getParameters());

                        if (subentry.getValue().getResponses() != null
                                && !subentry.getValue().getResponses().isEmpty()) {

                            subentry.getValue().getResponses().entrySet().iterator().next().getValue().
                                    setDescription(javadocProvider.getMethodResponseDoc(ori));
                        }
                    }
                }
            }
        }
        if (replaceTags && data.getTags() != null) {
            Collections.sort(data.getTags(), new Comparator<Tag>() {

                @Override
                public int compare(final Tag tag1, final Tag tag2) {
                    return tag1.getName().compareTo(tag2.getName());
                }
            });
        }
        applyDefaultVersion(data);
        return data;
    }

    private MessageContext createMessageContext() {
        return JAXRSUtils.createContextValue(
                               JAXRSUtils.getCurrentMessage(), null, MessageContext.class);
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

    protected void applyDefaultVersion(Swagger data) {
        if (applyDefaultVersion && data.getInfo() != null && data.getInfo().getVersion() == null
                && beanConfig != null && beanConfig.getResourcePackage() != null) {
            Package resourcePackage = Package.getPackage(beanConfig.getResourcePackage());
            if (resourcePackage != null) {
                data.getInfo().setVersion(resourcePackage.getImplementationVersion());
            }
        }
    }

    /**
     * Allows to add parameters to the list, related to an {@link Operation} instance; the method is invoked
     * for all instances available.
     *
     * @param parameters list of parameters defined for an {@link Operation}
     * @see io.swagger.models.parameters.HeaderParameter
     * @see io.swagger.models.parameters.CookieParameter
     * @see io.swagger.models.parameters.PathParameter
     * @see io.swagger.models.parameters.BodyParameter
     * @see io.swagger.models.parameters.QueryParameter
     * @see io.swagger.models.parameters.RefParameter
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

    public void setBeanConfig(BeanConfig beanConfig) {
        this.beanConfig = beanConfig;
    }

    public void setApplyDefaultVersion(boolean applyDefaultVersion) {
        this.applyDefaultVersion = applyDefaultVersion;
    }
}
