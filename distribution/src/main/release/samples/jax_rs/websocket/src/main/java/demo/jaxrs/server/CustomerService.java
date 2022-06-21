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
package demo.jaxrs.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.http.HttpServletResponse;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.apache.cxf.transport.websocket.WebSocketConstants;

@Path("/customerservice/")
@Produces("text/xml")
public class CustomerService {
    private static final int MAX_ERROR_COUNT = 5;
    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    long currentId = 123;
    Map<Long, Customer> customers = new HashMap<>();
    Map<Long, Order> orders = new HashMap<>();
    Map<String, WriterHolder<OutputStream>> monitors = new HashMap<>();
    Map<String, WriterHolder<HttpServletResponse>> monitors2 = new HashMap<>();

    public CustomerService() {
        init();
    }

    @GET
    @Path("/customers/{id}/")
    public Customer getCustomer(@PathParam("id") String id) {
        System.out.println("----invoking getCustomer, Customer id is: " + id);
        long idNumber = Long.parseLong(id);
        Customer customer = customers.get(idNumber);
        if (customer != null) {
            sendCustomerEvent("retrieved", customer);
        }
        return customer;
    }

    @PUT
    @Path("/customers/")
    public Response updateCustomer(Customer customer) {
        System.out.println("----invoking updateCustomer, Customer name is: " + customer.getName());
        Customer c = customers.get(customer.getId());
        Response r;
        if (c != null) {
            customers.put(customer.getId(), customer);
            r = Response.ok().build();
            sendCustomerEvent("updated", customer);
        } else {
            r = Response.notModified().build();
        }

        return r;
    }

    @POST
    @Path("/customers/")
    public Response addCustomer(Customer customer) {
        System.out.println("----invoking addCustomer, Customer name is: " + customer.getName());
        customer.setId(++currentId);

        customers.put(customer.getId(), customer);
        sendCustomerEvent("added", customer);
        return Response.ok(customer).build();
    }

    @DELETE
    @Path("/customers/{id}/")
    public Response deleteCustomer(@PathParam("id") String id) {
        System.out.println("----invoking deleteCustomer, Customer id is: " + id);
        long idNumber = Long.parseLong(id);
        Customer c = customers.get(idNumber);

        Response r;
        if (c != null) {
            r = Response.ok().build();
            Customer customer = customers.remove(idNumber);
            if (customer != null) {
                sendCustomerEvent("deleted", customer);
            }
        } else {
            r = Response.notModified().build();
        }

        return r;
    }

    @Path("/orders/{orderId}/")
    public Order getOrder(@PathParam("orderId") String orderId) {
        System.out.println("----invoking getOrder, Order id is: " + orderId);
        long idNumber = Long.parseLong(orderId);
        return orders.get(idNumber);
    }

    @GET
    @Path("/monitor")
    @Produces("text/*")
    public StreamingOutput monitorCustomers(
            @HeaderParam(WebSocketConstants.DEFAULT_REQUEST_ID_KEY) String reqid) {
        final String key = reqid == null ? "*" : reqid;
        return new StreamingOutput() {
            public void write(final OutputStream out) throws IOException, WebApplicationException {
                monitors.put(key, new WriterHolder(out, MAX_ERROR_COUNT));
                out.write(("Subscribed at " + new java.util.Date()).getBytes());
            }

        };
    }

    @GET
    @Path("/monitor2")
    @Produces("text/*")
    public void monitorCustomers2(
            @jakarta.ws.rs.core.Context final HttpServletResponse httpResponse,
            @HeaderParam(WebSocketConstants.DEFAULT_REQUEST_ID_KEY) String reqid) {
        final String key = reqid == null ? "*" : reqid;
        monitors2.put(key, new WriterHolder(httpResponse, MAX_ERROR_COUNT));
        try {
            httpResponse.getOutputStream().write(("Subscribed at " + new java.util.Date()).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GET
    @Path("/unmonitor/{key}")
    @Produces("text/*")
    public String unmonitorCustomers(@PathParam("key") String key) {
        return (monitors.remove(key) != null ? "Removed: " : "Already removed: ") + key;
    }

    @GET
    @Path("/unmonitor2/{key}")
    @Produces("text/*")
    public String unmonitorCustomers2(@PathParam("key") String key) {
        return (monitors2.remove(key) != null ? "Removed: " : "Already removed: ") + key;
    }

    // CHECKSTYLE:OFF
    private void sendCustomerEvent(final String msg, final Customer customer) {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    String t = msg + ": " + customer.getId() + "/" + customer.getName();
                    for (Iterator<WriterHolder<OutputStream>> it = monitors.values().iterator(); it.hasNext();) {
                        WriterHolder<OutputStream> wh = it.next();
                        try {
                            wh.getValue().write(t.getBytes());
                            wh.getValue().flush();
                            wh.reset();
                        } catch (IOException e) {
                            System.out.println("----error writing to " + wh.getValue() + " " + wh.get());
                            if (wh.increment()) {
                                // the max error count reached; purging the output resource
                                e.printStackTrace();
                                try {
                                    wh.getValue().close();
                                } catch (IOException e2) {
                                    // ignore;
                                }
                                it.remove();
                                System.out.println("----purged " + wh.getValue());
                            }
                        }
                    }
                    for (Iterator<WriterHolder<HttpServletResponse>> it = monitors2.values().iterator();
                            it.hasNext();) {
                        WriterHolder<HttpServletResponse> wh = it.next();
                        try {
                            wh.getValue().getOutputStream().write(t.getBytes());
                            wh.getValue().getOutputStream().flush();
                            wh.reset();
                        } catch (IOException e) {
                            System.out.println("----error writing to " + wh.getValue() + " " + wh.get());
                            if (wh.increment()) {
                                // the max error count reached; purging the output resource
                                e.printStackTrace();
                                try {
                                    wh.getValue().getOutputStream().close();
                                } catch (IOException e2) {
                                    // ignore;
                                }
                                it.remove();
                                System.out.println("----purged " + wh.getValue());
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }
    // CHECKSTYLE:ON

    final void init() {
        Customer c = new Customer();
        c.setName("John");
        c.setId(123);
        customers.put(c.getId(), c);
        c = new Customer();
        c.setName("Homer");
        c.setId(235);
        customers.put(c.getId(), c);

        Order o = new Order();
        o.setDescription("order 223");
        o.setId(223);
        orders.put(o.getId(), o);
    }

    private static class WriterHolder<T> {
        private final T value;
        private final int max;
        private final AtomicInteger errorCount;

        WriterHolder(T object, int max) {
            this.value = object;
            this.max = max;
            this.errorCount = new AtomicInteger();
        }

        public T getValue() {
            return value;
        }

        public int get() {
            return errorCount.get();
        }
        public boolean increment() {
            return max < errorCount.getAndIncrement();
        }

        public void reset() {
            errorCount.getAndSet(0);
        }
    }
}
