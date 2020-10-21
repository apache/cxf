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

package org.apache.cxf.spring.boot.autoconfigure.micrometer.provider;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.metrics.micrometer.provider.TimedAnnotationProvider;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.springframework.core.annotation.MergedAnnotationCollectors;
import org.springframework.core.annotation.MergedAnnotations;

import io.micrometer.core.annotation.Timed;

import static java.util.Collections.emptySet;

public class SpringBasedTimedAnnotationProvider implements TimedAnnotationProvider {

    private final ConcurrentHashMap<HandlerMethod, Set<Timed>> timedAnnotationCache = new ConcurrentHashMap<>();

    @Override
    public Set<Timed> getTimedAnnotations(Exchange ex) {
        HandlerMethod handlerMethod = new HandlerMethod(ex);

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

    Set<Timed> findTimedAnnotations(AnnotatedElement element) {
        return MergedAnnotations.from(element).stream(Timed.class)
                .collect(MergedAnnotationCollectors.toAnnotationSet());
    }

    private static final class HandlerMethod {
        private final Class beanType;
        private final Method method;

        private HandlerMethod(Exchange exchange) {
            Service service = exchange.getService();
            BindingOperationInfo bop = exchange.getBindingOperationInfo();
            MethodDispatcher md = (MethodDispatcher) service.get(MethodDispatcher.class.getName());

            this.method = md.getMethod(bop);
            this.beanType = method.getDeclaringClass();
        }

        private Class getBeanType() {
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
