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


import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import jakarta.jms.BytesMessage;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.apache.cxf.jaxrs.ext.Oneway;
import org.apache.cxf.jaxrs.ext.ProtocolHeaders;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;

@Path("/bookstore")
public class JMSBookStore {

    @jakarta.ws.rs.core.Context
    private ProtocolHeaders headers;

    private Map<Long, Book> books = new HashMap<>();



    public JMSBookStore() {
        books.put(123L, new Book("CXF JMS Rocks", 123L));
    }


    @GET
    @Path("/bookidarray")
    @Produces("application/xml")
    public Book getBookByURLQuery(@QueryParam("id") String[] ids) throws Exception {
        if (ids == null || ids.length != 3) {
            throw new WebApplicationException();
        }
        return doGetBook(ids[0] + ids[1] + ids[2]);
    }

    @GET
    @Path("/books/{bookId}/")
    @Produces("application/xml")
    public Book getBook(@PathParam("bookId") String id) throws BookNotFoundFault {
        return doGetBook(id);
    }

    @Path("/booksubresource/{bookId}/")
    public Book getBookSubResource(@PathParam("bookId") String id) throws BookNotFoundFault {
        return doGetBook(id);
    }

    private Book doGetBook(String id) throws BookNotFoundFault {
        Book book = books.get(Long.parseLong(id));
        if (book != null) {
            return book;
        }
        BookNotFoundDetails details = new BookNotFoundDetails();
        details.setId(Long.parseLong(id));
        throw new BookNotFoundFault(details);
    }

    @POST
    @Path("/books")
    @Produces("text/xml")
    @Consumes("application/xml")
    public Response addBook(Book book) {

        String ct1 = headers.getRequestHeaderValue("Content-Type");
        String ct2 = headers.getRequestHeader("Content-Type").get(0);
        String ct3 = headers.getRequestHeaders().getFirst("Content-Type");
        if (!("application/xml".equals(ct1) && ct1.equals(ct2) && ct1.equals(ct3))) {
            throw new RuntimeException("Unexpected content type");
        }
        if (!"custom.value".equals(headers.getRequestHeaderValue("custom.protocol.header"))) {
            throw new RuntimeException("Custom header is not set");
        }

        book.setId(124);
        books.put(book.getId(), book);

        return Response.ok(book).build();
    }

    @PUT
    @Path("/oneway")
    @Consumes()
    @Oneway
    public void onewayRequest(Book book) throws Exception {

        Context ctx = getContext();
        ConnectionFactory factory = (ConnectionFactory)ctx.lookup("ConnectionFactory");
        Destination replyToDestination = (Destination)ctx.lookup("dynamicQueues/test.jmstransport.response");

        Connection connection = null;
        try {
            connection = factory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            postOneWayBook(session, replyToDestination, book);
            session.close();
        } finally {
            try {
                if (connection != null) {
                    connection.stop();
                    connection.close();
                }
            } catch (JMSException ex) {
                // ignore
            }
        }
    }

    private Context getContext() throws Exception {
        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                          "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory");
        props.setProperty(Context.PROVIDER_URL, "tcp://localhost:" + EmbeddedJMSBrokerLauncher.PORT);
        return new InitialContext(props);

    }

    private void postOneWayBook(Session session, Destination destination, Book book)
        throws Exception {
        MessageProducer producer = session.createProducer(destination);
        BytesMessage message = session.createBytesMessage();
        message.writeBytes(writeBook(book));
        producer.send(message);
        producer.close();
    }

    private byte[] writeBook(Book b) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Marshaller m = c.createMarshaller();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        m.marshal(b, bos);
        return bos.toByteArray();
    }
}


