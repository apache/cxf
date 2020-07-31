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
package org.apache.cxf.sts.rs.impl;

import java.util.Optional;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.cxf.sts.rs.api.BaseResponse;

public class RestExceptionMapper implements ExceptionMapper<Exception> {

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(Exception exception) {
        return Response
                .status(Status.INTERNAL_SERVER_ERROR)
                .entity(new BaseResponse(BaseResponse.StatusType.ERROR, exception.getMessage()))
                .type(getResponseType())
                .build();
    }

    private MediaType getResponseType() {
        return Optional.ofNullable(headers)
                .map(HttpHeaders::getAcceptableMediaTypes)
                .filter(mediaTypes -> mediaTypes.contains(MediaType.APPLICATION_JSON_TYPE))
                .map(mediaTypes -> mediaTypes.get(0))
                .orElse(MediaType.APPLICATION_XML_TYPE);
    }
}
