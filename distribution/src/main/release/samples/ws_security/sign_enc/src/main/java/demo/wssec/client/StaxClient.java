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
import java.util.Properties;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.hello_world_soap_http.Greeter;
import org.apache.cxf.hello_world_soap_http.GreeterService;
import org.apache.cxf.ws.security.wss4j.StaxCryptoCoverageChecker;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxOutInterceptor;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.xml.security.stax.ext.SecurePart;

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
            URL busFile = StaxClient.class.getResource("wssec.xml");
            Bus bus = bf.createBus(busFile.toString());
            BusFactory.setDefaultBus(bus);

            Properties encCryptoProperties = 
                CryptoFactory.getProperties("etc/Client_Encrypt.properties",
                    StaxClient.class.getClassLoader());
            Properties sigCryptoProperties = 
                CryptoFactory.getProperties("etc/Client_Sign.properties", StaxClient.class.getClassLoader());
            
            WSSSecurityProperties properties = new WSSSecurityProperties();
            properties.addAction(WSSConstants.USERNAMETOKEN);
            properties.addAction(WSSConstants.TIMESTAMP);
            properties.addAction(WSSConstants.SIGNATURE);
            properties.addAction(WSSConstants.ENCRYPT);

            properties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_DIGEST);
            properties.setTokenUser("abcd");
            properties.setSignatureUser("clientx509v1");
            properties.setEncryptionUser("serverx509v1");
            
            properties.setEncryptionCryptoProperties(encCryptoProperties);
            properties.setEncryptionKeyIdentifier(
                WSSecurityTokenConstants.KeyIdentifier_IssuerSerial
            );
            properties.setEncryptionKeyTransportAlgorithm(
                "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"
            );
            properties.addEncryptionPart(
                new SecurePart(new QName(WSSConstants.NS_WSSE10,
                    "UsernameToken"), SecurePart.Modifier.Element)
            );
            properties.addEncryptionPart(
                new SecurePart(new QName(WSSConstants.NS_SOAP11, "Body"), SecurePart.Modifier.Content)
            );
            
            properties.setSignatureCryptoProperties(sigCryptoProperties);
            properties.setSignatureKeyIdentifier(
                WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE
            );
            properties.setSignatureAlgorithm("http://www.w3.org/2000/09/xmldsig#rsa-sha1");
            properties.addSignaturePart(
                new SecurePart(new QName(WSSConstants.NS_WSU10, "Timestamp"), SecurePart.Modifier.Element)
            );
            properties.addSignaturePart(
                new SecurePart(new QName(WSSConstants.NS_SOAP11, "Body"), SecurePart.Modifier.Element)
            );
            properties.addSignaturePart(
                new SecurePart(new QName("http://www.w3.org/2005/08/addressing", "ReplyTo"),
                    SecurePart.Modifier.Element)
            );
            properties.setCallbackHandler(new UTPasswordCallback());
            
            WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
            bus.getOutInterceptors().add(ohandler);
            
            WSSSecurityProperties inProperties = new WSSSecurityProperties();
            inProperties.addAction(WSSConstants.USERNAMETOKEN);
            inProperties.addAction(WSSConstants.TIMESTAMP);
            inProperties.addAction(WSSConstants.SIGNATURE);
            inProperties.addAction(WSSConstants.ENCRYPT);

            inProperties.setCallbackHandler(new UTPasswordCallback());
            inProperties.setDecryptionCryptoProperties(sigCryptoProperties);
            inProperties.setSignatureVerificationCryptoProperties(encCryptoProperties);
            
            WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
            bus.getInInterceptors().add(inhandler);

            // Check to make sure that the SOAP Body and Timestamp were signed,
            // and that the SOAP Body was encrypted
            StaxCryptoCoverageChecker coverageChecker = new StaxCryptoCoverageChecker();
            coverageChecker.setSignBody(true);
            coverageChecker.setSignTimestamp(true);
            coverageChecker.setEncryptBody(true);
            bus.getInInterceptors().add(coverageChecker);

            GreeterService service = new GreeterService();
            Greeter port = service.getGreeterPort();

            String[] names = new String[] {"Anne", "Bill", "Chris", "Sachin Tendulkar"};
            // make a sequence of 4 invocations
            for (int i = 0; i < 4; i++) {
                System.out.println("Invoking greetMe...");
                String response = port.greetMe(names[i]);
                System.out.println("response: " + response + "\n");
            }

            // allow asynchronous resends to occur
            Thread.sleep(30 * 1000);

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
