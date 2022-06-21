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
import java.io.OutputStream;
import java.util.concurrent.atomic.LongAdder;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.annotations.UseNio;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.nio.NioReadEntity;
import org.apache.cxf.jaxrs.nio.NioWriteEntity;

@Path("/bookstore")
public class NioBookStore {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getBookStream() throws IOException {
        final ByteArrayInputStream in = new ByteArrayInputStream(
            IOUtils.readBytesFromStream(getClass().getResourceAsStream("/files/books.txt")));
        final byte[] buffer = new byte[4096];

        return Response
            .ok()
            .entity(
                new NioWriteEntity(
                out -> {
                    final int n = in.read(buffer);

                    if (n >= 0) {
                        out.write(buffer, 0, n);
                        return true;
                    }

                    closeInputStream(in);

                    return false;
                }
                // by default the runtime will throw the exception itself
                // if the error handler is not provided
                
                //,
                //throwable -> {
                //    throw throwable;
                //}
            ))
            .build();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/is")
    @UseNio
    public InputStream getBookStreamFromInputStream() throws IOException {
        return getClass().getResourceAsStream("/files/books.txt");
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public void uploadBookStream(@Suspended AsyncResponse response) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[4096];
        final LongAdder adder = new LongAdder();

        new NioReadEntity(
        // read handler                  
        in -> {
            final int n = in.read(buffer);
            if (n > 0) {
                adder.add(n);
                out.write(buffer, 0, n);
            }
        },
        // completion handler
        () -> {
            closeOutputStream(out);
            response.resume("Book Store uploaded: " + adder.longValue() + " bytes");
        }
        // by default the runtime will resume AsyncResponse with Throwable itself
        // if the error handler is not provided
        
        //,
        // error handler
        //t -> {
        //    response.resume(t);
        //}
        );
    }
    
    private static void closeInputStream(InputStream in) {
        try {
            in.close();
        } catch (IOException ex) {
            /* do nothing */
        }
    }
    private static void closeOutputStream(OutputStream out) {
        try {
            out.close();
        } catch (IOException ex) {
            /* do nothing */
        }
    }
}
