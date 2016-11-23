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
package sample.ws;

import java.io.StringReader;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;


//CHECKSTYLE:OFF
public class SampleWsApplicationClient {
 
    public static void main(String[] args) {
        String address = "http://localhost:8080/Service/Hello";
        // final String request =
        // "<q0:sayHello xmlns:q0=\"http://service.ws.sample\">Elan</q0:sayHello>";
        String request = "<q0:sayHello xmlns:q0=\"http://service.ws.sample/\"><myname>Elan</myname></q0:sayHello>";

        StreamSource source = new StreamSource(new StringReader(request));
        StreamResult result = new StreamResult(System.out);

        //assertThat(this.output.toString(),
        //           containsString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        //                          + "<ns2:sayHelloResponse xmlns:ns2=\"http://service.ws.sample/\">"
        //                          + "<return>Hello, Welcome to CXF Spring boot Elan!!!</return>"
        //                          + "</ns2:sayHelloResponse>"));
    }

}
//CHECKSTYLE:ON

