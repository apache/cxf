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

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import org.codehaus.jra.Delete;
import org.codehaus.jra.Get;
import org.codehaus.jra.HttpResource;
import org.codehaus.jra.Post;
import org.codehaus.jra.Put;

@WebService(targetNamespace = "http://demo.restful.server")
public interface CustomerService {

    @Get
    @HttpResource(location = "/customers")
    @WebResult(name = "Customers")
    Customers getCustomers();

    @Get
    @HttpResource(location = "/customers/{id}")
    Customer getCustomer(@WebParam(name = "GetCustomer")
                         GetCustomer getCustomer) throws CustomerNotFoundFault;

    @Put
    @HttpResource(location = "/customers/{id}")
    void updateCustomer(@WebParam(name = "Customer")
                        Customer c);

    @Post
    @HttpResource(location = "/customers")
    long addCustomer(@WebParam(name = "Customer")
                     Customer c);

    @Delete
    @HttpResource(location = "/customers/{id}")
    void deleteCustomer(@WebParam(name = "id")
                        long id) throws CustomerNotFoundFault;

}
