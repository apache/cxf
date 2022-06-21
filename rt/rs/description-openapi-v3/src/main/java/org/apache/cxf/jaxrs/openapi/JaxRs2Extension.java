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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.MatrixParam;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.jaxrs2.ResolvedParameter;
import io.swagger.v3.jaxrs2.ext.AbstractOpenAPIExtension;
import io.swagger.v3.jaxrs2.ext.OpenAPIExtension;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;

/**
 * Adds matrix parameters support.
 */
public class JaxRs2Extension extends AbstractOpenAPIExtension {

    @Override
    public ResolvedParameter extractParameters(
            final List<Annotation> annotations,
            final Type type,
            final Set<Type> typesToSkip,
            final Components components,
            final Consumes classConsumes,
            final Consumes methodConsumes,
            final boolean includeRequestBody,
            final JsonView jsonViewAnnotation,
            final Iterator<OpenAPIExtension> chain) {

        if (shouldIgnoreType(type, typesToSkip)) {
            return new ResolvedParameter();
        }

        List<Parameter> parameters = annotations.stream().
                filter(annotation -> annotation instanceof MatrixParam).
                map(annotation -> {
                    MatrixParam param = (MatrixParam) annotation;
                    Parameter mp = new PathParameter().name(param.value());
                    mp.setStyle(Parameter.StyleEnum.MATRIX);

                    ResolvedSchema resolvedSchema = ModelConverters.getInstance().readAllAsResolvedSchema(type);
                    if (resolvedSchema != null) {
                        mp.setSchema(resolvedSchema.schema);
                    }
                    applyBeanValidatorAnnotations(mp, annotations);

                    return mp;
                }).collect(Collectors.toList());

        // Only call down to the other items in the chain if no parameters were produced
        if (parameters.isEmpty()) {
            return super.extractParameters(
                    annotations, type, typesToSkip, components, classConsumes,
                    methodConsumes, includeRequestBody, jsonViewAnnotation, chain);
        }

        ResolvedParameter resolved = new ResolvedParameter();
        resolved.parameters = parameters;
        return resolved;
    }

    /**
     * This is mostly a duplicate of {@link io.swagger.v3.core.jackson.ModelResolver#applyBeanValidatorAnnotations}.
     *
     * @param parameter
     * @param annotations
     */
    private void applyBeanValidatorAnnotations(final Parameter parameter, final List<Annotation> annotations) {
        Map<String, Annotation> annos = new HashMap<>();
        if (annotations != null) {
            annotations.forEach(annotation -> {
                annos.put(annotation.annotationType().getName(), annotation);
            });
        }

        if (annos.containsKey(NotNull.class.getName())) {
            parameter.setRequired(true);
        }

        Schema<?> schema = parameter.getSchema();

        if (annos.containsKey(Min.class.getName())) {
            Min min = (Min) annos.get(Min.class.getName());
            schema.setMinimum(BigDecimal.valueOf(min.value()));
        }
        if (annos.containsKey(Max.class.getName())) {
            Max max = (Max) annos.get(Max.class.getName());
            schema.setMaximum(BigDecimal.valueOf(max.value()));
        }
        if (annos.containsKey(Size.class.getName())) {
            Size size = (Size) annos.get(Size.class.getName());

            schema.setMinimum(BigDecimal.valueOf(size.min()));
            schema.setMaximum(BigDecimal.valueOf(size.max()));

            schema.setMinItems(size.min());
            schema.setMaxItems(size.max());
        }
        if (annos.containsKey(DecimalMin.class.getName())) {
            DecimalMin min = (DecimalMin) annos.get(DecimalMin.class.getName());
            if (min.inclusive()) {
                schema.setMinimum(BigDecimal.valueOf(Double.valueOf(min.value())));
            } else {
                schema.setExclusiveMinimum(!min.inclusive());
            }
        }
        if (annos.containsKey(DecimalMax.class.getName())) {
            DecimalMax max = (DecimalMax) annos.get(DecimalMax.class.getName());
            if (max.inclusive()) {
                schema.setMaximum(BigDecimal.valueOf(Double.valueOf(max.value())));
            } else {
                schema.setExclusiveMaximum(!max.inclusive());
            }
        }
        if (annos.containsKey(Pattern.class.getName())) {
            Pattern pattern = (Pattern) annos.get(Pattern.class.getName());
            schema.setPattern(pattern.regexp());
        }
    }

}
