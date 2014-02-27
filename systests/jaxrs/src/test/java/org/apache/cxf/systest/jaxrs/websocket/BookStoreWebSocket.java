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

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.StreamingOutput;

import org.apache.cxf.systest.jaxrs.Book;

@Path("/web/bookstore")
public class BookStoreWebSocket {
    
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
        return new Long(theBookId);
    }
    
    @GET
    @Path("/bookbought")
    @Produces("text/*")
    public StreamingOutput getBookBought() {
        return new StreamingOutput() {
            public void write(final OutputStream out) throws IOException, WebApplicationException {
                out.write(("Today: " + new java.util.Date()).getBytes());
                // just for testing, using a thread
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            for (int r = 2, i = 1; i <= 5; r *= 2, i++) {
                                Thread.sleep(500);
                                out.write(Integer.toString(r).getBytes());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        };
    }
    
}


