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

package demo.service;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.Validate;
import org.apache.cxf.common.logging.LogUtils;

@Path("/customers/")
public class CustomerService {
    private static final Logger LOGGER = LogUtils.getL7dLogger(CustomerService.class);

    Map<Long, Customer> customers = new HashMap<Long, Customer>();

    @GET
    @Path("/{id}/")
    @Produces("application/json")
    public Customer getCustomer(@PathParam("id") final String id) {
        Validate.notNull(id);
        Validate.notEmpty(id);
        
        LOGGER.log(Level.FINE, "Invoking getCustomer, id={0}", id);

        Customer customer = customers.get(Long.parseLong(id));

        if (customer == null) {
            LOGGER.log(Level.SEVERE, "Specified customer does not exist, id={0}", id);    
        }

        return customer;
    }

    @POST
    @Consumes("application/json")
    public Response updateCustomer(final Customer customer) {
        Validate.notNull(customer);

        LOGGER.log(Level.FINE, "Invoking updateCustomer, customer={0}", customer);

        if (isCustomerExists(customer)) {
            LOGGER.log(Level.FINE, "Specified customer exists, update data, customer={0}", customer);
        } else {
            LOGGER.log(Level.WARNING, "Specified customer does not exist, add data, customer={0}", customer);
        }

        customers.put(customer.getId(), customer);

        LOGGER.log(Level.INFO, "Customer was updated successful, customer={0}", customer);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}/")
    public Response deleteCustomer(@PathParam("id") String id) {
        Validate.notNull(id);
        Validate.notEmpty(id);
        
        LOGGER.log(Level.FINE, "Invoking deleteCustomer, id={0}", id);

        long identifier = Long.parseLong(id);

        Response response;

        if (isCustomerExists(identifier)) {
            LOGGER.log(Level.FINE, "Specified customer exists, remove data, id={0}", id);
            customers.remove(identifier);
            LOGGER.log(Level.INFO, "Customer was removed successful, id={0}", id);
            response = Response.ok().build();
        } else {
            LOGGER.log(Level.SEVERE, "Specified customer does not exist, remove fail, id={0}", id);
            response = Response.notModified().build();
        }

        return response;
    }

    private boolean isCustomerExists(final Customer customer) {
        return customers.get(customer.getId()) != null;
    }

    private boolean isCustomerExists(final long id) {
        return customers.get(id) != null;
    }

    @XmlRootElement(name = "Customer")
    public static class Customer {
        private long id;
        private String name;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Customer{id=" + id + ", name='" + name + "'}";
        }
    }
}
