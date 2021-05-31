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

import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.junit.Test;

import javax.annotation.Priority;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ParamConverterComparatorTest {

    /**
     * The @Priority annotation can be used to give precedence
     * to one converter over another.  Lowest number wins.
     */
    @Test
    public void priorityAnnotation() {

        @Priority(37)
        class Red extends Provider {
        }

        @Priority(11)
        class Green extends Provider {
        }

        @Priority(71)
        class Blue extends Provider {
        }

        final List<ProviderInfo<ParamConverterProvider>> infos = Providers.converters()
                .custom(new Blue())
                .custom(new Green())
                .custom(new Red())
                .get();

        assertOrder(infos, "Green\n"
                + "Red\n"
                + "Blue");

    }

    /**
     * Any converters built-in by default should have a lower
     * priority than user supplied converters.  User supplied
     * converters should be favored.
     */
    @Test
    public void customOverSystem() {
        class Red extends Provider {
        }

        class Green extends Provider {
        }

        class Blue extends Provider {
        }

        final List<ProviderInfo<ParamConverterProvider>> infos = Providers.converters()
                .custom(new Blue())
                .system(new Green())
                .custom(new Red())
                .get();

        assertOrder(infos, "Blue\n"
                + "Red\n"
                + "Green");
    }

    /**
     * Ensure the 'custom' status we keep internally does not
     * take precedence over the @Priority annotation.
     */
    @Test
    public void priorityAnnotationBeatsCustomBoolean() {

        @Priority(37)
        class Red extends Provider {
        }

        @Priority(11)
        class Green extends Provider {
        }

        @Priority(71)
        class Blue extends Provider {
        }

        final List<ProviderInfo<ParamConverterProvider>> infos = Providers.converters()
                .custom(new Blue())
                .custom(new Green())
                .system(new Red())
                .get();

        assertOrder(infos, "Green\n"
                + "Red\n"
                + "Blue");
    }

    /**
     * We do a fallback sort on class name as when there is nothing
     * else to influence order, we essentially get a JVM influenced
     * list that *will* change order once in a while between jvm
     * restarts.  This can have frustrating consequences for users
     * who are expecting no change in behavior as they aren't
     * changing their code.
     */
    @Test
    public void fallBackClassNameSort() {
        class Red extends Provider {
        }

        class Green extends Provider {
        }

        class Blue extends Provider {
        }

        final List<ProviderInfo<ParamConverterProvider>> infos = Providers.converters()
                .custom(new Blue())
                .custom(new Green())
                .custom(new Red())
                .get();

        assertOrder(infos, "Blue\n"
                + "Green\n"
                + "Red");
    }

    private static class Provider implements ParamConverterProvider {
        @Override
        public <T> ParamConverter<T> getConverter(final Class<T> aClass, final Type type, final Annotation[] annotations) {
            return null;
        }
    }

    private static class Providers<T> {
        private final List<ProviderInfo<T>> providers = new ArrayList<>();

        public static Providers<ParamConverterProvider> converters() {
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

    public static void assertOrder(final List<ProviderInfo<ParamConverterProvider>> actual,
                                   final String expected) {
        final ProviderFactory.ParamConverterComparator comparator =
                new ProviderFactory.ParamConverterComparator();

        actual.sort(comparator);
        final String order = actual.stream()
                .map(ProviderInfo::getProvider)
                .map(Object::getClass)
                .map(Class::getSimpleName)
                .reduce((s, s2) -> s + "\n" + s2)
                .get();

        assertEquals(expected, order);
    }

}
