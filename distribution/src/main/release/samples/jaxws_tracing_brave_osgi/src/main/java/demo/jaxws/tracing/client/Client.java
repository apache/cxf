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


package demo.jaxws.tracing.client;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;

import demo.jaxws.tracing.server.Book;
import demo.jaxws.tracing.server.CatalogService;

public final class Client {

    private static final QName SERVICE_NAME
        = new QName("http://impl.server.tracing.jaxws.demo/", "CatalogServiceImplService");


    private Client() {
    }

    public static void main(String[] args) throws Exception {

        URL wsdl = new URL("http://localhost:8181/cxf/catalog?wsdl");

        Service service = Service.create(wsdl, SERVICE_NAME);
        QName portQName = new QName("http://impl.server.tracing.jaxws.demo/", "CatalogServiceImplPort");
        CatalogService port =
                service.getPort(portQName, CatalogService.class);

        Book book = new Book("1", "New Book");
        System.out.println("Adding a book");
        port.addBook(book);

        System.exit(0);

    }

}
