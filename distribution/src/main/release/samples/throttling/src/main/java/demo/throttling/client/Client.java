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
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

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
        try (Greeter port = service.getSoapPort()) {
            port.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, username);
            port.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "password");
            int x = 0;
            do {
                if (doStop) {
                    break;
                }
                port.greetMe(username + "-" + x);
                x++;
            } while (x < 10000);
            long end = System.currentTimeMillis();
            double rate = x * 1000 / (end - start);
            System.out.println(username + " finished " + x + " invocations: " + rate + " req/sec");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public void stop() {
        doStop = true;
    }
    
    public static void main(String args[]) throws Exception {
        if (args.length == 0) {
            System.out.println("please specify wsdl");
            System.exit(1);
        }

        URL wsdlURL;
        File wsdlFile = new File(args[0]);
        if (wsdlFile.exists()) {
            wsdlURL = wsdlFile.toURI().toURL();
        } else {
            wsdlURL = new URL(args[0]);
        }

        System.out.println(wsdlURL);
        SOAPService ss = new SOAPService(wsdlURL, SERVICE_NAME);
        List<Client> c = new ArrayList<Client>();
        Client client = new Client("Tom", ss);
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
        
        System.out.println("Sleeping on main thread for 60 seconds");
        Thread.sleep(60000);
        for (Client c2 : c) {
            c2.stop();
        }
        Thread.sleep(2000);

        System.exit(0);
    }

    

}
