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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.doc.DocumentationProvider;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;

public class Swagger2Serializers extends SwaggerSerializers {

    private final boolean dynamicBasePath;

    private final boolean replaceTags;

    private final DocumentationProvider javadocProvider;

    private final List<ClassResourceInfo> cris;

    public Swagger2Serializers(
            final boolean dynamicBasePath,
            final boolean replaceTags,
            final DocumentationProvider javadocProvider,
            final List<ClassResourceInfo> cris) {

        super();

        this.dynamicBasePath = dynamicBasePath;
        this.replaceTags = replaceTags;
        this.javadocProvider = javadocProvider;
        this.cris = cris;
    }

    @Override
    public void writeTo(
            final Swagger data,
            final Class<?> type,
            final Type genericType,
            final Annotation[] annotations,
            final MediaType mediaType,
            final MultivaluedMap<String, Object> headers,
            final OutputStream out) throws IOException {

        if (dynamicBasePath) {
            MessageContext ctx = JAXRSUtils.createContextValue(
                    JAXRSUtils.getCurrentMessage(), null, MessageContext.class);
            data.setBasePath(StringUtils.substringBeforeLast(ctx.getHttpServletRequest().getRequestURI(), "/"));
        }

        if (replaceTags || javadocProvider != null) {
            Map<String, ClassResourceInfo> operations = new HashMap<>();
            Map<Pair<String, String>, OperationResourceInfo> methods = new HashMap<>();
            for (ClassResourceInfo cri : cris) {
                for (OperationResourceInfo ori : cri.getMethodDispatcher().getOperationResourceInfos()) {
                    StringBuilder fullPath = new StringBuilder().
                            append(cri.getURITemplate().getValue()).
                            append(ori.getURITemplate().getValue());
                    if (fullPath.charAt(fullPath.length() - 1) == '/') {
                        fullPath.setLength(fullPath.length() - 1);
                    }
                    // Adapt to Swagger's path expression
                    if (fullPath.toString().endsWith(":.*}")) {
                        fullPath.setLength(fullPath.length() - 4);
                        fullPath.append('}');
                    }

                    operations.put(fullPath.toString(), cri);
                    methods.put(ImmutablePair.of(ori.getHttpMethod(), fullPath.toString()), ori);
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
                    tag.setName(cri.getURITemplate().getValue());
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

                        if (subentry.getValue().getResponses() != null
                                && !subentry.getValue().getResponses().isEmpty()) {

                            subentry.getValue().getResponses().entrySet().iterator().next().getValue().
                                    setDescription(javadocProvider.getMethodResponseDoc(ori));
                        }
                    }
                }
            }
        }

        super.writeTo(data, type, genericType, annotations, mediaType, headers, out);
    }
}
