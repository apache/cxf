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
package org.apache.cxf.jaxrs.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.jaxrs.model.ProviderInfo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MessageBodyWriterComparatorTest {

    /**
     * These three providers are identical from the JAX-RS perspective.
     *
     * However, we want to ensure the user always gets a consistent experience
     * and which provider is invoked first to be completely stable and deterministic.
     *
     * As often providers are discovered on the classpath via the @Provider annotation
     * and classpath order is not deterministic, we provide a fallback stable sort so
     * users don't experience "random failures" when doing things like restarting their
     * jvm, upgrading their server, and non-coding activities that can change classpath
     * order.
     */
    @Test
    public void sortIsStable() throws Exception {

        class Red extends Writer<Object> {
        }
        class Green extends Writer<Object> {
        }
        class Blue extends Writer<Object> {
        }

        final List<ProviderInfo<MessageBodyWriter<?>>> providers = Providers.writers()
                .system(new Red())
                .system(new Green())
                .system(new Blue())
                .get();


        assertOrder(providers, "Blue\n"
                + "Green\n"
                + "Red");
    }


    @Test
    public void mostSpecificMediaTypeWins() {

        @Produces("*/*")
        class StarStar extends Writer<Object> {
        }
        @Produces("text/plain")
        class TextPlain extends Writer<Object> {
        }
        @Produces("text/*")
        class TextStar extends Writer<Object> {
        }

        final List<ProviderInfo<MessageBodyWriter<?>>> providers = Providers.writers()
                .system(new StarStar())
                .system(new TextPlain())
                .system(new TextStar())
                .get();


        assertOrder(providers, "TextPlain\n"
                + "TextStar\n"
                + "StarStar");
    }

    // These Generics should be ignored
    interface Alpha<T> {
    }

    // These Generics should be ignored
    interface Beta<T, R> {
    }

    @Test
    public void mostSpecificClassTypeWins() {

        class Shape {
        }
        class Square extends Shape {
        }

        class ShapeWriter extends Writer<Shape> implements Alpha<URI> {
        }
        class SquareWriter extends Writer<Square> implements Beta<Object, Shape> {
        }
        class ObjectWriter extends Writer<Object> implements Alpha<Shape> {
        }

        final List<ProviderInfo<MessageBodyWriter<?>>> providers = Providers.writers()
                .system(new ShapeWriter())
                .system(new SquareWriter())
                .system(new ObjectWriter())
                .get();


        assertOrder(providers, "SquareWriter\n"
                + "ShapeWriter\n"
                + "ObjectWriter");
    }


    public static void assertOrder(final List<ProviderInfo<MessageBodyWriter<?>>> actual,
                                   final String expected) {
        final ProviderFactory.MessageBodyWriterComparator comparator =
                new ProviderFactory.MessageBodyWriterComparator();

        actual.sort(comparator);
        final String order = actual.stream()
                .map(ProviderInfo::getProvider)
                .map(Object::getClass)
                .map(Class::getSimpleName)
                .reduce((s, s2) -> s + "\n" + s2)
                .get();

        assertEquals(expected, order);
    }

    private static class Providers<T> {
        private final List<ProviderInfo<T>> providers = new ArrayList<>();

        public static Providers<MessageBodyWriter<?>> writers() {
            return new Providers<>();
        }

        public Providers<T> system(final T provider) {
            providers.add(new ProviderInfo<>(provider, null, false));
            return this;
        }

        public Providers<T> custom(final T provider) {
            providers.add(new ProviderInfo<>(provider, null, true));
            return this;
        }

        public List<ProviderInfo<T>> get() {
            Collections.shuffle(providers);
            return providers;
        }
    }


    public static class Writer<T> implements MessageBodyWriter<T> {
        @Override
        public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeTo(T t, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType,
                            MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream)
                throws IOException, WebApplicationException {
            throw new UnsupportedOperationException();
        }
    }
}
