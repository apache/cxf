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

package demo.throttling.client;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import com.codahale.metrics.MetricRegistry;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.metrics.codahale.CodahaleMetricsProvider;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;

public final class Client implements Runnable {

    private static final QName SERVICE_NAME
        = new QName("http://apache.org/hello_world_soap_http", "SOAPService");

    private final String username;
    private final SOAPService service;
    private volatile boolean doStop;
    private Client(String name, SOAPService service) {
        this.username = name;
        this.service = service;
    }
    
    @Override
    public void run() {
        long start = System.currentTimeMillis();
        int x = 0;
        boolean exceeded = false;
        try (Greeter port = service.getSoapPort(new MetricsFeature())) {
            port.getRequestContext().put(MetricsProvider.CLIENT_ID, username);
            port.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, username);
            port.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "password");
            try {
                do {
                    if (doStop) {
                        break;
                    }
                    port.greetMe(username + "-" + x);
                    x++;
                } while (x < 10000);
            } catch (javax.xml.ws.WebServiceException wse) {
                if (wse.getCause().getMessage().contains("429")) {
                    //exceeded are allowable number of requests
                    exceeded = true;
                } else {
                    wse.printStackTrace();
                }
            }
            long end = System.currentTimeMillis();
            double rate = x * 1000 / (end - start);
            System.out.println(username + " finished " + x + " invocations: " + rate + " req/sec " 
                + (exceeded ? "(exceeded max)" : ""));
            try {
                //sleep for a few seconds before the client is closed so things can be seen in JMX 
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                //ignore
            } 
            
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
    public void stop() {
        doStop = true;
    }
    
    public static void main(String args[]) throws Exception {
        if (args.length == 0) {
            args = new String[] {SOAPService.WSDL_LOCATION.toExternalForm()};
        }

        URL wsdlURL;
        File wsdlFile = new File(args[0]);
        if (wsdlFile.exists()) {
            wsdlURL = wsdlFile.toURI().toURL();
        } else {
            wsdlURL = new URL(args[0]);
        }
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("bus.jmx.usePlatformMBeanServer", Boolean.TRUE);
        properties.put("bus.jmx.enabled", Boolean.TRUE);
        properties.put("bus.jmx.createMBServerConnectorFactory", Boolean.FALSE);
        Bus b = new CXFBusFactory().createBus(null, properties);
        MetricRegistry registry = new MetricRegistry();
        CodahaleMetricsProvider.setupJMXReporter(b, registry);
        b.setExtension(registry, MetricRegistry.class);        

        SOAPService ss = new SOAPService(wsdlURL, SERVICE_NAME);
        List<Client> c = new ArrayList<Client>();
        Client client;
        client = new Client("Tom", ss);
        new Thread(client).start();
        c.add(client);
        client = new Client("Rob", ss);
        new Thread(client).start();
        c.add(client);
        client = new Client("Vince", ss);
        new Thread(client).start();
        c.add(client);
        client = new Client("Malcolm", ss);
        new Thread(client).start();
        c.add(client);
        client = new Client("Jonas", ss);
        new Thread(client).start();
        c.add(client);
        
        System.out.println("Sleeping on main thread for 60 seconds");
        Thread.sleep(60000);
        for (Client c2 : c) {
            c2.stop();
        }
        Thread.sleep(2000);

        Thread.sleep(1000000);
        //System.exit(0);
    }

    

}
