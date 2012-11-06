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

package demo.hw_https.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.namespace.QName;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;

public final class ClientNonSpring {

    private static final QName SERVICE_NAME
        = new QName("http://apache.org/hello_world_soap_http", "SOAPService");

    private static final QName PORT_NAME =
        new QName("http://apache.org/hello_world_soap_http", "SoapPort");


    private ClientNonSpring() {
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
        Greeter port = ss.getPort(PORT_NAME, Greeter.class);        
        if ("secure".equals(args[1])) {
            setupTLS(port);
        } else if ("insecure".equals(args[1])) {
            //do nothing
        } else {
            System.out.println("arg1 needs to be either secure or insecure");
            System.exit(1);
        }
        
        System.out.println("Invoking greetMe...");
        try {
            String resp = port.greetMe(System.getProperty("user.name"));
            System.out.println("Server responded with: " + resp);
            System.out.println();

        } catch (Exception e) {
            System.out.println("Invocation failed with the following: " + e.getCause());
            System.out.println();
        }

        System.exit(0);
    }
    
    private static void setupTLS(Greeter port) 
        throws FileNotFoundException, IOException, GeneralSecurityException {
        String keyStoreLoc = "src/main/config/clientKeystore.jks";
        HTTPConduit httpConduit = (HTTPConduit) ClientProxy.getClient(port).getConduit();
 
        TLSClientParameters tlsCP = new TLSClientParameters();
        String keyPassword = "ckpass";
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStoreLoc), "cspass".toCharArray());
        KeyManager[] myKeyManagers = getKeyManagers(keyStore, keyPassword);
        tlsCP.setKeyManagers(myKeyManagers);
 
        
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(new FileInputStream(keyStoreLoc), "cspass".toCharArray());
        TrustManager[] myTrustStoreKeyManagers = getTrustManagers(trustStore);
        tlsCP.setTrustManagers(myTrustStoreKeyManagers);
        
        httpConduit.setTlsClientParameters(tlsCP);
    }

    private static TrustManager[] getTrustManagers(KeyStore trustStore) 
        throws NoSuchAlgorithmException, KeyStoreException {
        String alg = KeyManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory fac = TrustManagerFactory.getInstance(alg);
        fac.init(trustStore);
        return fac.getTrustManagers();
    }
    
    private static KeyManager[] getKeyManagers(KeyStore keyStore, String keyPassword) 
        throws GeneralSecurityException, IOException {
        String alg = KeyManagerFactory.getDefaultAlgorithm();
        char[] keyPass = keyPassword != null
                     ? keyPassword.toCharArray()
                     : null;
        KeyManagerFactory fac = KeyManagerFactory.getInstance(alg);
        fac.init(keyStore, keyPass);
        return fac.getKeyManagers();
    }

}
