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

package demo.mtom.client;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import javax.activation.DataHandler;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.mime.TestMtomPortType;
import org.apache.cxf.mime.TestMtomService;

public final class Client {

    private static final QName SERVICE_NAME = new QName("http://cxf.apache.org/mime",
        "TestMtomService");

    private static final QName PORT_NAME = new QName("http://cxf.apache.org/mime",
        "TestMtomPort");

    private Client() {
    }

    public static void main(String args[]) throws Exception {

        Client client = new Client();

        if (args.length == 0) {
            System.out.println("Please specify the WSDL file.");
            System.exit(1);
        }
        URL wsdlURL;
        File wsdlFile = new File(args[0]);

        if (wsdlFile.exists()) {
            wsdlURL = wsdlFile.toURL();
        } else {
            wsdlURL = new URL(args[0]);
        }
        System.out.println(wsdlURL);

        TestMtomService tms = new TestMtomService(wsdlURL, SERVICE_NAME);
        TestMtomPortType port = (TestMtomPortType) tms.getPort(PORT_NAME,
            TestMtomPortType.class);
        Binding binding = ((BindingProvider)port).getBinding();
        ((SOAPBinding)binding).setMTOMEnabled(true);

        URL fileURL = Client.class.getResource("me.bmp");
        File aFile = new File(new URI(fileURL.toString()));
        long fileSize = aFile.length();
        System.out.println("Filesize of me.bmp image is: " + fileSize);

        System.out.println("\nStarting MTOM Test using basic byte array:");
        Holder<String> name = new Holder<String>("Sam");
        Holder<byte[]> param = new Holder<byte[]>();
        param.value = new byte[(int) fileSize];
        InputStream in = fileURL.openStream();
        in.read(param.value);
        System.out.println("--Sending the me.bmp image to server");
        System.out.println("--Sending a name value of " + name.value);

        port.testByteArray(name, param);

        System.out.println("--Received byte[] back from server, returned size is "
            + param.value.length);
        System.out.println("--Returned string value is " + name.value);

        Image image = ImageIO.read(new ByteArrayInputStream(param.value));
        System.out.println("--Loaded image from byte[] successfully, hashCode="
            + image.hashCode());
        System.out.println("Successfully ran MTOM/byte array demo");

        System.out.println("\nStarting MTOM test with DataHandler:");
        name.value = "Bob";
        Holder<DataHandler> handler = new Holder<DataHandler>();

        handler.value = new DataHandler(fileURL);

        System.out.println("--Sending the me.bmp image to server");
        System.out.println("--Sending a name value of " + name.value);

        port.testDataHandler(name, handler);

        InputStream mtomIn = handler.value.getInputStream();
        fileSize = 0;
        for (int i = mtomIn.read(); i != -1; i = mtomIn.read()) {
            fileSize++;
        }

        System.out.println("--Received DataHandler back from server, "
            + "returned size is " + fileSize);
        System.out.println("--Returned string value is " + name.value);

        System.out.println("Successfully ran MTOM/DataHandler demo");
        System.exit(0);
    }

    private static InputStream getResourceStream(File file) throws Exception {
        InputStream in = new FileInputStream(file);
        return in;
    }
}
