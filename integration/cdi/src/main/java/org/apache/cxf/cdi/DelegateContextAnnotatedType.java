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
package org.apache.cxf.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;

import static java.util.stream.Collectors.toSet;

final class DelegateContextAnnotatedType<X> implements AnnotatedType<X> {
    private static final Inject INJECT = new InjectLiteral();
    private static final ContextResolved CONTEXT_RESOLVED = ContextResolved.LITERAL;
    private final AnnotatedType<X> original;
    private final Set<AnnotatedField<? super X>> replacedFields;

    DelegateContextAnnotatedType(AnnotatedType<X> original) {
        this.original = original;
        this.replacedFields = replaceFields(original);
    }

    private Set<AnnotatedField<? super X>> replaceFields(AnnotatedType<? super X> delegate) {
        return delegate.getFields().stream().map(this::wrap).collect(toSet());
    }

    Set<Type> getContextFieldTypes() {
        return replacedFields.stream()
                .filter(f -> f.isAnnotationPresent(Context.class) || f.isAnnotationPresent(ContextResolved.class))
                .map(f -> f.getJavaMember().getAnnotatedType().getType())
                .collect(toSet());
    }

    @Override
    public Class<X> getJavaClass() {
        return original.getJavaClass();
    }

    @Override
    public Set<AnnotatedConstructor<X>> getConstructors() {
        return original.getConstructors();
    }

    @Override
    public Set<AnnotatedMethod<? super X>> getMethods() {
        return original.getMethods();
    }

    @Override
    public Set<AnnotatedField<? super X>> getFields() {
        return replacedFields;
    }

    @Override
    public Type getBaseType() {
        return original.getBaseType();
    }

    @Override
    public Set<Type> getTypeClosure() {
        return original.getTypeClosure();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return original.getAnnotation(annotationType);
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return original.getAnnotations();
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return original.isAnnotationPresent(annotationType);
    }

    private AnnotatedField<? super X> wrap(AnnotatedField<? super X> af) {
        if (af.isAnnotationPresent(Context.class)) {
            return new DelegateAnnotatedField<>(af);
        } else {
            return af;
        }
    }

    private final class DelegateAnnotatedField<Y> implements AnnotatedField<Y> {
        private final AnnotatedField<Y> original;
        private final Set<Annotation> annotationSet;

        private DelegateAnnotatedField(AnnotatedField<Y> delegate) {
            this.original = delegate;
            this.annotationSet = processAnnotations(delegate.getAnnotations());
        }

        private Set<Annotation> processAnnotations(Set<Annotation> annotations) {
            Set<Annotation> resultAnnotations = new LinkedHashSet<>();
            for (Annotation a : annotations) {
                if (a instanceof Context) {
                    resultAnnotations.add(INJECT);
                    resultAnnotations.add(CONTEXT_RESOLVED);
                }
                resultAnnotations.add(a);
            }
            return Collections.unmodifiableSet(resultAnnotations);
        }

        @Override
        public Field getJavaMember() {
            return original.getJavaMember();
        }

        @Override
        public boolean isStatic() {
            return original.isStatic();
        }

        @Override
        public AnnotatedType<Y> getDeclaringType() {
            return original.getDeclaringType();
        }

        @Override
        public Type getBaseType() {
            return original.getBaseType();
        }

        @Override
        public Set<Type> getTypeClosure() {
            return original.getTypeClosure();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
            for (Annotation a : annotationSet) {
                if (annotationType == a.annotationType()) {
                    return (T)a;
                }
            }
            return null;
        }

        @Override
        public Set<Annotation> getAnnotations() {
            return annotationSet;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
            return getAnnotation(annotationType) != null;
        }
    }

    private static final class InjectLiteral extends AnnotationLiteral<Inject> implements Inject {
        private static final long serialVersionUID = 1L;

    }
}
