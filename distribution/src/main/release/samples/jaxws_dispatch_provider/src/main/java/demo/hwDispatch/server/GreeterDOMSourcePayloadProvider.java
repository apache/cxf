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

package demo.hwDispatch.server;

import java.io.InputStream;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceProvider;

@WebServiceProvider()
public class GreeterDOMSourcePayloadProvider implements Provider<DOMSource> {

    public GreeterDOMSourcePayloadProvider() {
        //Complete
    }

    public DOMSource invoke(DOMSource request) {
        DOMSource response = new DOMSource();
        try {
            System.out.println("Incoming Client Request as a DOMSource data in PAYLOAD Mode");
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = transformerFactory.newTransformer();
            StreamResult result = new StreamResult(System.out);
            transformer.transform(request, result);
            System.out.println("\n");
            
            InputStream is = getClass().getResourceAsStream("/GreetMeDocLiteralResp3.xml");
            
            SOAPMessage greetMeResponse =  MessageFactory.newInstance().createMessage(null, is);
            is.close();            
            response.setNode(greetMeResponse.getSOAPBody().extractContentAsDocument());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return response;
    }
}
