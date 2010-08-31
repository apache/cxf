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


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.imageio.ImageIO;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.helpers.IOUtils;
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
    @Path("/books/image")
    @Consumes("multipart/mixed")
    @Produces("multipart/mixed")
    public byte[] addBookImage(byte[] image) throws Exception {
        return image;
    }
    
    @POST
    @Path("/xop")
    @Consumes("multipart/related")
    @Produces("multipart/related;type=text/xml")
    public XopType addBookXop(XopType type) throws Exception {
        if (!"xopName".equals(type.getName())) {
            throw new RuntimeException("Wrong name property");
        }
        String bookXsd = IOUtils.readStringFromStream(type.getAttachinfo().getInputStream());
        String bookXsd2 = IOUtils.readStringFromStream(
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/book.xsd"));
        if (!bookXsd.equals(bookXsd2)) {
            throw new RuntimeException("Wrong attachinfo property");
        }
        if (!Boolean.getBoolean("java.awt.headless") && type.getImage() == null) {
            throw new RuntimeException("Wrong image property");
        }
        context.put(org.apache.cxf.message.Message.MTOM_ENABLED, 
                    (Object)"true");
        
        XopType xop = new XopType();
        xop.setName("xopName");
        InputStream is = 
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/book.xsd");
        byte[] data = IOUtils.readBytesFromStream(is);
        xop.setAttachinfo(new DataHandler(new ByteArrayDataSource(data, "application/octet-stream")));
        
        xop.setAttachinfo2(bookXsd.getBytes());
     
        if (Boolean.getBoolean("java.awt.headless")) {
            System.out.println("Running headless. Ignoring an Image property.");
        } else {
            xop.setImage(ImageIO.read(getClass().getResource(
                "/org/apache/cxf/systest/jaxrs/resources/java.jpg")));
        }
        return xop;
    }
    
    
    @POST
    @Path("/books/formimage2")
    @Consumes("multipart/form-data")
    @Produces("multipart/form-data")
    public MultipartBody addBookFormImage2(MultipartBody image) throws Exception {
        image.getAllAttachments();
        return image;
    }
    
    @POST
    @Path("/books/formimage")
    @Consumes("multipart/form-data")
    @Produces("multipart/form-data")
    public MultipartBody addBookFormImage(MultipartBody image) throws Exception {
        List<Attachment> atts = image.getAllAttachments();
        if (atts.size() != 1) {
            throw new WebApplicationException();
        }
        List<Attachment> newAtts = new ArrayList<Attachment>();
        Attachment at = atts.get(0);
        MultivaluedMap<String, String> headers = at.getHeaders();
        if (!"http://host/bar".equals(headers.getFirst("Content-Location"))) {
            throw new WebApplicationException();
        }
        if (!"custom".equals(headers.getFirst("Custom-Header"))) {
            throw new WebApplicationException();
        }
        headers.putSingle("Content-Location", "http://host/location");
        newAtts.add(new Attachment(at.getContentId(), at.getDataHandler(), headers));
        
        return new MultipartBody(newAtts);
    }
    
    @POST
    @Path("/books/jaxbjsonimage")
    @Consumes("multipart/mixed")
    @Produces("multipart/mixed")
    public Map<String, Object> addBookJaxbJsonImage(@Multipart("root.message@cxf.apache.org") Book jaxb, 
                                                    @Multipart("1") Book json, 
                                                    @Multipart("2") byte[] image) throws Exception {
        Map<String, Object> objects = new LinkedHashMap<String, Object>();
        objects.put("application/xml", jaxb);
        objects.put("application/json", json);
        objects.put("application/octet-stream", new ByteArrayInputStream(image));
        return objects;
        
    }
    
    @POST
    @Path("/books/jaxbimagejson")
    @Consumes("multipart/mixed")
    @Produces("multipart/mixed")
    public Map<String, Object> addBookJaxbJsonImage2(@Multipart("theroot") Book jaxb, 
                                                     @Multipart("thejson") Book json, 
                                                     @Multipart("theimage") byte[] image) throws Exception {
        Map<String, Object> objects = new LinkedHashMap<String, Object>();
        objects.put("application/xml", jaxb);
        objects.put("application/json", json);
        objects.put("application/octet-stream", new ByteArrayInputStream(image));
        return objects;
        
    }
    
    @POST
    @Path("/books/stream")
    @Produces("text/xml")
    public Response addBookFromStream(StreamSource source) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Unmarshaller u = c.createUnmarshaller();
        Book b = (Book)u.unmarshal(source);
        b.setId(124);
        return Response.ok(b).build();
    }
    
    @POST
    @Path("/books/form")
    @Consumes("multipart/form-data")
    @Produces("text/xml")
    public Response addBookFromForm(MultivaluedMap<String, String> data) throws Exception {
        Book b = new Book();
        b.setId(Long.valueOf(data.getFirst("id")));
        b.setName(data.getFirst("name"));
        return Response.ok(b).build();
    }
    
    @POST
    @Path("/books/formbody")
    @Consumes("multipart/form-data")
    @Produces("text/xml")
    public Response addBookFromFormBody(MultipartBody body) throws Exception {
        MultivaluedMap<String, String> data = AttachmentUtils.populateFormMap(context);
        Book b = new Book();
        b.setId(Long.valueOf(data.getFirst("id")));
        b.setName(data.getFirst("name"));
        return Response.ok(b).build();
    }
    
    @POST
    @Path("/books/formbody2")
    @Consumes("multipart/form-data")
    @Produces("text/xml")
    public Response addBookFromFormBody2() throws Exception {
        return addBookFromFormBody(AttachmentUtils.getMultipartBody(context));
    }
    
    
    @POST
    @Path("/books/formparam")
    @Produces("text/xml")
    public Response addBookFromFormParam(@FormParam("name") String title, 
                                         @FormParam("id") Long id) throws Exception {
        if (!"CXF in Action - 2".equals(title)) {
            throw new RuntimeException();
        }
        Book b = new Book();
        b.setId(id);
        b.setName(title);
        return Response.ok(b).build();
    }
    
    @POST
    @Path("/books/formparambean")
    @Produces("text/xml")
    public Response addBookFromFormBean(@FormParam("") Book b) throws Exception {
        return Response.ok(b).build();
    }
    
    
    @POST
    @Path("/books/istream")
    @Produces("text/xml")
    public Response addBookFromInputStream(InputStream is) throws Exception {
        return readBookFromInputStream(is);
    }
    
    @POST
    @Path("/books/dsource")
    @Produces("text/xml")
    public Response addBookFromDataSource(DataSource ds) throws Exception {
        return readBookFromInputStream(ds.getInputStream());
    }
    
    @POST
    @Path("/books/jaxb2")
    @Consumes("multipart/related;type=\"text/xml\"")
    @Produces("text/xml")
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
    @Path("/books/jaxb-body")
    @Consumes("multipart/related;type=\"text/xml\"")
    @Produces("text/xml")
    public Response addBookParts2(MultipartBody body) 
        throws Exception {
        Book b1 = body.getAttachmentObject("rootPart", Book.class);
        Book b2 = body.getAttachmentObject("book2", Book.class);
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
    @Path("/books/jaxbonly")
    @Consumes("multipart/mixed")
    @Produces("multipart/mixed;type=text/xml")
    public List<Book> addBooks(List<Book> books) {
        List<Book> books2 = new ArrayList<Book>();
        books2.addAll(books);
        return books2;
    }
    
    @POST
    @Path("/books/jaxbjson")
    @Produces("text/xml")
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
    @Path("/books/jsonform")
    @Produces("text/xml")
    @Consumes("multipart/form-data")
    public Response addBookJsonFromForm(Book b1) 
        throws Exception {
        b1.setId(124);
        return Response.ok(b1).build();
    }
    
    @POST
    @Path("/books/filesform")
    @Produces("text/xml")
    @Consumes("multipart/form-data")
    public Response addBookFilesForm(@Multipart("owner") String name, 
                                     @Multipart("files") List<Book> books) 
        throws Exception {
        if (books.size() != 2) {
            throw new WebApplicationException();
        }
        Book b1 = books.get(0);
        Book b2 = books.get(1);
        if (!"CXF in Action - 1".equals(b1.getName())
            || !"CXF in Action - 2".equals(b2.getName())
            || !"Larry".equals(name)) {
            throw new WebApplicationException();
        }
        b1.setId(124);
        b1.setName("CXF in Action - 2");
        return Response.ok(b1).build();
    }
    
    @POST
    @Path("/books/jaxbform")
    @Produces("text/xml")
    @Consumes("multipart/form-data")
    public Response addBookJaxbFromForm(Book b1) 
        throws Exception {
        b1.setId(124);
        return Response.ok(b1).build();
    }
    
    @POST
    @Path("/books/jsonjaxbform")
    @Produces("text/xml")
    @Consumes("multipart/form-data")
    public Response addBookJaxbJsonForm(@Multipart("jsonPart") Book b1, 
                                        @Multipart("bookXML") Book b2) 
        throws Exception {
        if (!"CXF in Action - 1".equals(b1.getName())
            || !"CXF in Action - 2".equals(b2.getName())) {
            throw new WebApplicationException();
        }
        b2.setId(124);
        return Response.ok(b2).build();
    }
    
    @POST
    @Path("/books/jsonjaxbformencoded")
    @Produces("text/xml")
    @Consumes("multipart/form-data")
    public Response addBookJaxbJsonFormEncoded(@Multipart("jsonPart") Book b1, 
                                        @Multipart("bookXML") Book b2) 
        throws Exception {
        return addBookJaxbJsonForm(b1, b2);
    }
    
    
    @POST
    @Path("/books/dsource2")
    @Produces("text/xml")
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
    @Produces("text/xml")
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
    @Produces("text/xml")
    public Response addBookFromListOfAttachments(MultipartBody body)  
        throws Exception {
        return addBookFromListOfAttachments(body.getAllAttachments());
    }
    
    @POST
    @Path("/books/lististreams")
    @Produces("text/xml")
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
    @Produces("text/xml")
    public Response addBookFromDataHandler(DataHandler dh) throws Exception {
        return readBookFromInputStream(dh.getInputStream());
    }
    
    @POST
    @Path("/books/attachment")
    @Produces("text/xml")
    public Response addBookFromAttachment(Attachment a) throws Exception {
        return readBookFromInputStream(a.getDataHandler().getInputStream());
    }
    
    @POST
    @Path("/books/mchandlers")
    @Produces("text/xml")
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
    @Produces("text/xml")
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
    @Produces("text/xml")
    public Response addBook(Book b) throws Exception {
        b.setId(124);
        return Response.ok(b).build();
    }
    
    @POST
    @Path("/books/mismatch1")
    @Consumes("multipart/related;type=\"bar/foo\"")
    @Produces("text/xml")
    public Response addBookMismatched(Book b) {
        throw new WebApplicationException();
    }
    
    @POST
    @Path("/books/mismatch2")
    @Produces("text/xml")
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


