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


package demo.callback.client;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.callback.SOAPService;
import org.apache.callback.ServerPortType;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;





public final class Client {

    private static final QName SERVICE_NAME 
        = new QName("http://apache.org/callback", "SOAPService");

    private Client() {
    } 

    public static void main(String args[]) throws Exception {
        
        
        Object implementor = new CallbackImpl();
        String address = "http://localhost:9005/CallbackContext/CallbackPort";
        Endpoint endpoint = Endpoint.publish(address, implementor);
        
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
        
        SOAPService ss = new SOAPService(wsdlURL, SERVICE_NAME);
        ServerPortType port = ss.getSOAPPort();
        
        InputStream is = demo.callback.client.Client.class.getResourceAsStream("/callback_infoset.xml");
        Document doc = StaxUtils.read(is);
        Element referenceParameters = DOMUtils.findChildWithAtt(doc.getDocumentElement(),
                                                                "wsa:ReferenceParameters",
                                                                "name", "");
        W3CEndpointReference ref = (W3CEndpointReference)endpoint.getEndpointReference(referenceParameters);
        

        String resp = port.registerCallback(ref);
        System.out.println("Response from server: " + resp);
        
        System.exit(0); 
    }

}
