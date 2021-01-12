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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

/**
 * ParamConverterProvider for Java 8 JSR 310 Date Time API
 */
@Provider
public class JavaTimeTypesParamConverterProvider implements ParamConverterProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {

        if (rawType.equals(LocalDateTime.class)) {
            return (ParamConverter<T>) new LocalDateTimeConverter();
        } else if (rawType.equals(LocalDate.class)) {
            return (ParamConverter<T>) new LocalDateConverter();
        } else if (rawType.equals(LocalTime.class)) {
            return (ParamConverter<T>) new LocalTimeConverter();
        } else if (rawType.equals(OffsetDateTime.class)) {
            return (ParamConverter<T>) new OffsetDateTimeConverter();
        } else if (rawType.equals(OffsetTime.class)) {
            return (ParamConverter<T>) new OffsetTimeConverter();
        } else if (rawType.equals(ZonedDateTime.class)) {
            return (ParamConverter<T>) new ZonedDateTimeConverter();
        } else {
            return null;
        }
    }

    public class LocalDateTimeConverter implements ParamConverter<LocalDateTime> {
        @Override
        public LocalDateTime fromString(String value) {
            try {
                return LocalDateTime.parse(value, getFormatter());
            } catch (DateTimeParseException parseException) {
                throw new IllegalArgumentException(parseException);
            }
        }

        @Override
        public String toString(LocalDateTime localDateTime) {
            return getFormatter().format(localDateTime);
        }

        protected DateTimeFormatter getFormatter() {
            return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        }
    }

    public class LocalDateConverter implements ParamConverter<LocalDate> {
        @Override
        public LocalDate fromString(String value) {
            try {
                return LocalDate.parse(value, getFormatter());
            } catch (DateTimeParseException parseException) {
                throw new IllegalArgumentException(parseException);
            }
        }

        @Override
        public String toString(LocalDate localDate) {
            return getFormatter().format(localDate);
        }

        protected DateTimeFormatter getFormatter() {
            return DateTimeFormatter.ISO_LOCAL_DATE;
        }
    }

    public class LocalTimeConverter implements ParamConverter<LocalTime> {
        @Override
        public LocalTime fromString(String value) {
            try {
                return LocalTime.parse(value, getFormatter());
            } catch (DateTimeParseException parseException) {
                throw new IllegalArgumentException(parseException);
            }
        }

        @Override
        public String toString(LocalTime localTime) {
            return getFormatter().format(localTime);
        }

        protected DateTimeFormatter getFormatter() {
            return DateTimeFormatter.ISO_LOCAL_TIME;
        }
    }

    public class OffsetDateTimeConverter implements ParamConverter<OffsetDateTime> {
        @Override
        public OffsetDateTime fromString(String value) {
            try {
                return OffsetDateTime.parse(value, getFormatter());
            } catch (DateTimeParseException parseException) {
                throw new IllegalArgumentException(parseException);
            }
        }

        @Override
        public String toString(OffsetDateTime offsetDateTime) {
            return getFormatter().format(offsetDateTime);
        }

        protected DateTimeFormatter getFormatter() {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        }
    }

    public class OffsetTimeConverter implements ParamConverter<OffsetTime> {
        @Override
        public OffsetTime fromString(String value) {
            try {
                return OffsetTime.parse(value, getFormatter());
            } catch (DateTimeParseException parseException) {
                throw new IllegalArgumentException(parseException);
            }
        }

        @Override
        public String toString(OffsetTime offsetTime) {
            return getFormatter().format(offsetTime);
        }

        protected DateTimeFormatter getFormatter() {
            return DateTimeFormatter.ISO_OFFSET_TIME;
        }
    }

    public class ZonedDateTimeConverter implements ParamConverter<ZonedDateTime> {
        @Override
        public ZonedDateTime fromString(String value) {
            try {
                return ZonedDateTime.parse(value, getFormatter());
            } catch (DateTimeParseException parseException) {
                throw new IllegalArgumentException(parseException);
            }
        }

        @Override
        public String toString(ZonedDateTime zonedDateTime) {
            return getFormatter().format(zonedDateTime);
        }

        protected DateTimeFormatter getFormatter() {
            return DateTimeFormatter.ISO_ZONED_DATE_TIME;
        }
    }
}
