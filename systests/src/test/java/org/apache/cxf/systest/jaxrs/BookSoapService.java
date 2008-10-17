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
package org.apache.cxf.systest.jaxrs;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;

/**
 * 
 */

@WebServiceClient(name = "BookService", 
                  targetNamespace = "http://books.com")
public class BookSoapService extends Service {
    static final QName SERVICE = new QName("http://books.com", "BookService");
    static final QName BOOK_PORT = 
        new QName("http://books.com", "BookPort");
    public BookSoapService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    @WebEndpoint(name = "BookPort")
    public BookStoreJaxrsJaxws getBookPort() {
        return (BookStoreJaxrsJaxws)super.getPort(BOOK_PORT, 
                                                  BookStoreJaxrsJaxws.class);
    }

}
