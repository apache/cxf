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
package org.apache.cxf.systest.jaxrs.tracing.htrace;

import org.apache.htrace.core.TimelineAnnotation;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsCollectionContaining;


public class IsTimelineContaining extends IsCollectionContaining<TimelineAnnotation> {
    public IsTimelineContaining(final String value) {
        super(new TypeSafeMatcher<TimelineAnnotation>() {
            @Override
            public void describeTo(Description description) {
                description
                    .appendText("timeline with name ")
                    .appendValue(value);
            }

            @Override
            protected boolean matchesSafely(TimelineAnnotation item) {
                return value.equals(item.getMessage());
            }
        });
    }

    public static IsTimelineContaining hasItem(final String value) {
        return new IsTimelineContaining(value);
    }
}