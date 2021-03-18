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

package org.apache.cxf.metrics.micrometer.provider;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import static java.util.Optional.ofNullable;

public class StandardTagsProvider implements TagsProvider {

    private final ExceptionClassProvider exceptionClassProvider;
    private final StandardTags standardTags;

    public StandardTagsProvider(ExceptionClassProvider exceptionClassProvider, StandardTags standardTags) {
        this.exceptionClassProvider = exceptionClassProvider;
        this.standardTags = standardTags;
    }

    @Override
    public Iterable<Tag> getTags(Exchange ex, boolean client) {
        final Message request = getRequest(ex, client);
        final Message response = getResponse(ex, client);

        Class<?> exception = exceptionClassProvider.getExceptionClass(ex, client);

        return Tags.of(
                standardTags.method(request),
                standardTags.uri(request),
                standardTags.exception(exception),
                standardTags.status(response),
                standardTags.outcome(response));
    }

    private Message getResponse(Exchange ex, boolean client) {
        if (client) {
            return ofNullable(ex.getInMessage()).orElseGet(ex::getInFaultMessage);
        } else {
            return ofNullable(ex.getOutMessage()).orElseGet(ex::getOutFaultMessage);
        }
    }

    private Message getRequest(Exchange ex, boolean client) {
        if (client) {
            return ofNullable(ex.getOutMessage()).orElseGet(ex::getOutFaultMessage);
        } else {
            return ofNullable(ex.getInMessage()).orElseGet(ex::getInFaultMessage);
        }
    }
}
