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

package org.apache.cxf.systest.rest;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import org.apache.cxf.binding.http.HttpBindingFactory;
import org.apache.cxf.customer.book.BookService;
import org.apache.cxf.customer.book.BookServiceImpl;
import org.apache.cxf.customer.book.BookServiceWrapped;
import org.apache.cxf.customer.book.BookServiceWrappedImpl;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.codehaus.jettison.mapped.MappedXMLInputFactory;
import org.codehaus.jettison.mapped.MappedXMLOutputFactory;
    
public class BookServer extends AbstractBusTestServerBase {

    protected void run() {
        //book service in unwrapped style
        BookServiceImpl serviceObj = new BookServiceImpl();
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceClass(BookService.class);
        // Use the HTTP Binding which understands the Java Rest Annotations
        sf.setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        sf.setAddress("http://localhost:9080/xml/");
        sf.getServiceFactory().setInvoker(new BeanInvoker(serviceObj));

        // Turn the "wrapped" style off. This means that CXF won't generate
        // wrapper XML elements and we'll have prettier XML text. This
        // means that we need to stick to one request and one response
        // parameter though.
        sf.getServiceFactory().setWrapped(false);

        sf.create();
        
        //book service in wrapped style
        BookServiceWrappedImpl serviceWrappedObj = new BookServiceWrappedImpl();
        JaxWsServerFactoryBean sfWrapped = new JaxWsServerFactoryBean();
        sfWrapped.setServiceClass(BookServiceWrapped.class);
        // Use the HTTP Binding which understands the Java Rest Annotations
        sfWrapped.setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        sfWrapped.setAddress("http://localhost:9080/xmlwrapped");
        sfWrapped.getServiceFactory().setInvoker(new BeanInvoker(serviceWrappedObj));
        sfWrapped.create();
        
        
        JaxWsServerFactoryBean sfJson = new JaxWsServerFactoryBean();
        sfJson.setServiceClass(BookService.class);
        // Use the HTTP Binding which understands the Java Rest Annotations
        sfJson.setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        sfJson.setAddress("http://localhost:9080/json");
        sfJson.getServiceFactory().setInvoker(new BeanInvoker(serviceObj));

        // Turn the "wrapped" style off. This means that CXF won't generate
        // wrapper JSON elements and we'll have prettier JSON text. This
        // means that we need to stick to one request and one response
        // parameter though.
        sfJson.getServiceFactory().setWrapped(false);

        // Tell CXF to use a different Content-Type for the JSON endpoint
        // This should probably be application/json, but text/plain allows
        // us to view easily in a web browser.
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("Content-Type", "text/plain");

        // Set up the JSON StAX implementation
        Map<String, String> nstojns = new HashMap<String, String>();
        nstojns.put("http://book.acme.com", "acme");

        MappedXMLInputFactory xif = new MappedXMLInputFactory(nstojns);
        properties.put(XMLInputFactory.class.getName(), xif);

        MappedXMLOutputFactory xof = new MappedXMLOutputFactory(nstojns);
        properties.put(XMLOutputFactory.class.getName(), xof);

        sfJson.setProperties(properties);

        sfJson.create();
    }

    public static void main(String[] args) {
        try {
            BookServer s = new BookServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
