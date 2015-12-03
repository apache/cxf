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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.ws.rs.BeanParam;
import javax.ws.rs.MatrixParam;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import io.swagger.converter.ModelConverters;
import io.swagger.jaxrs.ext.AbstractSwaggerExtension;
import io.swagger.jaxrs.ext.SwaggerExtension;
import io.swagger.jaxrs.ext.SwaggerExtensions;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.AbstractNumericProperty;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.Json;
import io.swagger.util.ParameterProcessor;

public class JaxRs2Extension extends AbstractSwaggerExtension {

    private final ObjectMapper mapper = Json.mapper();

    @Override
    public List<Parameter> extractParameters(
            final List<Annotation> annotations,
            final Type type,
            final Set<Type> typesToSkip,
            final Iterator<SwaggerExtension> chain) {

        if (shouldIgnoreType(type, typesToSkip)) {
            return new ArrayList<Parameter>();
        }

        List<Parameter> parameters = new ArrayList<Parameter>();
        for (Annotation annotation : annotations) {
            if (annotation instanceof MatrixParam) {
                MatrixParam param = (MatrixParam) annotation;
                MatrixParameter mp = new MatrixParameter().name(param.value());

                Property schema = createProperty(type);
                if (schema != null) {
                    mp.setProperty(schema);
                }
                parameters.add(mp);
            } else if (annotation instanceof BeanParam) {
                // Use Jackson's logic for processing Beans
                final BeanDescription beanDesc = mapper.getSerializationConfig().introspect(constructType(type));
                final List<BeanPropertyDefinition> properties = beanDesc.findProperties();

                for (final BeanPropertyDefinition propDef : properties) {
                    final AnnotatedField field = propDef.getField();
                    final AnnotatedMethod setter = propDef.getSetter();
                    final List<Annotation> paramAnnotations = new ArrayList<Annotation>();
                    final Iterator<SwaggerExtension> extensions = SwaggerExtensions.chain();
                    Type paramType = null;

                    // Gather the field's details
                    if (field != null) {
                        paramType = field.getGenericType();

                        for (final Annotation fieldAnnotation : field.annotations()) {
                            if (!paramAnnotations.contains(fieldAnnotation)) {
                                paramAnnotations.add(fieldAnnotation);
                            }
                        }
                    }

                    // Gather the setter's details but only the ones we need
                    if (setter != null) {
                        // Do not set the param class/type from the setter if the values are already identified
                        if (paramType == null && setter.getGenericParameterTypes() != null) {
                            paramType = setter.getGenericParameterTypes()[0];
                        }

                        for (final Annotation fieldAnnotation : setter.annotations()) {
                            if (!paramAnnotations.contains(fieldAnnotation)) {
                                paramAnnotations.add(fieldAnnotation);
                            }
                        }
                    }

                    // Re-process all Bean fields and let the default swagger-jaxrs processor do its thing
                    List<Parameter> extracted =
                            extensions.next().extractParameters(paramAnnotations, paramType, typesToSkip, extensions);

                    // since downstream processors won't know how to introspect @BeanParam, process here
                    for (Parameter param : extracted) {
                        if (ParameterProcessor.applyAnnotations(null, param, paramType, paramAnnotations) != null) {
                            applyBeanValidatorAnnotations(param, paramAnnotations);
                            parameters.add(param);
                        }
                    }
                }
            }
        }

        // Only call down to the other items in the chain if no parameters were produced
        if (parameters.isEmpty()) {
            parameters = super.extractParameters(annotations, type, typesToSkip, chain);
        }

        return parameters;
    }

    private Property createProperty(final Type type) {
        return enforcePrimitive(ModelConverters.getInstance().readAsProperty(type), 0);
    }

    private Property enforcePrimitive(final Property in, final int level) {
        if (in instanceof RefProperty) {
            return new StringProperty();
        }
        if (in instanceof ArrayProperty) {
            if (level == 0) {
                final ArrayProperty array = (ArrayProperty) in;
                array.setItems(enforcePrimitive(array.getItems(), level + 1));
            } else {
                return new StringProperty();
            }
        }
        return in;
    }

    /**
     * This is essentially a duplicate of {@link io.swagger.jackson.ModelResolver.applyBeanValidatorAnnotations}.
     *
     * @param property
     * @param annotations
     */
    private void applyBeanValidatorAnnotations(final Parameter property, final List<Annotation> annotations) {
        Map<String, Annotation> annos = new HashMap<String, Annotation>();
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                annos.put(annotation.annotationType().getName(), annotation);
            }
        }
        if (annos.containsKey(NotNull.class.getName())) {
            property.setRequired(true);
        }
        if (annos.containsKey(Min.class.getName()) && property instanceof AbstractNumericProperty) {
            Min min = (Min) annos.get(Min.class.getName());
            AbstractNumericProperty ap = (AbstractNumericProperty) property;
            ap.setMinimum(new Double(min.value()));
        }
        if (annos.containsKey(Max.class.getName()) && property instanceof AbstractNumericProperty) {
            Max max = (Max) annos.get(Max.class.getName());
            AbstractNumericProperty ap = (AbstractNumericProperty) property;
            ap.setMaximum(new Double(max.value()));
        }
        if (annos.containsKey(Size.class.getName())) {
            Size size = (Size) annos.get(Size.class.getName());
            if (property instanceof AbstractNumericProperty) {
                AbstractNumericProperty ap = (AbstractNumericProperty) property;
                ap.setMinimum(new Double(size.min()));
                ap.setMaximum(new Double(size.max()));
            } else if (property instanceof StringProperty) {
                StringProperty sp = (StringProperty) property;
                sp.minLength(size.min());
                sp.maxLength(size.max());
            } else if (property instanceof ArrayProperty) {
                ArrayProperty sp = (ArrayProperty) property;
                sp.setMinItems(size.min());
                sp.setMaxItems(size.max());
            }
        }
        if (annos.containsKey(DecimalMin.class.getName())) {
            DecimalMin min = (DecimalMin) annos.get(DecimalMin.class.getName());
            if (property instanceof AbstractNumericProperty) {
                AbstractNumericProperty ap = (AbstractNumericProperty) property;
                if (min.inclusive()) {
                    ap.setMinimum(new Double(min.value()));
                } else {
                    ap.setExclusiveMinimum(!min.inclusive());
                }
            }
        }
        if (annos.containsKey(DecimalMax.class.getName())) {
            DecimalMax max = (DecimalMax) annos.get(DecimalMax.class.getName());
            if (property instanceof AbstractNumericProperty) {
                AbstractNumericProperty ap = (AbstractNumericProperty) property;
                if (max.inclusive()) {
                    ap.setMaximum(new Double(max.value()));
                } else {
                    ap.setExclusiveMaximum(!max.inclusive());
                }
            }
        }
        if (annos.containsKey(Pattern.class.getName())) {
            Pattern pattern = (Pattern) annos.get(Pattern.class.getName());
            if (property instanceof StringProperty) {
                StringProperty ap = (StringProperty) property;
                ap.setPattern(pattern.regexp());
            }
        }
    }

}
