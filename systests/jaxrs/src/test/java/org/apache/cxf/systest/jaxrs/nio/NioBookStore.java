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
package org.apache.cxf.systest.jaxrs.nio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.LongAdder;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.apache.cxf.annotations.UseNio;
import org.apache.cxf.helpers.IOUtils;

@Path("/bookstore")
public class NioBookStore {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response readBooks() throws IOException {
        final ByteArrayInputStream in = new ByteArrayInputStream(
            IOUtils.readBytesFromStream(getClass().getResourceAsStream("/files/books.txt")));
        final byte[] buffer = new byte[4096];

        return Response.ok().entity(
            out -> {
                try {
                    final int n = in.read(buffer);

                    if (n >= 0) {
                        out.write(buffer, 0, n);
                        return true;
                    }
                        
                    try { 
                        in.close(); 
                    } catch (IOException ex) { 
                        /* do nothing */ 
                    }
                    
                    return false;
                } catch (IOException ex) {
                    throw new WebApplicationException(ex);
                }
            },
            throwable -> {
                throw throwable;
            }
        ).build();
    }
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/is")
    @UseNio
    public InputStream readBooksFromInputStream() throws IOException {
        return getClass().getResourceAsStream("/files/books.txt");
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    public void uploadBooks(@Context Request request, @Suspended AsyncResponse response) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[4096];
        final LongAdder adder = new LongAdder();

        request.entity(
            in -> {
                try {
                    final int n = in.read(buffer);
                    if (n > 0) {
                        adder.add(n);
                        out.write(buffer, 0, n);
                    }
                } catch (IOException e) {
                    throw new WebApplicationException(e);
                }
            },
            in -> {
                try {
                    if (!in.isFinished()) {
                        throw new IllegalStateException("Reader did not finish yet");
                    }
                    
                    out.close();
                    response.resume("Book Store uploaded: " + adder.longValue() + " bytes");
                } catch (IOException e) {
                    throw new WebApplicationException(e);
                }
            },
            throwable -> {              // error handler
                System.out.println("Problem found: " + throwable.getMessage());
                throw throwable;
            }
        );
    }
}
