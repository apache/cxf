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
package demo.restful.server;

import java.util.HashMap;
import java.util.Map;

import javax.jws.WebService;

// END SNIPPET: service
@WebService(endpointInterface = "demo.restful.server.CustomerService")
public class CustomerServiceImpl implements CustomerService {
    long currentId = 1;
    Map<Long, Customer> customers = new HashMap<Long, Customer>();

    public CustomerServiceImpl() {
        Customer customer = createCustomer();
        customers.put(customer.getId(), customer);
    }

    public Customers getCustomers() {
        Customers c = new Customers();
        c.setCustomer(customers.values());
        return c;
    }

    public Customer getCustomer(GetCustomer getCustomer) throws CustomerNotFoundFault {
        Customer c = customers.get(getCustomer.getId());
        if (c == null) {
            CustomerNotFoundDetails details = new CustomerNotFoundDetails();
            details.setId(getCustomer.getId());
            throw new CustomerNotFoundFault(details);
        }
        return c;
    }

    public void updateCustomer(Customer c) {
        customers.put(c.getId(), c);
    }

    public long addCustomer(Customer c) {
        long id = ++currentId;
        c.setId(id);

        customers.put(id, c);

        return c.getId();
    }

    public void deleteCustomer(long id) {
        customers.remove(id);
    }

    final Customer createCustomer() {
        Customer c = new Customer();
        c.setName("John");
        c.setId(123);
        return c;
    }
}
// END SNIPPET: service
