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
package org.apache.cxf.jaxrs.ext.search;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;

/**
 * Default ParamConverterProvider with support of primitive Java type converters including Date.
 */
public class DefaultParamConverterProvider implements ParamConverterProvider {
    private final Map< Class< ? >, ParamConverter< ? > > converters =
            new HashMap<>();

    /**
     * Date type converter.
     */
    private static final class DateParamConverter implements ParamConverter< Date > {
        @Override
        public Date fromString(final String value) {
            return SearchUtils.dateFromStringWithDefaultFormats(value);
        }

        @Override
        public String toString(final Date value) {
            return value != null ? DateTools.dateToString(value, Resolution.MILLISECOND) : null;
        }
    }

    /**
     * Long type converter.
     */
    private static final class LongParamConverter implements ParamConverter< Long > {
        @Override
        public Long fromString(final String value) {
            return Long.valueOf(value);
        }

        @Override
        public String toString(final Long value) {
            return Long.toString(value);
        }
    }

    /**
     * Double type converter.
     */
    private static final class DoubleParamConverter implements ParamConverter< Double > {
        @Override
        public Double fromString(final String value) {
            return Double.valueOf(value);
        }

        @Override
        public String toString(final Double value) {
            return Double.toString(value);
        }
    }

    /**
     * Float type converter.
     */
    private static final class FloatParamConverter implements ParamConverter< Float > {
        @Override
        public Float fromString(final String value) {
            return Float.valueOf(value);
        }

        @Override
        public String toString(final Float value) {
            return Float.toString(value);
        }
    }

    /**
     * Integer type converter.
     */
    private static final class IntegerParamConverter implements ParamConverter< Integer > {
        @Override
        public Integer fromString(final String value) {
            return Integer.valueOf(value);
        }

        @Override
        public String toString(final Integer value) {
            return Integer.toString(value);
        }
    }

    public DefaultParamConverterProvider() {
        converters.put(Date.class, new DateParamConverter());
        converters.put(Long.class, new LongParamConverter());
        converters.put(Double.class, new DoubleParamConverter());
        converters.put(Float.class, new FloatParamConverter());
        converters.put(Integer.class, new IntegerParamConverter());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType,
            final Annotation[] annotations) {

        for (final Entry<Class<?>, ParamConverter<?>> entry: converters.entrySet()) {
            if (entry.getKey().isAssignableFrom(rawType)) {
                return (ParamConverter<T>)entry.getValue();
            }
        }

        return null;
    }
}
