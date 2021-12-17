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

import javax.xml.transform.dom.DOMSource;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceProvider;

@WebServiceProvider()
@ServiceMode(value = Service.Mode.MESSAGE)
public class GreeterDOMSourceMessageProvider implements Provider<DOMSource> {

    public GreeterDOMSourceMessageProvider() {
        //Complete
    }

    public DOMSource invoke(DOMSource request) {
        DOMSource response = new DOMSource();
        try {
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage soapReq = factory.createMessage();
            soapReq.getSOAPPart().setContent(request);

            System.out.println("Incoming Client Request as a DOMSource data in MESSAGE Mode");
            soapReq.writeTo(System.out);
            System.out.println("\n");

            SOAPMessage greetMeResponse = null;
            try (InputStream is = getClass().getResourceAsStream("/GreetMeDocLiteralResp2.xml")) {
                greetMeResponse = factory.createMessage(null, is);
            }
            response.setNode(greetMeResponse.getSOAPPart());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return response;
    }
}
