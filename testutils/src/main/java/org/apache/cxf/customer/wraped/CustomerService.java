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
package org.apache.cxf.customer.wraped;

import java.util.HashMap;
import java.util.Map;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import org.apache.cxf.customer.Customer;
import org.apache.cxf.customer.Customers;

// END SNIPPET: service
@WebService(targetNamespace = "http://cxf.apache.org/jra")
public class CustomerService {
    long currentId = 1;
    Map<Long, Customer> customers = new HashMap<>();

    public CustomerService() {
        Customer customer = createCustomer();
        customers.put(customer.getId(), customer);
    }

    @WebMethod
    @WebResult(name = "customers")
    public Customers getCustomers() {
        Customers cbean = new Customers();
        cbean.setCustomer(customers.values());
        return cbean;
    }

    @WebMethod
    @WebResult(name = "customer")
    public Customer getCustomer(@WebParam(name = "id") Long id) {
        return customers.get(id);
    }

    @WebMethod
    public void updateCustomer(@WebParam(name = "id") String id,
                               @WebParam(name = "customer") Customer c) {
        customers.put(c.getId(), c);
    }

    @WebMethod
    public void addCustomer(@WebParam(name = "customer") Customer c) {
        long id = ++currentId;
        c.setId(id);

        customers.put(id, c);
    }

    @WebMethod
    public void deleteCustomer(String id) {
        customers.remove(Long.valueOf(id));
    }

    final Customer createCustomer() {
        Customer c = new Customer();
        c.setName("Dan Diephouse");
        c.setId(123);
        return c;
    }
}
// END SNIPPET: service
