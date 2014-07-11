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
package com.example.customerservice.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Starter that initializes the spring context and so also creates the service endpoint.
 */
public final class CustomerServiceSpringServer {
    private CustomerServiceSpringServer() {
    }
    
    public static void main(String args[]) throws Exception {
        ClassPathXmlApplicationContext ctx
            = new ClassPathXmlApplicationContext("server-applicationContext.xml");
        System.out.println("Server started. Press any key to shut down.");
        System.in.read();
        ctx.close();
    }

}
