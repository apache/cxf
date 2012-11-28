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

package demo.ws_policy.client;

import java.net.URL;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.PingMeFault;
import org.apache.hello_world_soap_http.SOAPService;

public final class Client {

    private static final String USER_NAME = System.getProperty("user.name");

    private Client() {
    }

    public static void main(String args[]) {
        try {

            SpringBusFactory bf = new SpringBusFactory();
            URL busFile = Client.class.getResource("/client.xml");
            Bus bus = bf.createBus(busFile.toString());
            BusFactory.setDefaultBus(bus);

            SOAPService service = new SOAPService();
            Greeter port = service.getSoapPort();

            System.out.println("Invoking sayHi...");
            String resp = port.sayHi();
            System.out.println("Server responded with: " + resp + "\n");

            System.out.println("Invoking greetMe...");
            resp = port.greetMe(USER_NAME);
            System.out.println("Server responded with: " + resp + "\n");

            System.out.println("Invoking greetMeOneWay...");
            port.greetMeOneWay(USER_NAME);
            System.out.println("No response from server as method is OneWay\n");

            try {
                System.out.println("Invoking pingMe, expecting exception...");
                port.pingMe();
            } catch (PingMeFault ex) {
                System.out.println("Expected exception occurred: " + ex);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }  finally {
            System.exit(0);
        }
    }

}
