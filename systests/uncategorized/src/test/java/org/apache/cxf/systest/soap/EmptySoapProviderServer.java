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
package org.apache.cxf.systest.soap;

import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.ws.BindingType;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceProvider;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class EmptySoapProviderServer extends AbstractBusTestServerBase {
    public static final String REG_PORT = allocatePort(EmptySoapProviderServer.class);

    List<Endpoint> eps = new LinkedList<>();

    protected void run() {
        String address = "http://localhost:" + REG_PORT + "/helloProvider/helloPort";
        GreeterProvider provider = new GreeterProvider();
        eps.add(Endpoint.publish(address, provider));
    }

    public void tearDown() {
        while (!eps.isEmpty()) {
            Endpoint ep = eps.remove(0);
            ep.stop();
        }
    }

    public static void main(String[] args) {
        try {
            EmptySoapProviderServer s = new EmptySoapProviderServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
    
    @WebServiceProvider(serviceName = "HelloProviderService", 
                        portName = "HelloProviderPort", 
                        targetNamespace = "http://apache.org/hello_world_xml_http/bare")
    @BindingType(value = jakarta.xml.ws.soap.SOAPBinding.SOAP11HTTP_BINDING)
    @ServiceMode(value = jakarta.xml.ws.Service.Mode.PAYLOAD)
    public class GreeterProvider implements Provider<Source> {
        public Source invoke(Source req) {
            return new StreamSource();
        }
    }
}
