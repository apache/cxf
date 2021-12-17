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

package org.apache.cxf.jaxrs.reactivestreams.server;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

@Provider
public class ResponseStatusOnlyExceptionMapper implements ExceptionMapper<ResponseStatusOnlyException> {
    @Override
    public Response toResponse(ResponseStatusOnlyException exception) {
        final Message message = JAXRSUtils.getCurrentMessage();
        final Throwable cause = exception.getCause();
        final Response response = ExceptionUtils.convertFaultToResponse(cause, message);
        
        if (response != null) {
            return Response.fromResponse(response).entity(null).build();
        } else {
            return Response.serverError().build();
        }
    }
}
