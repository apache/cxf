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
import java.net.URL;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.doc.DocumentationProvider;
import org.apache.cxf.jaxrs.model.doc.JavaDocProvider;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Swagger;

public class DefaultSwagger2Serializers extends SwaggerSerializers implements Swagger2Serializers {
    private Swagger2Customizer customizer = new Swagger2Customizer();
    
    @Override
    public void writeTo(
            final Swagger data,
            final Class<?> type,
            final Type genericType,
            final Annotation[] annotations,
            final MediaType mediaType,
            final MultivaluedMap<String, Object> headers,
            final OutputStream out) throws IOException {
        Swagger customizedData = customizer.customize(data);
        super.writeTo(customizedData, type, genericType, annotations, mediaType, headers, out);
    }

    @Override
    public void setDynamicBasePath(final boolean dynamicBasePath) {
        customizer.setDynamicBasePath(dynamicBasePath);
    }

    public void setReplaceTags(final boolean replaceTags) {
        customizer.setReplaceTags(replaceTags);
    }

    public void setJavadocProvider(final DocumentationProvider javadocProvider) {
        customizer.setJavadocProvider(javadocProvider);
    }

    @Override
    public void setClassResourceInfos(final List<ClassResourceInfo> classResourceInfos) {
        customizer.setClassResourceInfos(classResourceInfos);
    }

    public void setJavaDocPath(final String javaDocPath) throws Exception {
        customizer.setJavadocProvider(new JavaDocProvider(javaDocPath));
    }

    public void setJavaDocPaths(final String... javaDocPaths) throws Exception {
        customizer.setJavadocProvider(new JavaDocProvider(javaDocPaths));
    }

    public void setJavaDocURLs(final URL[] javaDocURLs) {
        customizer.setJavadocProvider(new JavaDocProvider(javaDocURLs));
    }

    @Override
    public void setBeanConfig(BeanConfig beanConfig) {
        customizer.setBeanConfig(beanConfig);

    }

    public Swagger2Customizer getCustomizer() {
        return customizer;
    }

    public void setCustomizer(Swagger2Customizer customizer) {
        this.customizer = customizer;
    }
}
