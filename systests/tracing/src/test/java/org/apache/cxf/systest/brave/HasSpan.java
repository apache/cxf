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

import java.util.Map;
import java.util.stream.Collectors;

import brave.handler.MutableSpan;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsIterableContaining;

public class HasSpan extends IsIterableContaining<MutableSpan> {
    public HasSpan(final String name) {
        this(name, null);
    }

    public HasSpan(final String name, final Matcher<Iterable<? super String>> matcher) {
        super(new TypeSafeMatcher<MutableSpan>() {
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
            protected boolean matchesSafely(MutableSpan item) {
                if (!name.equals(item.name())) {
                    return false;
                }

                if (matcher != null) {
                    return matcher.matches(item.annotations()
                        .stream()
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList()));
                }

                return true;
            }
        });
    }

    public static HasSpan hasSpan(final String name) {
        return new HasSpan(name);
    }

    public static HasSpan hasSpan(final String name, final Matcher<Iterable<? super String>> matcher) {
        return new HasSpan(name, matcher);
    }
}