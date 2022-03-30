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

package org.apache.cxf.metrics.micrometer.provider.jaxrs;

import java.lang.reflect.Method;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;

import io.micrometer.core.instrument.Tag;

import static java.util.Optional.ofNullable;

public class JaxrsTags {

    private static final String UNKNOWN = "UNKNOWN";

    private static final Tag OPERATION_UNKNOWN = Tag.of("operation", UNKNOWN);
    
    public Tag operation(Message request) {
        return ofNullable(request)
            .flatMap(MessageUtils::getTargetMethod)
            .map(Method::getName)
            .map(operation -> Tag.of("operation", operation))
            .orElseGet(() -> method(request));
    }
    
    public Tag method(Message request) {
        return ofNullable(request)
            .map(Message::getExchange)
            .map(ex -> ex.get(Method.class))
            .map(Method::getName)
            .map(operation -> Tag.of("operation", operation))
            .orElse(OPERATION_UNKNOWN);
    }
}
