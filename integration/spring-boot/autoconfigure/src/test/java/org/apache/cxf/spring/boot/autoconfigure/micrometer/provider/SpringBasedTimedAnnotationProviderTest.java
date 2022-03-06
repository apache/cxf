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

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.BindingOperationInfo;

import io.micrometer.api.annotation.Timed;
import io.micrometer.api.annotation.TimedSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.openMocks;

@SuppressWarnings({"unused"})
public class SpringBasedTimedAnnotationProviderTest {
    private SpringBasedTimedAnnotationProvider underTest;
    @Mock
    private Exchange exchange;
    @Mock
    private Service service;
    @Mock
    private BindingOperationInfo bindingOperationInfo;
    @Mock
    private MethodDispatcher methodDispatcher;

    @Before
    public void setUp() {
        openMocks(this);
        underTest = new SpringBasedTimedAnnotationProvider();

        doReturn(service).when(exchange).getService();
        doReturn(bindingOperationInfo).when(exchange).getBindingOperationInfo();
        doReturn(methodDispatcher).when(service).get(MethodDispatcher.class.getName());
    }

    @Test
    public void testReturnAllDifferentTimedAnnotationsFromMethod() throws NoSuchMethodException {
        // given
        class TesterClass {
            @Timed(value = "timed1")
            @Timed(value = "timed2")
            @Timed
            public void method() {
            }
        }
        mockClass(TesterClass.class);

        // when
        Set<Timed> actual = underTest.getTimedAnnotations(exchange, false);

        // then
        assertThat(actual.stream()
                .map(Timed::value)
                .collect(Collectors.toSet()), containsInAnyOrder("timed1", "timed2", ""));
    }

    @Test
    public void testReturnAllDifferentTimedAnnotationsFromMethodWhenTimedSetIsUsed()
            throws NoSuchMethodException {
        // given
        class TesterClass {
            @TimedSet({@Timed(value = "timed1"), @Timed(value = "timed2"), @Timed})
            public void method() {
            }
        }
        mockClass(TesterClass.class);

        // when
        Set<Timed> actual = underTest.getTimedAnnotations(exchange, false);

        // then
        assertThat(actual.stream()
                .map(Timed::value)
                .collect(Collectors.toSet()), containsInAnyOrder("timed1", "timed2", ""));
    }

    @Test
    public void testReturnMetaAnnotationFromMethodWithAliasFor() throws NoSuchMethodException {
        // given
        class TesterClass {
            @CustomTimed("aliasTimed")
            public void method() {
            }
        }
        mockClass(TesterClass.class);

        // when
        Set<Timed> actual = underTest.getTimedAnnotations(exchange, false);

        // then
        assertThat(actual.stream().map(Timed::value).collect(Collectors.toSet()), containsInAnyOrder("aliasTimed"));
    }

    @Test
    public void testReturnAllDifferentTimedAnnotationsFromClass() throws NoSuchMethodException {
        // given
        @Timed(value = "timed1")
        @Timed(value = "timed2")
        @Timed
        class TesterClass {
            public void method() {
            }
        }
        mockClass(TesterClass.class);

        // when
        Set<Timed> actual = underTest.getTimedAnnotations(exchange, false);

        // then
        assertThat(actual.stream()
                .map(Timed::value)
                .collect(Collectors.toSet()), containsInAnyOrder("timed1", "timed2", ""));
    }


    @Test
    public void testReturnAnnotationsFromMethodAndNotFromClass() throws NoSuchMethodException {
        // given
        @Timed(value = "timed1")
        class TesterClass {
            @Timed(value = "timed2")
            public void method() {
            }
        }
        mockClass(TesterClass.class);

        // when
        Set<Timed> actual = underTest.getTimedAnnotations(exchange, false);

        // then
        assertThat(actual.stream().map(Timed::value).collect(Collectors.toSet()), containsInAnyOrder("timed2"));
    }

    @Test
    public void testReturnEmptySetWhenNeitherClassNorInputMethodHasTimedAnnotation()
            throws NoSuchMethodException {
        // given
        class TesterClass {
            public void method() {
            }

            @Timed
            public void otherMethod() {
            }
        }
        mockClass(TesterClass.class);

        // when
        Set<Timed> actual = underTest.getTimedAnnotations(exchange, false);

        // then
        assertThat(actual.stream().map(Timed::value).collect(Collectors.toSet()), empty());
    }

    private void mockClass(Class<?> testerClazz) throws NoSuchMethodException {
        Method method = testerClazz.getMethod("method");
        doReturn(method).when(methodDispatcher).getMethod(bindingOperationInfo);
    }
}
