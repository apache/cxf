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


import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.ws.rs.ConsumeMime;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.utils.multipart.AttachmentUtils;

@Path("/bookstore")
public class MultipartStore {

    @Context
    private MessageContext context;
    
    public MultipartStore() {
    }
    
    @POST
    @Path("/books/stream")
    @ProduceMime("text/xml")
    public Response addBookFromStream(StreamSource source) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Unmarshaller u = c.createUnmarshaller();
        Book b = (Book)u.unmarshal(source);
        b.setId(124);
        return Response.ok(b).build();
    }
    
    @POST
    @Path("/books/form")
    @ConsumeMime("multipart/form-data")
    @ProduceMime("text/xml")
    public Response addBookFromForm(MultivaluedMap<String, String> data) throws Exception {
        Book b = new Book();
        b.setId(Long.valueOf(data.getFirst("id")));
        b.setName(data.getFirst("name"));
        return Response.ok(b).build();
    }
    
    @POST
    @Path("/books/formbody")
    @ConsumeMime("multipart/form-data")
    @ProduceMime("text/xml")
    public Response addBookFromFormBody(MultipartBody body) throws Exception {
        MultivaluedMap<String, String> data = AttachmentUtils.populateFormMap(context);
        Book b = new Book();
        b.setId(Long.valueOf(data.getFirst("id")));
        b.setName(data.getFirst("name"));
        return Response.ok(b).build();
    }
    
    @POST
    @Path("/books/formbody2")
    @ConsumeMime("multipart/form-data")
    @ProduceMime("text/xml")
    public Response addBookFromFormBody2() throws Exception {
        return addBookFromFormBody(AttachmentUtils.getMultipartBody(context));
    }
    
    
    @POST
    @Path("/books/istream")
    @ProduceMime("text/xml")
    public Response addBookFromInputStream(InputStream is) throws Exception {
        return readBookFromInputStream(is);
    }
    
    @POST
    @Path("/books/dsource")
    @ProduceMime("text/xml")
    public Response addBookFromDataSource(DataSource ds) throws Exception {
        return readBookFromInputStream(ds.getInputStream());
    }
    
    @POST
    @Path("/books/jaxb2")
    @ConsumeMime("multipart/related;type=\"text/xml\"")
    @ProduceMime("text/xml")
    public Response addBookParts(@Multipart("rootPart") Book b1,
                                 @Multipart("book2") Book b2) 
        throws Exception {
        if (b1.equals(b2)) {
            throw new WebApplicationException();
        }
        if (!b1.getName().equals(b2.getName())) {
            throw new WebApplicationException();
        }
        b1.setId(124);
        return Response.ok(b1).build();
    }
    
    @POST
    @Path("/books/jaxbjson")
    @ProduceMime("text/xml")
    public Response addBookJaxbJson(
        @Multipart(value = "rootPart", type = "text/xml") Book2 b1,
        @Multipart(value = "book2", type = "application/json") Book b2) 
        throws Exception {
        if (!"CXF in Action".equals(b1.getName())
            || !"CXF in Action - 2".equals(b2.getName())) {
            throw new WebApplicationException();
        }
        b2.setId(124);
        return Response.ok(b2).build();
    }
    
    @POST
    @Path("/books/dsource2")
    @ProduceMime("text/xml")
    public Response addBookFromDataSource2(@Multipart("rootPart") DataSource ds1,
                                           @Multipart("book2") DataSource ds2) 
        throws Exception {
        Response r1 = readBookFromInputStream(ds1.getInputStream());
        Response r2 = readBookFromInputStream(ds2.getInputStream());
        Book b1 = (Book)r1.getEntity();
        Book b2 = (Book)r2.getEntity();
        if (b1.equals(b2)) {
            throw new WebApplicationException();
        }
        if (!b1.getName().equals(b2.getName())) {
            throw new WebApplicationException();
        }
        return r1;
    }
    
    @POST
    @Path("/books/listattachments")
    @ProduceMime("text/xml")
    public Response addBookFromListOfAttachments(List<Attachment> atts)  
        throws Exception {
        Response r1 = readBookFromInputStream(atts.get(0).getDataHandler().getInputStream());
        Response r2 = readBookFromInputStream(atts.get(2).getDataHandler().getInputStream());
        Book b1 = (Book)r1.getEntity();
        Book b2 = (Book)r2.getEntity();
        if (b1.equals(b2)) {
            throw new WebApplicationException();
        }
        if (!b1.getName().equals(b2.getName())) {
            throw new WebApplicationException();
        }
        return r1;
    }
    
    @POST
    @Path("/books/body")
    @ProduceMime("text/xml")
    public Response addBookFromListOfAttachments(MultipartBody body)  
        throws Exception {
        return addBookFromListOfAttachments(body.getAllAttachments());
    }
    
    @POST
    @Path("/books/lististreams")
    @ProduceMime("text/xml")
    public Response addBookFromListOfStreams(List<InputStream> atts)  
        throws Exception {
        Response r1 = readBookFromInputStream(atts.get(0));
        Response r2 = readBookFromInputStream(atts.get(2));
        Book b1 = (Book)r1.getEntity();
        Book b2 = (Book)r2.getEntity();
        if (b1.equals(b2)) {
            throw new WebApplicationException();
        }
        if (!b1.getName().equals(b2.getName())) {
            throw new WebApplicationException();
        }
        return r1;
    }
    
    @POST
    @Path("/books/dhandler")
    @ProduceMime("text/xml")
    public Response addBookFromDataHandler(DataHandler dh) throws Exception {
        return readBookFromInputStream(dh.getInputStream());
    }
    
    @POST
    @Path("/books/attachment")
    @ProduceMime("text/xml")
    public Response addBookFromAttachment(Attachment a) throws Exception {
        return readBookFromInputStream(a.getDataHandler().getInputStream());
    }
    
    @POST
    @Path("/books/mchandlers")
    @ProduceMime("text/xml")
    public Response addBookFromMessageContext() throws Exception {
        Map<String, Attachment> handlers = AttachmentUtils.getAttachmentsMap(context);
        for (Map.Entry<String, Attachment> entry : handlers.entrySet()) {
            if (entry.getKey().equals("book2")) {
                return readBookFromInputStream(entry.getValue().getDataHandler().getInputStream());
            }
        }
        throw new WebApplicationException(500);
    }
    
    @POST
    @Path("/books/attachments")
    @ProduceMime("text/xml")
    public Response addBookFromAttachments() throws Exception {
        Collection<Attachment> handlers = AttachmentUtils.getChildAttachments(context);
        for (Attachment a : handlers) {
            if (a.getContentId().equals("book2")) {
                return readBookFromInputStream(a.getDataHandler().getInputStream());
            }
        }
        throw new WebApplicationException(500);
    }
    
    @POST
    @Path("/books/jaxb")
    @ProduceMime("text/xml")
    public Response addBook(Book b) throws Exception {
        b.setId(124);
        return Response.ok(b).build();
    }
    
    @POST
    @Path("/books/mismatch1")
    @ConsumeMime("multipart/related;type=\"bar/foo\"")
    @ProduceMime("text/xml")
    public Response addBookMismatched(Book b) {
        throw new WebApplicationException();
    }
    
    @POST
    @Path("/books/mismatch2")
    @ProduceMime("text/xml")
    public Response addBookMismatched2(@Multipart(value = "rootPart", type = "f/b") Book b) {
        throw new WebApplicationException();
    }
    
    private Response readBookFromInputStream(InputStream is) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Unmarshaller u = c.createUnmarshaller();
        Book b = (Book)u.unmarshal(is);
        b.setId(124);
        return Response.ok(b).build();
    }
    
}


