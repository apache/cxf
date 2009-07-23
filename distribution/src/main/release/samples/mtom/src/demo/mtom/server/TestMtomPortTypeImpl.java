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

package demo.mtom.server;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.ws.Holder;

import org.apache.cxf.mime.TestMtomPortType;

@WebService(serviceName = "TestMtomService",
                portName = "TestMtomPort",
                endpointInterface = "org.apache.cxf.mime.TestMtomPortType",
                targetNamespace = "http://cxf.apache.org/mime")

public class TestMtomPortTypeImpl implements TestMtomPortType {


    public void testByteArray(Holder<String> name, Holder<byte[]> attachinfo) {
        System.out.println("Received image from client");
        System.out.println("The image data size is " + attachinfo.value.length);        
        name.value = "Hello " + name.value;        
    }

    public void testDataHandler(Holder<String> name, Holder<DataHandler> attachinfo) {
        try {
            System.out.println("Received image with mtom enabled from client");
            InputStream mtomIn = attachinfo.value.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            copy(mtomIn, out);
            System.out.println("The image data size is " + out.size());
            name.value = "Hello " + name.value;
            mtomIn.close();
            attachinfo.value = new DataHandler(new ByteArrayDataSource(out.toByteArray(),
                                                                       attachinfo.value.getContentType()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static int copy(final InputStream input, final OutputStream output) throws IOException {
        final byte[] buffer = new byte[4096];
        int n = 0;
        n = input.read(buffer);
        int total = 0;
        while (-1 != n) {
            output.write(buffer, 0, n);
            total += n;
            n = input.read(buffer);
        }
        return total;
    }
    
    
}
