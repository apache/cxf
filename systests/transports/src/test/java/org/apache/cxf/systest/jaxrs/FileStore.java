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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.activation.DataHandler;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

@Path("/file-store")
public class FileStore {
    private final ConcurrentMap<String, byte[]> store = new ConcurrentHashMap<>();
    @Context private HttpHeaders headers;

    @POST
    @Path("/stream")
    @Consumes("*/*")
    public Response addBook(@QueryParam("chunked") boolean chunked, InputStream in) throws IOException {
        String transferEncoding = headers.getHeaderString("Transfer-Encoding");

        if (chunked != Objects.equals("chunked", transferEncoding)) {
            throw new WebApplicationException(Status.EXPECTATION_FAILED);
        }

        try (in) {
            if (chunked) {
                return Response.ok(new StreamingOutput() {
                    @Override
                    public void write(OutputStream out) throws IOException, WebApplicationException {
                        in.transferTo(out);
                    }
                }).build();
            } else {
                // Make sure we have small amount of data for chunking to not kick in
                final byte[] content = in.readAllBytes(); 
                return Response.ok(Arrays.copyOf(content, content.length / 10)).build();
            }
        }
    }

    @POST
    @Consumes("multipart/form-data")
    public void addBook(@QueryParam("chunked") boolean chunked, 
            @Suspended final AsyncResponse response, @Context final UriInfo uri, final MultipartBody body)  {
        
        String transferEncoding = headers.getHeaderString("Transfer-Encoding");
        if (chunked != Objects.equals("chunked", transferEncoding)) {
            response.resume(Response.status(Status.EXPECTATION_FAILED).build());
            return;
        }

        for (final Attachment attachment: body.getAllAttachments()) {
            final DataHandler handler = attachment.getDataHandler();

            if (handler != null) {
                final String source = handler.getName();
                if (StringUtils.isEmpty(source)) {
                    response.resume(Response.status(Status.BAD_REQUEST).build());
                    return;
                }

                try {
                    if (store.containsKey(source)) {
                        response.resume(Response.status(Status.CONFLICT).build());
                        return;
                    }

                    final byte[] content = IOUtils.readBytesFromStream(handler.getInputStream());
                    if (store.putIfAbsent(source, content) != null) {
                        response.resume(Response.status(Status.CONFLICT).build());
                        return;
                    }
                } catch (final Exception ex) {
                    response.resume(Response.serverError().build());
                }

                if (response.isSuspended()) {
                    response.resume(Response.created(uri.getRequestUriBuilder().path(source).build())
                            .build());
                }
            }
        }

        if (response.isSuspended()) {
            response.resume(Response.status(Status.BAD_REQUEST).build());
        }
    }
}
