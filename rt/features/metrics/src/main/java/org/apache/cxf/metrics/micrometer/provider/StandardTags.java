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

import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.message.Message;

import io.micrometer.core.instrument.Tag;

import static java.util.Optional.ofNullable;

public class StandardTags {

    private static final String UNKNOWN = "UNKNOWN";
    private static final String OUTCOME = "outcome";

    private static final Tag URI_UNKNOWN = Tag.of("uri", UNKNOWN);

    private static final Tag EXCEPTION_NONE = Tag.of("exception", "None");

    private static final Tag STATUS_UNKNOWN = Tag.of("status", UNKNOWN);

    private static final Tag OUTCOME_UNKNOWN = Tag.of(OUTCOME, UNKNOWN);

    private static final Tag OUTCOME_INFORMATIONAL = Tag.of(OUTCOME, "INFORMATIONAL");

    private static final Tag OUTCOME_SUCCESS = Tag.of(OUTCOME, "SUCCESS");

    private static final Tag OUTCOME_REDIRECTION = Tag.of(OUTCOME, "REDIRECTION");

    private static final Tag OUTCOME_CLIENT_ERROR = Tag.of(OUTCOME, "CLIENT_ERROR");

    private static final Tag OUTCOME_SERVER_ERROR = Tag.of(OUTCOME, "SERVER_ERROR");

    private static final Tag METHOD_UNKNOWN = Tag.of("method", UNKNOWN);

    public Tag method(Message request) {
        return ofNullable(request)
                .map(e -> e.get(Message.HTTP_REQUEST_METHOD))
                .filter(e -> e instanceof String)
                .map(e -> (String) e)
                .map(method -> Tag.of("method", method))
                .orElse(METHOD_UNKNOWN);
    }

    public Tag status(Message response) {
        return ofNullable(response)
                .map(e -> e.get(Message.RESPONSE_CODE))
                .filter(e -> e instanceof Integer)
                .map(e -> (Integer) e)
                .map(String::valueOf)
                .map(status -> Tag.of("status", status))
                .orElse(STATUS_UNKNOWN);
    }

    public Tag uri(Message request) {
        return ofNullable(request)
                .map(e -> e.get(Message.BASE_PATH))
                .filter(e -> e instanceof String)
                .map(e -> (String) e)
                .map(e -> Tag.of("uri", e))
                .orElse(URI_UNKNOWN);
    }

    public Tag exception(Class<?> exceptionClass) {
        return ofNullable(exceptionClass)
                .map(Class::getSimpleName)
                .map(e -> Tag.of("exception", StringUtils.isEmpty(e) ? exceptionClass.getName() : e))
                .orElse(EXCEPTION_NONE);
    }

    public Tag outcome(Message response) {
        Optional<Integer> statusCode =
                ofNullable(response)
                        .map(e -> e.get(Message.RESPONSE_CODE))
                        .filter(e -> e instanceof Integer)
                        .map(e -> (Integer) e);
        if (!statusCode.isPresent()) {
            return OUTCOME_UNKNOWN;
        }
        int status = statusCode.orElseThrow(() -> new NoSuchElementException("Should not throw"));
        if (status < 200) {
            return OUTCOME_INFORMATIONAL;
        }
        if (status < 300) {
            return OUTCOME_SUCCESS;
        }
        if (status < 400) {
            return OUTCOME_REDIRECTION;
        }
        if (status < 500) {
            return OUTCOME_CLIENT_ERROR;
        }
        return OUTCOME_SERVER_ERROR;
    }
}
