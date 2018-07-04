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
package sample.ws.service;


import java.io.StringReader;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.ws.client.core.WebServiceTemplate;


import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import sample.ws.SampleWsApplication;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = SampleWsApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class SampleWsApplicationTest {

    //CHECKSTYLE:OFF
    @Rule
    public OutputCapture output = new OutputCapture(); // SUPPRESS CHECKSTYLE
    //CHECKSTYLE:ON

    private WebServiceTemplate webServiceTemplate = new WebServiceTemplate();

    @LocalServerPort
    private int port;

    @Before
    public void setUp() {
        this.webServiceTemplate.setDefaultUri("http://localhost:" + this.port + "/Service/Hello");
    }

    @Test
    public void testHelloRequest() {
        // final String request =
        // "<q0:sayHello xmlns:q0=\"http://service.ws.sample\">Elan</q0:sayHello>";
        String request = "<q0:sayHello xmlns:q0=\"http://service.ws.sample/\"><myname>Elan</myname></q0:sayHello>";

        StreamSource source = new StreamSource(new StringReader(request));
        StreamResult result = new StreamResult(System.out);

        this.webServiceTemplate.sendSourceAndReceiveToResult(source, result);
        assertThat(this.output.toString(),
                   containsString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                  + "<ns2:sayHelloResponse xmlns:ns2=\"http://service.ws.sample/\">"
                                  + "<return>Hello, Welcome to CXF Spring boot Elan!!!</return>"
                                  + "</ns2:sayHelloResponse>"));
    }

}
