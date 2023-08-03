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

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;

import static org.hamcrest.Matchers.equalTo;

public class HasAttribute<T> extends FeatureMatcher<Attributes, T> {
    private final AttributeKey<T> key;

    public HasAttribute(AttributeKey<T> key, Matcher<T> valueMatcher) {
        super(valueMatcher, "attribute map containing key " + key.getKey() + " with value",
              "attribute value");
        this.key = key;
    }

    public static <V> HasAttribute<V> hasAttribute(AttributeKey<V> key, Matcher<V> valueMatcher) {
        return new HasAttribute<V>(key, valueMatcher);
    }

    public static <V> HasAttribute<V> hasAttribute(AttributeKey<V> key, V value) {
        return hasAttribute(key, equalTo(value));
    }

    public static HasAttribute<String> hasAttribute(String key, String value) {
        return hasAttribute(AttributeKey.stringKey(key), value);
    }

    @Override
    protected T featureValueOf(Attributes attributes) {
        return attributes.get(key);
    }
}
