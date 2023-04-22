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
package org.apache.cxf.mtom_xop;

import java.io.InputStream;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.soap.AttachmentPart;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPBodyElement;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.Service.Mode;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceProvider;
import jakarta.xml.ws.soap.MTOM;


@WebServiceProvider(portName = "TestMtomProviderPort",
serviceName = "TestMtomService",
targetNamespace = "http://cxf.apache.org/mime",
wsdlLocation = "testutils/mtom_xop.wsdl")
@ServiceMode(value = Mode.MESSAGE)
@MTOM

public class TestMtomProviderImpl implements Provider<SOAPMessage> {

    public SOAPMessage invoke(final SOAPMessage request) {
        try {
            System.out.println("=== Received client request ===");

            // create the SOAPMessage
            SOAPMessage message = MessageFactory.newInstance().createMessage();
            SOAPPart part = message.getSOAPPart();
            SOAPEnvelope envelope = part.getEnvelope();
            SOAPBody body = envelope.getBody();


            SOAPBodyElement testResponse = body
                .addBodyElement(envelope.createName("testXopResponse", null, "http://cxf.apache.org/mime/types"));
            SOAPElement name = testResponse.addChildElement("name", null, "http://cxf.apache.org/mime/types");
            name.setTextContent("return detail + call detail");
            SOAPElement attachinfo = testResponse.addChildElement(
                                         "attachinfo", null, "http://cxf.apache.org/mime/types");
            SOAPElement include = attachinfo.addChildElement("Include", "xop",
                                                                "http://www.w3.org/2004/08/xop/include");

            int fileSize = 0;
            try (InputStream pre = this.getClass().getResourceAsStream("/wsdl/mtom_xop.wsdl")) {
                for (int i = pre.read(); i != -1; i = pre.read()) {
                    fileSize++;
                }
            }

            int count = 50;
            byte[] data = new byte[fileSize *  count];
            for (int x = 0; x < count; x++) {
                this.getClass().getResourceAsStream("/wsdl/mtom_xop.wsdl").read(data,
                                                                                fileSize * x,
                                                                                fileSize);
            }


            DataHandler dh = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));

            // create the image attachment
            AttachmentPart attachment = message.createAttachmentPart(dh);
            attachment.setContentId("mtom_xop.wsdl");
            message.addAttachmentPart(attachment);
            System.out
                .println("Adding attachment: " + attachment.getContentId() + ":" + attachment.getSize());

            // add the reference to the image attachment
            include.addAttribute(envelope.createName("href"), "cid:" + attachment.getContentId());

            return message;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
