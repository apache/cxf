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

package com.example.customerservice.client;

import java.util.List;

import junit.framework.Assert;

import com.example.customerservice.Customer;
import com.example.customerservice.CustomerService;
import com.example.customerservice.NoSuchCustomerException;

public final class CustomerServiceTester {
    
    // The CustomerService proxy will be injected either by spring or by a direct call to the setter 
    CustomerService customerService;
    
    public CustomerService getCustomerService() {
        return customerService;
    }

    public void setCustomerService(CustomerService customerService) {
        this.customerService = customerService;
    }

    public void testCustomerService() throws NoSuchCustomerException {
        List<Customer> customers = null;
        
        // First we test the positive case where customers are found and we retreive
        // a list of customers
        System.out.println("Sending request for customers named Smith");
        customers = customerService.getCustomersByName("Smith");
        System.out.println("Response received");
        Assert.assertEquals(2, customers.size());
        Assert.assertEquals("Smith", customers.get(0).getName());
        
        // Then we test for an unknown Customer name and expect the NoSuchCustomerException
        try {
            customers = customerService.getCustomersByName("None");
            Assert.fail("We should get a NoSuchCustomerException here");
        } catch (NoSuchCustomerException e) {
            System.out.println(e.getMessage());
            Assert.assertNotNull("FaultInfo must not be null", e.getFaultInfo());
            Assert.assertEquals("None", e.getFaultInfo().getCustomerName());
            System.out.println("NoSuchCustomer exception was received as expected");
        }
        
        // The implementation of updateCustomer is set to sleep for some seconds. 
        // Still this method should return instantly as the method is declared
        // as a one way method in the WSDL
        Customer customer = new Customer();
        customer.setName("Smith");
        customerService.updateCustomer(customer);
        
        System.out.println("All calls were successful");
    }

}
