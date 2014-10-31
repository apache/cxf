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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.cxf.transport.websocket.WebSocketConstants;

@Path("/customerservice/")
@Produces("text/xml")
public class CustomerService {
    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    long currentId = 123;
    Map<Long, Customer> customers = new HashMap<Long, Customer>();
    Map<Long, Order> orders = new HashMap<Long, Order>();
    Map<String, OutputStream> monitors = new HashMap<String, OutputStream>();
    
    public CustomerService() {
        init();
    }

    @GET
    @Path("/customers/{id}/")
    public Customer getCustomer(@PathParam("id") String id) {
        System.out.println("----invoking getCustomer, Customer id is: " + id);
        long idNumber = Long.parseLong(id);
        Customer customer = customers.get(idNumber);
        sendCustomerEvent("retrieved", customer);
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
        Order c = orders.get(idNumber);
        return c;
    }

    @GET
    @Path("/monitor")
    @Produces("text/*")
    public StreamingOutput monitorCustomers(@HeaderParam(WebSocketConstants.DEFAULT_REQUEST_ID_KEY)
                                            String reqid) {
        final String key = reqid == null ? "*" : reqid; 
        return new StreamingOutput() {
            public void write(final OutputStream out) throws IOException, WebApplicationException {
                monitors.put(key, out);
                out.write(("Subscribed at " + new java.util.Date()).getBytes());
            }
        };
    }

    @GET
    @Path("/unmonitor/{key}")
    @Produces("text/*")
    public String unmonitorCustomers(@PathParam("key") String key) {
        return (monitors.remove(key) != null ? "Removed: " : "Already removed: ") + key; 
    }

    private void sendCustomerEvent(final String msg, final Customer customer) {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    String t = msg + ": " + customer.getId() + "/" + customer.getName();
                    for (Iterator<OutputStream> it = monitors.values().iterator(); it.hasNext();) {
                        OutputStream out = it.next();
                        try {
                            out.write(t.getBytes());
                        } catch (IOException e) {
                            try {
                                out.close();
                            } catch (IOException e2) {
                                // ignore;
                            }
                            it.remove();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }
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

}
