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

package org.apache.cxf.systest.jaxrs.websocket;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.cxf.jaxrs.ext.StreamingResponse;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.transport.websocket.WebSocketConstants;

@Path("/web/bookstore")
public class BookStoreWebSocket {
    private static ExecutorService executor = Executors.newSingleThreadExecutor();
    private Map<String, OutputStream> eventsStreams = new HashMap<>();

    @GET
    @Path("/booknames")
    @Produces("text/plain")
    public byte[] getBookName() {
        return "CXF in Action".getBytes();
    }

    @GET
    @Path("/booknames/servletstream")
    @Produces("text/plain")
    public void getBookNameStream(@Context HttpServletResponse response) throws Exception {
        OutputStream os = response.getOutputStream();
        response.setContentType("text/plain");
        os.write("CXF in Action".getBytes());
        os.flush();
    }

    @GET
    @Path("/books/{id}")
    @Produces("application/xml")
    public Book getBook(@PathParam("id") long id) {
        return new Book("CXF in Action", id);
    }

    @POST
    @Path("/booksplain")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Long echoBookId(long theBookId) {
        return Long.valueOf(theBookId);
    }

    @GET
    @Path("/bookbought")
    @Produces("application/*")
    public StreamingOutput getBookBought() {
        return new StreamingOutput() {
            public void write(final OutputStream out) throws IOException, WebApplicationException {
                out.write(("Today: " + new java.util.Date()).getBytes());
                // just for testing, using a thread
                executor.execute(new Runnable() {
                    public void run() {
                        try {
                            for (int r = 2, i = 1; i <= 5; r *= 2, i++) {
                                Thread.sleep(500);
                                out.write(Integer.toString(r).getBytes());
                                out.flush();
                            }
                            out.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
    }

    @GET
    @Path("/bookstream")
    @Produces("application/json")
    public StreamingResponse<Book> getBookStream() {
        return new StreamingResponse<Book>() {
            public void writeTo(final StreamingResponse.Writer<Book> out) throws IOException {
                out.write(new Book("WebSocket1", 1));
                executor.execute(new Runnable() {
                    public void run() {
                        try {
                            for (int i = 2; i <= 5; i++) {
                                Thread.sleep(500);
                                out.write(new Book("WebSocket" + i, i));
                                out.getEntityStream().flush();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
    }

    @GET
    @Path("/hold/{t}")
    @Produces("text/plain")
    public String hold(@PathParam("t") long t) {
        Date from = new Date();
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            // ignore
        }
        return "Held from " + from + " for " + t + " ms";
    }

    @GET
    @Path("/events/register")
    @Produces("text/plain")
    public StreamingOutput registerEventsStream(@HeaderParam(WebSocketConstants.DEFAULT_REQUEST_ID_KEY) String reqid) {
        final String key = reqid == null ? "*" : reqid;
        return new StreamingOutput() {
            public void write(final OutputStream out) throws IOException, WebApplicationException {
                eventsStreams.put(key, out);
                out.write(("Registered " + key + " at " + new java.util.Date()).getBytes());
            }
        };

    }

    @GET
    @Path("/events/create/{name}")
    @Produces("text/plain")
    public String createEvent(@PathParam("name") String name) {
        for (Iterator<OutputStream> it = eventsStreams.values().iterator(); it.hasNext();) {
            OutputStream out = it.next();
            try {
                out.write(("News: event " + name + " created").getBytes());
                out.flush();
            } catch (IOException e) {
                it.remove();
                e.printStackTrace();
            }
        }
        return name + " created";
    }

    @GET
    @Path("/events/unregister/{key}")
    @Produces("text/plain")
    public String unregisterEventsStream(@PathParam("key") String key) {
        return (eventsStreams.remove(key) != null ? "Unregistered: " : "Already Unregistered: ") + key;
    }
}


