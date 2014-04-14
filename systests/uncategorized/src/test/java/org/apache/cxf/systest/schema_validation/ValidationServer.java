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

package org.apache.cxf.systest.schema_validation;


import java.util.LinkedList;
import java.util.List;

import javax.jws.WebService;
import javax.xml.transform.Source;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class ValidationServer extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(ValidationServer.class);

    List<Endpoint> eps = new LinkedList<Endpoint>();
    
    public ValidationServer() {
    }

    protected void run() {
        Object implementor = new SchemaValidationImpl();
        String address = "http://localhost:" + PORT + "/SoapContext";
        eps.add(Endpoint.publish(address + "/SoapPort", implementor));
        eps.add(Endpoint.publish(address + "/SoapPortValidate", new ValidatingSchemaValidationImpl()));
        eps.add(Endpoint.publish(address + "/PProvider", new PayloadProvider()));
        eps.add(Endpoint.publish(address + "/MProvider", new MessageProvider()));
    }

    public void tearDown() throws Exception {
        while (!eps.isEmpty()) {
            eps.remove(0).stop();
        }
    }
    
    @WebService(serviceName = "SchemaValidationService", 
        portName = "SoapPort",
        endpointInterface = "org.apache.schema_validation.SchemaValidation",
        targetNamespace = "http://apache.org/schema_validation",
        wsdlLocation = "classpath:/wsdl/schema_validation.wsdl")
    @SchemaValidation
    static class ValidatingSchemaValidationImpl extends SchemaValidationImpl {
        
    }


    @WebServiceProvider
    @ServiceMode(Service.Mode.PAYLOAD)
    @SchemaValidation
    static class PayloadProvider implements Provider<Source> {
        @Override
        public Source invoke(Source request) {
            return null;
        }
    }
    @WebServiceProvider
    @ServiceMode(Service.Mode.MESSAGE)
    @SchemaValidation
    static class MessageProvider implements Provider<Source> {
        @Override
        public Source invoke(Source request) {
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            ValidationServer s = new ValidationServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
