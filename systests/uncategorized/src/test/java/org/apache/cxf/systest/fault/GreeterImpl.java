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
package org.apache.cxf.systest.fault;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.headers.Header;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.intfault.BadRecordLitFault;
import org.apache.intfault.types.BareDocumentResponse;
@javax.jws.WebService(portName = "SoapPort", serviceName = "SOAPService",
                      targetNamespace = "http://apache.org/intfault",
                      endpointInterface = "org.apache.intfault.Greeter",
                      wsdlLocation = "wsdl/hello_world_fault.wsdl")
public class GreeterImpl {
    @Resource
    protected WebServiceContext context;

    public BareDocumentResponse testDocLitFault(String in) throws BadRecordLitFault {
        //System.out.println("Executing testDocLitFault sayHi\n");
        List<Header> headers = new ArrayList<>();
        Header header = null;
        try {
            header = new Header(new QName("http://test", "test"),
                                new String("test"), new JAXBDataBinding(String.class));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        headers.add(header);
        context.getMessageContext().put(Header.HEADER_LIST, headers);
        throw new BadRecordLitFault("int fault", 5);

    }

}
