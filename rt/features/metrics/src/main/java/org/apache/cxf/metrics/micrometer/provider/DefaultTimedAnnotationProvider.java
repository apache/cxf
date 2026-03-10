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

package org.apache.cxf.metrics.micrometer.provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.MessageUtils;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.annotation.TimedSet;

import static java.util.Collections.emptySet;

public class DefaultTimedAnnotationProvider implements TimedAnnotationProvider {

    private final ConcurrentHashMap<HandlerMethod, Set<Timed>> timedAnnotationCache = new ConcurrentHashMap<>();

    @Override
    public Set<Timed> getTimedAnnotations(Exchange ex, boolean client) {
        final HandlerMethod handlerMethod = HandlerMethod.create(ex, client);
        if (handlerMethod == null) {
            return emptySet();
        }
        
        final Set<Timed> exists = timedAnnotationCache.get(handlerMethod);
        if (exists != null) {
            return exists;
        }

        return timedAnnotationCache.computeIfAbsent(handlerMethod, method -> {
            Set<Timed> timed = findTimedAnnotations(method.getMethod());
            if (timed.isEmpty()) {
                timed = findTimedAnnotations(method.getBeanType());
                if (timed.isEmpty()) {
                    return emptySet();
                }
            }
            return timed;
        });
    }
    
    @Override
    public Optional<String> getDefaultMetricName(Exchange ex, boolean client) {
        final HandlerMethod handlerMethod = HandlerMethod.create(ex, client);
        if (handlerMethod == null) {
            return Optional.empty();
        } else {
            return Optional.of(handlerMethod.method.getName());
        }
    }

    Set<Timed> findTimedAnnotations(AnnotatedElement element) {
        Set<Annotation> foundAnnotations = new HashSet<>();

        findAllAnnotations(element, foundAnnotations);

        Stream<Timed> timedAnnotations = foundAnnotations.stream()
                .filter(annotation -> annotation.annotationType().equals(Timed.class))
                .map(annotation -> (Timed) annotation);

        Stream<Timed> timedAnnotationsFromTimedSet = foundAnnotations.stream()
                .filter(annotation -> annotation.annotationType().equals(TimedSet.class))
                .map(annotation -> (TimedSet) annotation)
                .map(TimedSet::value)
                .flatMap(Arrays::stream);

        return Stream.concat(timedAnnotations, timedAnnotationsFromTimedSet).collect(Collectors.toSet());
    }

    private static void findAllAnnotations(final AnnotatedElement annotatedElement,
                                           final Set<Annotation> foundAnnotations) {
        for (Annotation annotation : annotatedElement.getDeclaredAnnotations()) {
            if (!foundAnnotations.contains(annotation)) {
                foundAnnotations.add(annotation);
                findAllAnnotations(annotation.annotationType(), foundAnnotations);
            }
        }
    }

    private static final class HandlerMethod {
        private final Class<?> beanType;
        private final Method method;

        private HandlerMethod(Class<?> beanType, Method method) {
            this.method = method;
            this.beanType = beanType;
        }
        
        private static HandlerMethod create(Exchange exchange, boolean client) {
            return MessageUtils
                .getTargetMethod(client ? exchange.getOutMessage() : exchange.getInMessage())
                .map(method -> new HandlerMethod(method.getDeclaringClass(), method))
                .orElse(null);
        }

        private Class<?> getBeanType() {
            return beanType;
        }

        private Method getMethod() {
            return method;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            HandlerMethod that = (HandlerMethod) o;
            return Objects.equals(beanType, that.beanType) && Objects.equals(method, that.method);
        }

        @Override
        public int hashCode() {
            return Objects.hash(beanType, method);
        }
    }
}
