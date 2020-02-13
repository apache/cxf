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

package org.apache.cxf.metrics.micrometer.provider.jaxws;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.metrics.micrometer.provider.ExceptionClassProvider;
import org.apache.cxf.metrics.micrometer.provider.TagsProvider;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import static java.util.Optional.ofNullable;

public class JaxwsTagsProvider implements TagsProvider {

    private final ExceptionClassProvider exceptionClassProvider;
    private final JaxwsFaultCodeProvider faultCodeProvider;
    private final JaxwsTags cxfTags;

    public JaxwsTagsProvider(ExceptionClassProvider exceptionClassProvider, JaxwsFaultCodeProvider faultCodeProvider,
                             JaxwsTags cxfTags) {
        this.exceptionClassProvider = exceptionClassProvider;
        this.faultCodeProvider = faultCodeProvider;
        this.cxfTags = cxfTags;
    }

    @Override
    public Iterable<Tag> getTags(Exchange ex) {
        Message request = ofNullable(ex.getInMessage()).orElseGet(ex::getInFaultMessage);
        Message response = ofNullable(ex.getOutMessage()).orElseGet(ex::getOutFaultMessage);

        Class<?> exception = exceptionClassProvider.getExceptionClass(ex);
        String faultCode = faultCodeProvider.getFaultCode(ex);

        return Tags.of(
                cxfTags.method(request),
                cxfTags.uri(request),
                cxfTags.exception(exception),
                cxfTags.status(response),
                cxfTags.outcome(response),
                cxfTags.operation(request),
                cxfTags.faultCode(faultCode));
    }
}
