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
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.ws.security.wss4j.DefaultCryptoCoverageChecker;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;

/**
 * A DOM-based server
 */
public class Server {
    private static final String WSSE_NS 
        = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String WSU_NS
        = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

    protected Server() throws Exception {
        System.out.println("Starting Server");

        Object implementor = new GreeterImpl();
        String address = "http://localhost:9000/SoapContext/GreeterPort";
        EndpointImpl endpoint = (EndpointImpl)Endpoint.publish(address, implementor);
        
        Map<String, Object> outProps = new HashMap<String, Object>();
        outProps.put("action", "UsernameToken Timestamp Signature Encrypt");

        outProps.put("passwordType", "PasswordText");
        outProps.put("passwordCallbackClass", "demo.wssec.server.UTPasswordCallback");

        outProps.put("user", "Alice");
        outProps.put("signatureUser", "serverx509v1");

        outProps.put("encryptionUser", "clientx509v1");
        outProps.put("encryptionPropFile", "etc/Server_SignVerf.properties");
        outProps.put("encryptionKeyIdentifier", "IssuerSerial");
        outProps.put("encryptionParts", "{Element}{" + WSSE_NS + "}UsernameToken;"
                         + "{Content}{http://schemas.xmlsoap.org/soap/envelope/}Body");

        outProps.put("signaturePropFile", "etc/Server_Decrypt.properties");
        outProps.put("signatureKeyIdentifier", "DirectReference");
        outProps.put("signatureParts", "{Element}{" + WSU_NS + "}Timestamp;"
                         + "{Element}{http://schemas.xmlsoap.org/soap/envelope/}Body");

        outProps.put("encryptionKeyTransportAlgorithm", 
                         "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p");
        outProps.put("signatureAlgorithm", "http://www.w3.org/2000/09/xmldsig#rsa-sha1");

        endpoint.getOutInterceptors().add(new WSS4JOutInterceptor(outProps));

        Map<String, Object> inProps = new HashMap<String, Object>();

        inProps.put("action", "UsernameToken Timestamp Signature Encrypt");
        inProps.put("passwordType", "PasswordDigest");
        inProps.put("passwordCallbackClass", "demo.wssec.server.UTPasswordCallback");

        inProps.put("decryptionPropFile", "etc/Server_Decrypt.properties");
        inProps.put("encryptionKeyIdentifier", "IssuerSerial");

        inProps.put("signaturePropFile", "etc/Server_SignVerf.properties");
        inProps.put("signatureKeyIdentifier", "DirectReference");
        inProps.put("encryptionKeyTransportAlgorithm", 
                    "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p");
        inProps.put("signatureAlgorithm", "http://www.w3.org/2000/09/xmldsig#rsa-sha1");

        endpoint.getInInterceptors().add(new WSS4JInInterceptor(inProps));

        // Check to make sure that the SOAP Body and Timestamp were signed,
        // and that the SOAP Body was encrypted
        DefaultCryptoCoverageChecker coverageChecker = new DefaultCryptoCoverageChecker();
        coverageChecker.setSignBody(true);
        coverageChecker.setSignTimestamp(true);
        coverageChecker.setEncryptBody(true);
        endpoint.getInInterceptors().add(coverageChecker);

    }

    public static void main(String args[]) throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = Server.class.getResource("wssec.xml");
        Bus bus = bf.createBus(busFile.toString());

        
        BusFactory.setDefaultBus(bus);

        new Server();
        System.out.println("Server ready...");

        Thread.sleep(5 * 60 * 1000);

        bus.shutdown(true);
        System.out.println("Server exiting");
        System.exit(0);
    }
}
