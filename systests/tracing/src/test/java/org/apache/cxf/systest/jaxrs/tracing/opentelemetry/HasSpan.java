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
package org.apache.cxf.systest.jaxrs.tracing.opentelemetry;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsIterableContaining;

import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;

public class HasSpan extends IsIterableContaining<SpanData> {
    public HasSpan(final String name) {
        this(name, null);
    }

    public HasSpan(final String name, final Matcher<Iterable<? super EventData>> matcher) {
        super(new TypeSafeMatcher<SpanData>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("span with name ").appendValue(name).appendText(" ");

                if (matcher != null) {
                    description.appendText(" and ");
                    matcher.describeTo(description);
                }
            }

            @Override
            protected boolean matchesSafely(SpanData item) {
                if (!name.equals(item.getName())) {
                    return false;
                }

                if (matcher != null) {
                    return matcher.matches(item.getEvents());
                }

                return true;
            }
        });
    }

    public static HasSpan hasSpan(final String name) {
        return new HasSpan(name);
    }

    public static HasSpan hasSpan(final String name, final Matcher<Iterable<? super EventData>> matcher) {
        return new HasSpan(name, matcher);
    }
}
