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

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.hello_world_soap_http.Greeter;
import org.apache.cxf.hello_world_soap_http.GreeterService;

import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;


public final class Client {

    private static final String USER_NAME = System.getProperty("user.name");
    private static final String WSU_NS
        = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

    private Client() {
    }

    public static void main(String args[]) throws Exception {
        try {

            SpringBusFactory bf = new SpringBusFactory();
            URL busFile = Client.class.getResource("wssec.xml");
            Bus bus = bf.createBus(busFile.toString());
            bf.setDefaultBus(bus);

            Map<String, Object> outProps = new HashMap<String, Object>();
            outProps.put("action", "UsernameToken Timestamp Signature");

            outProps.put("passwordType", "PasswordDigest");
            outProps.put("user", "clientx509v1");
            outProps.put("passwordCallbackClass", "demo.wssec.client.UTPasswordCallback");

            //If you are using the patch WSS-194, then uncomment below two lines and comment
            //the above "user" prop line.
            //outProps.put("user", "abcd");
            //outProps.put("signatureUser", "clientx509v1");
            outProps.put("signaturePropFile", "etc/Client_Sign.properties");
            outProps.put("signatureKeyIdentifier", "DirectReference");
            outProps.put("signatureParts", 
                         "{Element}{" + WSU_NS + "}Timestamp;"
                         + "{Element}{http://schemas.xmlsoap.org/soap/envelope/}Body");

            bus.getOutInterceptors().add(new WSS4JOutInterceptor(outProps));

            Map<String, Object> inProps = new HashMap<String, Object>();

            inProps.put("action", "UsernameToken Timestamp Signature");
            inProps.put("passwordType", "PasswordText");
            inProps.put("passwordCallbackClass", "demo.wssec.client.UTPasswordCallback");

            inProps.put("signaturePropFile", "etc/Client_Encrypt.properties");
            inProps.put("signatureKeyIdentifier", "DirectReference");

            bus.getInInterceptors().add(new WSS4JInInterceptor(inProps));

            GreeterService service = new GreeterService();
            Greeter port = service.getGreeterPort();

            String[] names = new String[] {"Anne", "Bill", "Chris", "Scott"};
            // make a sequence of 4 invocations
            for (int i = 0; i < 4; i++) {
                System.out.println("Invoking greetMe...");
                String response = port.greetMe(names[i]);
                System.out.println("response: " + response + "\n");
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
