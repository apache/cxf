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

package org.apache.cxf.jaxrs.utils;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.RedirectionException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

final class SpecExceptions {

    private static final Map<Integer, Class<?>> EXCEPTIONS_MAP;

    static {
        EXCEPTIONS_MAP = new HashMap<>();
        EXCEPTIONS_MAP.put(400, BadRequestException.class);
        EXCEPTIONS_MAP.put(401, NotAuthorizedException.class);
        EXCEPTIONS_MAP.put(403, ForbiddenException.class);
        EXCEPTIONS_MAP.put(404, NotFoundException.class);
        EXCEPTIONS_MAP.put(405, NotAllowedException.class);
        EXCEPTIONS_MAP.put(406, NotAcceptableException.class);
        EXCEPTIONS_MAP.put(415, NotSupportedException.class);
        EXCEPTIONS_MAP.put(500, InternalServerErrorException.class);
        EXCEPTIONS_MAP.put(503, ServiceUnavailableException.class);
    }

    private SpecExceptions() {
    }

    public static Class<?> getWebApplicationExceptionClass(Response exResponse,
                                                           Class<?> defaultExceptionType) {
        int status = exResponse.getStatus();
        Class<?> cls = EXCEPTIONS_MAP.get(status);
        if (cls == null) {
            int family = status / 100;
            if (family == 3) {
                cls = RedirectionException.class;
            } else if (family == 4) {
                cls = ClientErrorException.class;
            } else if (family == 5) {
                cls = ServerErrorException.class;
            }
        }
        return cls == null ? defaultExceptionType : cls;
    }

    public static InternalServerErrorException toInternalServerErrorException(Throwable cause, Response response) {

        return new InternalServerErrorException(checkResponse(response, 500), cause);
    }

    public static BadRequestException toBadRequestException(Throwable cause, Response response) {

        return new BadRequestException(checkResponse(response, 400), cause);
    }

    public static NotFoundException toNotFoundException(Throwable cause, Response response) {

        return new NotFoundException(checkResponse(response, 404), cause);
    }

    public static NotAuthorizedException toNotAuthorizedException(Throwable cause, Response response) {

        return new NotAuthorizedException(checkResponse(response, 401), cause);
    }

    public static ForbiddenException toForbiddenException(Throwable cause, Response response) {

        return new ForbiddenException(checkResponse(response, 403), cause);
    }

    public static NotAcceptableException toNotAcceptableException(Throwable cause, Response response) {

        return new NotAcceptableException(checkResponse(response, 406), cause);
    }

    public static NotSupportedException toNotSupportedException(Throwable cause, Response response) {

        return new NotSupportedException(checkResponse(response, 415), cause);
    }

    public static WebApplicationException toHttpException(Throwable cause, Response response) {

        if (response == null) {
            throw new WebApplicationException(cause);
        }
        throw response.getStatus() >= 500 ? new ServerErrorException(response, cause)
            : new ClientErrorException(response, cause);
    }

    private static Response checkResponse(Response r, int status) {
        if (r == null) {
            return JAXRSUtils.toResponse(status);
        }
        return r;
    }
}
