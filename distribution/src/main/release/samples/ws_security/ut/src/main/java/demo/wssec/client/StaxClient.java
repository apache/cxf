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

package demo.wssec.client;

import java.io.Closeable;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.hello_world_soap_http.Greeter;
import org.apache.cxf.hello_world_soap_http.GreeterService;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxOutInterceptor;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;

import demo.wssec.server.UTPasswordCallback;

/**
 * A StAX-based client
 */
public final class StaxClient {

    private StaxClient() {
    }

    public static void main(String args[]) throws Exception {
        try {

            SpringBusFactory bf = new SpringBusFactory();
            URL busFile = StaxClient.class.getResource("/wssec.xml");
            Bus bus = bf.createBus(busFile.toString());
            BusFactory.setDefaultBus(bus);

            WSSSecurityProperties properties = new WSSSecurityProperties();
            properties.addAction(WSSConstants.USERNAMETOKEN);
            properties.addAction(WSSConstants.TIMESTAMP);

            properties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_DIGEST);
            properties.setTokenUser("abcd");
            properties.setCallbackHandler(new UTPasswordCallback());
            
            WSSSecurityProperties inProperties = new WSSSecurityProperties();
            inProperties.addAction(WSSConstants.USERNAMETOKEN);
            inProperties.addAction(WSSConstants.TIMESTAMP);

            inProperties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_TEXT);
            inProperties.setCallbackHandler(new UTPasswordCallback());
            
            GreeterService service = new GreeterService();
            Greeter port = service.getGreeterPort();
            org.apache.cxf.endpoint.Client client = ClientProxy.getClient(port);
            client.getInInterceptors().add(new WSS4JStaxInInterceptor(inProperties));
            client.getOutInterceptors().add(new WSS4JStaxOutInterceptor(properties));

            String[] names = new String[] {"Anne", "Bill", "Chris", "Scott"};
            // make a sequence of 4 invocations
            for (int i = 0; i < 4; i++) {
                System.out.println("Invoking greetMe...");
                String response = port.greetMe(names[i]);
                System.out.println("response: " + response + "\n");

            }
            if (port instanceof Closeable) {
                ((Closeable)port).close();
            }
            bus.shutdown(true);

        } catch (UndeclaredThrowableException ex) {
            ex.getUndeclaredThrowable().printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
