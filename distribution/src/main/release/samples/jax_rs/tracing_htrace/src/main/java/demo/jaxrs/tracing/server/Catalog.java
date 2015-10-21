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

package demo.jaxrs.tracing.server;


import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.tracing.Traceable;
import org.apache.cxf.tracing.TracerContext;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.trace.SpanReceiverHost;

import demo.jaxrs.tracing.conf.TracingConfiguration;

@Path("/catalog")
public class Catalog {
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final CatalogStore store;
    
    public Catalog() throws IOException {
        final Configuration configuration = HBaseConfiguration.create();
        configuration.set("hbase.zookeeper.quorum", "hbase");
        configuration.set("hbase.zookeeper.property.clientPort", "2181");
        configuration.set(SpanReceiverHost.SPAN_RECEIVERS_CONF_KEY, TracingConfiguration.HBASE_SPAN_RECEIVER.getName());
        store = new CatalogStore(configuration, "catalog");
    }
    
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response addBook(@Context final UriInfo uriInfo, @Context final TracerContext tracing, 
            @FormParam("title") final String title)  {
        try {
            final String id = UUID.randomUUID().toString();
        
            executor.submit(
                tracing.wrap("Inserting New Book", 
                    new Traceable<Void>() {
                        public Void call(final TracerContext context) throws Exception {
                            store.put(id, title);
                            return null;
                        }
                    }
                )
            ).get(10, TimeUnit.SECONDS);
            
            return Response
                .created(uriInfo.getRequestUriBuilder().path(id).build())
                .build();
        } catch (final Exception ex) {
            return Response
                .serverError()
                .entity(Json
                     .createObjectBuilder()
                     .add("error", ex.getMessage())
                     .build())
                .build();
        }
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public void getBooks(@Suspended final AsyncResponse response, 
            @Context final TracerContext tracing) throws Exception {
        tracing.continueSpan(new Traceable<Void>() {
            @Override
            public Void call(final TracerContext context) throws Exception {
                executor.submit(tracing.wrap("Looking for books", new Traceable<Void>() {
                    @Override
                    public Void call(final TracerContext context) throws Exception {
                        response.resume(store.scan());
                        return null;
                    }
                }));
                
                return null;
            }
        });
    }
    
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getBook(@PathParam("id") final String id) throws IOException {
        final JsonObject book = store.get(id);
        
        if (book == null) {
            throw new NotFoundException("Book with does not exists: " + id);
        }
        
        return book;
    }
    
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") final String id) throws IOException {
        if (!store.remove(id)) {
            throw new NotFoundException("Book with does not exists: " + id);
        }
        
        return Response.ok().build();
    }
}


