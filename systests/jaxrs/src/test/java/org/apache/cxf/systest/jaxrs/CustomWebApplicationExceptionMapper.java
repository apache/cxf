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
package org.apache.cxf.systest.jaxrs;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;

public class CustomWebApplicationExceptionMapper extends WebApplicationExceptionMapper
    implements ExceptionMapper<WebApplicationException> {

    public Response toResponse(WebApplicationException exception) {
        if (exception.getResponse().getStatus() == 500) {
            return Response.status(500).type("text/plain")
                .entity(exception.getCause().getMessage()).build();
        }
        return super.toResponse(exception);
    }

}
