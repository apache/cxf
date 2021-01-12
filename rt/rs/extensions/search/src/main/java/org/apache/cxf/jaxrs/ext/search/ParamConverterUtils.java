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

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

/**
 * Helper class to work with parameter converter providers and parameter converters.
 */
public final class ParamConverterUtils {
    private ParamConverterUtils() {
    }

    /**
     * Converts the string-based representation of the value to the instance of particular type
     * using parameter converter provider and available parameter converter.
     * @param type type to convert from string-based representation
     * @param provider parameter converter provider to use
     * @param value the string-based representation to convert
     * @return instance of particular type converter from its string representation
     */
    @SuppressWarnings("unchecked")
    public static< T > T getValue(final Class< T > type, final ParamConverterProvider provider,
            final String value) {

        if (String.class.isAssignableFrom(type)) {
            return (T)value;
        }

        if (provider != null) {
            final ParamConverter< T > converter = provider.getConverter(type, null, new Annotation[0]);
            if (converter != null) {
                return converter.fromString(value);
            }
        }

        throw new IllegalArgumentException(String.format(
                "Unable to convert string '%s' to instance of class '%s': no appropriate converter provided",
                value, type.getName()));
    }

    /**
     * Converts the instance of particular type into its string-based representation
     * using parameter converter provider and available parameter converter.
     * @param type type to convert to string-based representation
     * @param provider parameter converter provider to use
     * @param value the typed instance to convert to string representation
     * @return string-based representation of the instance of particular type
     */
    public static< T > String getString(final Class< T > type, final ParamConverterProvider provider,
            final T value) {

        if (provider != null) {
            final ParamConverter< T > converter = provider.getConverter(type, null, new Annotation[0]);
            if (converter != null) {
                return converter.toString(value);
            }
        }

        return value == null ? null : value.toString();
    }
}
