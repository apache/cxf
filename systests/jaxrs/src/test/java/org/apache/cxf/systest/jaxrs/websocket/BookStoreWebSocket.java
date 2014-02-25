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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

@Path("/bookstore")
public class BookStoreWebSocket {
    
    
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


