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
package org.apache.cxf.systest.jms;

import java.io.IOException;
import java.net.URL;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.apache.cxf.jms_mtom.JMSMTOMPortType;

@WebService(serviceName = "JMSMTOMService",
            portName = "JMSMTOMPortType",
            endpointInterface = "org.apache.cxf.jms_mtom.JMSMTOMPortType",
            targetNamespace = "http://cxf.apache.org/jms_mtom",
            wsdlLocation = "testutils/jms_test_mtom.wsdl")
public class JMSMTOMImpl implements JMSMTOMPortType {

    public void testDataHandler(Holder<String> name, Holder<DataHandler> attachinfo) {
        System.out.println(name.value);
        try {
            System.out.println(attachinfo.value.getInputStream().available());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("TestDataHandler End");
    }


    public DataHandler testOutMtom() {
        URL fileURL = this.getClass().getResource("/org/apache/cxf/systest/jms/JMSClientServerTest.class");
        return new DataHandler(fileURL);
    }
}
