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

package demo.wssec.server;

import java.net.URL;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.ws.security.wss4j.StaxCryptoCoverageChecker;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxOutInterceptor;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.xml.security.stax.ext.SecurePart;

/**
 * A StAX-based server
 */
public class StaxServer {

    protected StaxServer() throws Exception {
        System.out.println("Starting StaxServer");

        Object implementor = new GreeterImpl();
        String address = "http://localhost:9000/SoapContext/GreeterPort";
        Endpoint.publish(address, implementor);
    }

    public static void main(String args[]) throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = Server.class.getResource("wssec.xml");
        Bus bus = bf.createBus(busFile.toString());

        Properties decCryptoProperties = 
            CryptoFactory.getProperties("etc/Server_Decrypt.properties", StaxServer.class.getClassLoader());
        Properties sigVerCryptoProperties = 
            CryptoFactory.getProperties("etc/Server_SignVerf.properties", StaxServer.class.getClassLoader());
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.addAction(WSSConstants.USERNAMETOKEN);
        properties.addAction(WSSConstants.TIMESTAMP);
        properties.addAction(WSSConstants.SIGNATURE);
        properties.addAction(WSSConstants.ENCRYPT);

        properties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_TEXT);
        properties.setTokenUser("Alice");
        properties.setSignatureUser("serverx509v1");
        properties.setEncryptionUser("clientx509v1");
        
        properties.setEncryptionCryptoProperties(sigVerCryptoProperties);
        properties.setEncryptionKeyIdentifier(
            WSSecurityTokenConstants.KeyIdentifier_IssuerSerial
        );
        properties.setEncryptionKeyTransportAlgorithm(
            "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"
        );
        properties.addEncryptionPart(
            new SecurePart(new QName(WSSConstants.NS_WSSE10, "UsernameToken"), SecurePart.Modifier.Element)
        );
        properties.addEncryptionPart(
            new SecurePart(new QName(WSSConstants.NS_SOAP11, "Body"), SecurePart.Modifier.Content)
        );
        
        properties.setSignatureCryptoProperties(decCryptoProperties);
        properties.setSignatureKeyIdentifier(
            WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE
        );
        properties.setSignatureAlgorithm("http://www.w3.org/2000/09/xmldsig#rsa-sha1");
        properties.addSignaturePart(
            new SecurePart(new QName(WSSConstants.NS_WSU10, "Timestamp"), SecurePart.Modifier.Element)
        );
        properties.addSignaturePart(
            new SecurePart(new QName(WSSConstants.NS_SOAP11, "Body"), SecurePart.Modifier.Content)
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
        inProperties.setDecryptionCryptoProperties(decCryptoProperties);
        inProperties.setSignatureVerificationCryptoProperties(sigVerCryptoProperties);
        
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        bus.getInInterceptors().add(inhandler);

        // Check to make sure that the SOAP Body and Timestamp were signed,
        // and that the SOAP Body was encrypted
        StaxCryptoCoverageChecker coverageChecker = new StaxCryptoCoverageChecker();
        coverageChecker.setSignBody(true);
        coverageChecker.setSignTimestamp(true);
        coverageChecker.setEncryptBody(true);
        bus.getInInterceptors().add(coverageChecker);

        BusFactory.setDefaultBus(bus);

        new StaxServer();
        System.out.println("StaxServer ready...");

        Thread.sleep(5 * 60 * 1000);

        bus.shutdown(true);
        System.out.println("StaxServer exiting");
        System.exit(0);
    }
}
