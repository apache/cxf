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
package org.apache.cxf.customer.bare;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Resource;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceException;
import org.apache.cxf.customer.Customer;
import org.apache.cxf.customer.CustomerNotFoundDetails;
import org.apache.cxf.customer.CustomerNotFoundFault;
import org.apache.cxf.customer.Customers;

// END SNIPPET: service
@WebService(targetNamespace = "http://cxf.apache.org/jra")
public class CustomerService {
    long currentId = 1;
    Map<Long, Customer> customers = new HashMap<>();

    @Resource
    private WebServiceContext context;

    public CustomerService() {
        Customer customer = createCustomer();
        customers.put(customer.getId(), customer);
    }

    @WebMethod
    @WebResult(name = "customers")
    public Customers getCustomers(@WebParam(name = "GetCustomers") GetCustomers req) {
        Customers cbean = new Customers();
        cbean.setCustomer(customers.values());

        if (context == null || context.getMessageContext() == null) {
            throw new WebServiceException("WebServiceContext is null!");
        }

        return cbean;
    }

    @WebMethod
    @WebResult(name = "customer")
    public Customer getCustomer(@WebParam(name = "getCustomer") GetCustomer getCustomer)
        throws CustomerNotFoundFault {
        Customer c = customers.get(getCustomer.getId());
        if (c == null) {
            CustomerNotFoundDetails details = new CustomerNotFoundDetails();
            details.setId(getCustomer.getId());
            throw new CustomerNotFoundFault(details);
        }
        return c;
    }

    @WebMethod
    public String getSomeDetails(@WebParam(name = "getSomeDetails") GetCustomer getCustomer)
        throws CustomerNotFoundFault {
        return "some details";
    }

    @WebMethod
    public void updateCustomer(@WebParam(name = "customer") Customer c) {
        customers.put(c.getId(), c);
    }

    @WebMethod
    public void addCustomer(@WebParam(name = "customer") Customer c) {
        long id = ++currentId;
        c.setId(id);

        customers.put(id, c);
    }

    @WebMethod
    public void deleteCustomer(long id) {
        customers.remove(id);
    }

    final Customer createCustomer() {
        Customer c = new Customer();
        c.setName("Dan Diephouse");
        c.setId(123);
        return c;
    }
}
// END SNIPPET: service
