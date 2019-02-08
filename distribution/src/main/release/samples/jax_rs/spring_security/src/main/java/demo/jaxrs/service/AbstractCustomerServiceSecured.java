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
package demo.jaxrs.service;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/customerservice/")
public abstract class AbstractCustomerServiceSecured implements CustomerServiceSecured {

    protected long currentId = 123;
    protected Map<Long, Customer> customers = new HashMap<>();
    protected Map<Long, Order> orders = new HashMap<>();

    protected AbstractCustomerServiceSecured() {
        init();
    }

    @GET
    @Path("/customers/{id}/")
    public abstract Customer getCustomer(@PathParam("id") String id);

    @PUT
    @Path("/customers/{id}")
    public abstract Response updateCustomer(@PathParam("id") Long id, Customer customer);

    @POST
    @Path("/customers/")
    public abstract Response addCustomer(Customer customer);

    @DELETE
    @Path("/customers/{id}/")
    public abstract Response deleteCustomer(@PathParam("id") String id);

    @Path("/orders/{orderId}/")
    public abstract Order getOrder(@PathParam("orderId") String orderId);

    private void init() {
        Customer c = new Customer();
        c.setName("John");
        c.setId(123);
        customers.put(c.getId(), c);

        Order o = new Order();
        o.setDescription("order 223");
        o.setId(223);
        orders.put(o.getId(), o);
    }

}
