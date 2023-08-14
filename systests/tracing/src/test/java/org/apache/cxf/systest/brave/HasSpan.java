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
package org.apache.cxf.systest.brave;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsIterableContaining;
import zipkin2.Annotation;
import zipkin2.Span;

public class HasSpan extends IsIterableContaining<Span> {
    public HasSpan(final String name) {
        this(name, null);
    }

    public HasSpan(final String name, final Matcher<Iterable<? super Annotation>> matcher) {
        super(new TypeSafeMatcher<Span>() {
            @Override
            public void describeTo(Description description) {
                description
                    .appendText("span with name ")
                    .appendValue(name)
                    .appendText(" ");

                if (matcher != null) {
                    description.appendText(" and ");
                    matcher.describeTo(description);
                }
            }

            @Override
            protected boolean matchesSafely(Span item) {
                if (!name.equals(item.name())) {
                    return false;
                }

                if (matcher != null) {
                    return matcher.matches(item.annotations());
                }

                return true;
            }
        });
    }

    public static HasSpan hasSpan(final String name) {
        return new HasSpan(name);
    }

    public static HasSpan hasSpan(final String name, final Matcher<Iterable<? super Annotation>> matcher) {
        return new HasSpan(name, matcher);
    }
}