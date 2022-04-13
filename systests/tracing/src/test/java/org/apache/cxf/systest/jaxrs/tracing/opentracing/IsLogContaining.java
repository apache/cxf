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
package org.apache.cxf.systest.jaxrs.tracing.opentracing;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsIterableContaining;

import io.jaegertracing.internal.LogData;

public class IsLogContaining extends IsIterableContaining<LogData> {
    public IsLogContaining(final String value) {
        super(new TypeSafeMatcher<LogData>() {
            @Override
            public void describeTo(Description description) {
                description
                    .appendText("annotation with name ")
                    .appendValue(value);
            }

            @Override
            protected boolean matchesSafely(LogData item) {
                return value.equals(item.getMessage());
            }
        });
    }

    public static IsLogContaining hasItem(final String value) {
        return new IsLogContaining(value);
    }
}