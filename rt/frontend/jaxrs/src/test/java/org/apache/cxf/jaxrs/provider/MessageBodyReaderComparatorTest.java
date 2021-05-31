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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.cxf.jaxrs.model.ProviderInfo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MessageBodyReaderComparatorTest {

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

        class Red extends Reader<Object> {
        }
        class Green extends Reader<Object> {
        }
        class Blue extends Reader<Object> {
        }

        final List<ProviderInfo<MessageBodyReader<?>>> providers = Providers.readers()
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

        @Consumes("*/*")
        class StarStar extends Reader<Object> {
        }
        @Consumes("text/plain")
        class TextPlain extends Reader<Object> {
        }
        @Consumes("text/*")
        class TextStar extends Reader<Object> {
        }

        final List<ProviderInfo<MessageBodyReader<?>>> providers = Providers.readers()
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

        class ShapeReader extends Reader<Shape> implements Alpha<URI> {
        }
        class SquareReader extends Reader<Square> implements Beta<Object, Shape> {
        }
        class ObjectReader extends Reader<Object> implements Alpha<Shape> {
        }

        final List<ProviderInfo<MessageBodyReader<?>>> providers = Providers.readers()
                .system(new ShapeReader())
                .system(new SquareReader())
                .system(new ObjectReader())
                .get();


        assertOrder(providers, "SquareReader\n"
                + "ShapeReader\n"
                + "ObjectReader");
    }


    public static void assertOrder(final List<ProviderInfo<MessageBodyReader<?>>> actual,
                                   final String expected) {
        final ProviderFactory.MessageBodyReaderComparator comparator =
                new ProviderFactory.MessageBodyReaderComparator();

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

        public static Providers<MessageBodyReader<?>> readers() {
            return new Providers<>();
        }

        public static Providers<MessageBodyReader<?>> writers() {
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


    public static class Reader<T> implements MessageBodyReader<T> {
        @Override
        public boolean isReadable(final Class<?> aClass, final Type type, final Annotation[] annotations,
                                  final MediaType mediaType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T readFrom(final Class<T> aClass, final Type type, final Annotation[] annotations,
                          final MediaType mediaType, final MultivaluedMap<String, String> multivaluedMap,
                          final InputStream inputStream) throws IOException, WebApplicationException {
            throw new UnsupportedOperationException();
        }
    }
}
