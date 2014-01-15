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
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.hello_world_soap_http.Greeter;
import org.apache.cxf.hello_world_soap_http.GreeterService;
import org.apache.cxf.ws.security.wss4j.DefaultCryptoCoverageChecker;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;

/**
 * A DOM-based client
 */
public final class Client {

    private static final String WSSE_NS 
        = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String WSU_NS
        = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

    private Client() {
    }

    public static void main(String args[]) throws Exception {
        try {

            SpringBusFactory bf = new SpringBusFactory();
            URL busFile = Client.class.getResource("wssec.xml");
            Bus bus = bf.createBus(busFile.toString());
            BusFactory.setDefaultBus(bus);

            Map<String, Object> outProps = new HashMap<String, Object>();
            outProps.put("action", "UsernameToken Timestamp Signature Encrypt");

            outProps.put("passwordType", "PasswordDigest");

            outProps.put("user", "abcd");
            outProps.put("signatureUser", "clientx509v1");

            outProps.put("passwordCallbackClass", "demo.wssec.client.UTPasswordCallback");

            outProps.put("encryptionUser", "serverx509v1");
            outProps.put("encryptionPropFile", "etc/Client_Encrypt.properties");
            outProps.put("encryptionKeyIdentifier", "IssuerSerial");
            outProps.put("encryptionParts",
                         "{Element}{" + WSSE_NS + "}UsernameToken;"
                         + "{Content}{http://schemas.xmlsoap.org/soap/envelope/}Body");

            outProps.put("signaturePropFile", "etc/Client_Sign.properties");
            outProps.put("signatureKeyIdentifier", "DirectReference");
            outProps.put("signatureParts",
                         "{Element}{" + WSU_NS + "}Timestamp;"
                         + "{Element}{http://schemas.xmlsoap.org/soap/envelope/}Body;"
                         + "{}{http://www.w3.org/2005/08/addressing}ReplyTo;");

            outProps.put("encryptionKeyTransportAlgorithm", 
                         "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p");
            outProps.put("signatureAlgorithm", "http://www.w3.org/2000/09/xmldsig#rsa-sha1");


            Map<String, Object> inProps = new HashMap<String, Object>();

            inProps.put("action", "UsernameToken Timestamp Signature Encrypt");
            inProps.put("passwordType", "PasswordText");
            inProps.put("passwordCallbackClass", "demo.wssec.client.UTPasswordCallback");

            inProps.put("decryptionPropFile", "etc/Client_Sign.properties");
            inProps.put("encryptionKeyIdentifier", "IssuerSerial");

            inProps.put("signaturePropFile", "etc/Client_Encrypt.properties");
            inProps.put("signatureKeyIdentifier", "DirectReference");

            inProps.put("encryptionKeyTransportAlgorithm", 
                         "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p");
            inProps.put("signatureAlgorithm", "http://www.w3.org/2000/09/xmldsig#rsa-sha1");


            // Check to make sure that the SOAP Body and Timestamp were signed,
            // and that the SOAP Body was encrypted
            DefaultCryptoCoverageChecker coverageChecker = new DefaultCryptoCoverageChecker();
            coverageChecker.setSignBody(true);
            coverageChecker.setSignTimestamp(true);
            coverageChecker.setEncryptBody(true);

            GreeterService service = new GreeterService();
            Greeter port = service.getGreeterPort();
            org.apache.cxf.endpoint.Client client = ClientProxy.getClient(port);
            client.getInInterceptors().add(new WSS4JInInterceptor(inProps));
            client.getOutInterceptors().add(new WSS4JOutInterceptor(outProps));
            client.getInInterceptors().add(coverageChecker);
            

            String[] names = new String[] {"Anne", "Bill", "Chris", "Sachin Tendulkar"};
            // make a sequence of 4 invocations
            for (int i = 0; i < 4; i++) {
                System.out.println("Invoking greetMe...");
                String response = port.greetMe(names[i]);
                System.out.println("response: " + response + "\n");
            }

            // allow asynchronous resends to occur
            Thread.sleep(10 * 1000);

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
