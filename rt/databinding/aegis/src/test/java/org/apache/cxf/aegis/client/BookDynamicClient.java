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
package org.apache.cxf.aegis.client;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.type.basic.ArrayType;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.service.Service;

public final class BookDynamicClient {
    static final String ENCODED_NS = Soap11.getInstance().getSoapEncodingStyle();
    
    private BookDynamicClient() {
        //utility class
    }

    public static void main(String args[]) {
        // String serviceURL = "http://localhost:8080/BookService";
        try {
            Client client = new ClientImpl(new URL("http://localhost:6980/BookService?WSDL"));

            Service s = client.getEndpoint().getService();
            AegisDatabinding db = new AegisDatabinding();
            s.setDataBinding(db);
            db.initialize(s);
            
            TypeMapping tm = (TypeMapping) s.get(TypeMapping.class.getName());
            BeanType type = new BeanType();
            type.setSchemaType(new QName("http://org.codehaus.xfire.client", "Book"));
            type.setTypeClass(Book.class);
            type.setTypeMapping(tm);

            System.out.println(type);

            tm.register(type);

            ArrayType aType = new ArrayType();
            aType.setTypeClass(Book[].class);
            aType.setSchemaType(new QName("http://client.xfire.codehaus.org", "ArrayOfBook"));
            aType.setTypeMapping(tm);
            tm.register(aType);

            QName qn = tm.getTypeQName(Book.class);
            System.out.println("QName(" + tm.isRegistered(Book.class) + ") = " + qn);

            Book book = new Book();

            book.setAuthor("Dan");
            book.setIsbn("1");
            book.setTitle("XFire in Action");
            // client.invoke("addBook", new Object[] {book});

            book.setAuthor("Dierk");
            book.setIsbn("2");
            book.setTitle("Groovy in Action");
            // client.invoke("addBook", new Object[] {book});

            Book[] books = (Book[])client.invoke("getBooks", new Object[] {})[0];

            System.out.println("BOOKS:");

            for (int i = 0; i < books.length; i++) {
                System.out.println(books[i].getTitle());
            }
            /*
             * Book[] books = (Book [])client.invoke("findBook", new Object[]
             * {"2"});; System.out.println("ISBN :");
             * System.out.println(books[0].getAuthor());
             */
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
