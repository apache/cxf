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
package org.apache.cxf.systest;

import java.util.Collection;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class HasSize extends TypeSafeMatcher<Collection<?>> {
    private final int expectedSize;

    public HasSize(int expectedSize) {
        this.expectedSize = expectedSize;
    }

    @Override
    protected boolean matchesSafely(Collection<?> collection) {
        return collection.size() == expectedSize;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a collection with size ").appendValue(expectedSize);
    }

    @Override
    protected void describeMismatchSafely(Collection<?> collection, Description description) {
        description.appendText("size was ").appendValue(collection.size())
            .appendText(", contents: ").appendValue(collection);
    }

    public static HasSize hasSize(int size) {
        return new HasSize(size);
    }
}
