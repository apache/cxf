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

package demo.throttling.server;

import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.Endpoint;

import com.codahale.metrics.MetricRegistry;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.throttling.ThrottlingInterceptor;
import org.apache.cxf.throttling.ThrottlingManager;

public class Server {
    Map<String, Customer> customers = new HashMap<>();
    
    protected Server() throws Exception {
        System.out.println("Starting Server");

        customers.put("Tom", new Customer.PremiumCustomer("Tom"));
        customers.put("Rob", new Customer.PreferredCustomer("Rob"));
        customers.put("Vince", new Customer.RegularCustomer("Vince"));
        customers.put("Malcolm", new Customer.CheapCustomer("Malcolm"));
        
        Bus b = BusFactory.getDefaultBus();
        MetricRegistry registry = new MetricRegistry();
        b.setExtension(registry, MetricRegistry.class);        
        
        ThrottlingManager manager = new ThrottlingManager() {
            @Override
            public long getThrottleDelay(Message m) {
                if (m.get("THROTTLED") != null) {
                    return 0;
                }
                m.put("THROTTLED", true);
                Customer c = m.getExchange().get(Customer.class);
                return c.throttle(m);
            }           
        };
        b.getInInterceptors().add(new CustomerMetricsInterceptor(registry, customers));
        b.getInInterceptors().add(new ThrottlingInterceptor(Phase.PRE_STREAM, manager));
        //add Throttling
        
        Object implementor = new GreeterImpl();
        String address = "http://localhost:9001/SoapContext/SoapPort";
        Endpoint.publish(address, implementor, new MetricsFeature());
    }

    public static void main(String args[]) throws Exception {
        new Server();
        System.out.println("Server ready...");

        Thread.sleep(5 * 60 * 1000);
        System.out.println("Server exiting");
        System.exit(0);
    }
}
