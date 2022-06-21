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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import jakarta.ws.rs.ext.ParamConverter;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class JavaTimeTypesParamConverterProviderTest {

    JavaTimeTypesParamConverterProvider provider = new JavaTimeTypesParamConverterProvider();

    @Test
    public void localDate() {
        LocalDate localDate = LocalDate.of(2016, 2, 24);
        ParamConverter<LocalDate> converter = (ParamConverter<LocalDate>)
                provider.getConverter(localDate.getClass(), localDate.getClass(),
                        localDate.getClass().getAnnotations());
        Assert.assertEquals(localDate, converter.fromString(converter.toString(localDate)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidLocalDate() {
        ParamConverter<LocalDate> converter =
                provider.getConverter(LocalDate.class, LocalDate.class, null);
        converter.fromString("invalid");
    }

    @Test
    public void localDateTime() {
        LocalDateTime localDateTime = LocalDateTime.of(2016, 2, 24, 5, 55);
        ParamConverter<LocalDateTime> converter = (ParamConverter<LocalDateTime>)
                provider.getConverter(localDateTime.getClass(), localDateTime.getClass(),
                        localDateTime.getClass().getAnnotations());
        Assert.assertEquals(localDateTime, converter.fromString(converter.toString(localDateTime)));
    }

    @Test
    public void localTime() {
        LocalTime localTime = LocalTime.of(10, 33);
        ParamConverter<LocalTime> converter = (ParamConverter<LocalTime>)
                provider.getConverter(localTime.getClass(), localTime.getClass(),
                        localTime.getClass().getAnnotations());
        Assert.assertEquals(localTime, converter.fromString(converter.toString(localTime)));
    }

    @Test
    public void offsetDateTime() {
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2016, 2, 24, 5, 55, 0, 0, ZoneOffset.UTC);
        ParamConverter<OffsetDateTime> converter = (ParamConverter<OffsetDateTime>)
                provider.getConverter(offsetDateTime.getClass(), offsetDateTime.getClass(),
                        offsetDateTime.getClass().getAnnotations());
        Assert.assertEquals(offsetDateTime, converter.fromString(converter.toString(offsetDateTime)));
    }

    @Test
    public void offsetTime() {
        OffsetTime offsetTime = OffsetTime.of(10, 33, 24, 5, ZoneOffset.ofHours(2));
        ParamConverter<OffsetTime> converter = (ParamConverter<OffsetTime>)
                provider.getConverter(offsetTime.getClass(), offsetTime.getClass(),
                        offsetTime.getClass().getAnnotations());
        Assert.assertEquals(offsetTime, converter.fromString(converter.toString(offsetTime)));
    }

    @Test
    public void zonedDateTime() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2016, 2, 24, 6, 10, 9, 5, ZoneOffset.ofHours(6));
        ParamConverter<ZonedDateTime> converter = (ParamConverter<ZonedDateTime>)
                provider.getConverter(zonedDateTime.getClass(), zonedDateTime.getClass(),
                        zonedDateTime.getClass().getAnnotations());
        Assert.assertEquals(zonedDateTime, converter.fromString(converter.toString(zonedDateTime)));
    }
}
