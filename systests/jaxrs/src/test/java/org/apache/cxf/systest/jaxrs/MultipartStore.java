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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.xml.transform.stream.StreamSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
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

    @GET
    @Path("/content/string")
    @Produces("multipart/mixed")
    public Attachment getAttachmentWithStringContent() throws Exception {
        return new Attachment("Response_XML_Payload", "application/xml", "<Book><id>888</id></Book>");
    }

    @GET
    @Path("/content/bytes")
    @Produces("multipart/mixed")
    public Attachment getAttachmentWithByteContent() throws Exception {
        return new Attachment("Response_XML_Payload", "application/xml", "<Book><id>888</id></Book>".getBytes());
    }


    @POST
    @Path("/books/image")
    @Consumes("multipart/mixed")
    @Produces("multipart/mixed")
    @Multipart(type = "application/stream")
    public byte[] addBookImage(@Multipart byte[] image) throws Exception {
        return image;
    }

    @POST
    @Path("/xop")
    @Consumes("multipart/related")
    @Produces("multipart/related;type=text/xml")
    @Multipart("xop")
    public XopType addBookXop(@Multipart XopType type) throws Exception {
        if (!"xopName".equals(type.getName())) {
            throw new RuntimeException("Wrong name property");
        }
        String bookXsd = IOUtils.readStringFromStream(type.getAttachinfo().getInputStream());
        String bookXsd2 = IOUtils.readStringFromStream(
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/book.xsd"));
        if (!bookXsd.equals(bookXsd2)) {
            throw new RuntimeException("Wrong attachinfo property");
        }
        String bookXsdRef = IOUtils.readStringFromStream(type.getAttachInfoRef().getInputStream());
        if (!bookXsdRef.equals(bookXsd2)) {
            throw new RuntimeException("Wrong attachinforef property");
        }
        if (!Boolean.getBoolean("java.awt.headless") && type.getImage() == null) {
            throw new RuntimeException("Wrong image property");
        }
        context.put(org.apache.cxf.message.Message.MTOM_ENABLED,
                    "true");

        XopType xop = new XopType();
        xop.setName("xopName");
        InputStream is =
            getClass().getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/book.xsd");
        byte[] data = IOUtils.readBytesFromStream(is);
        xop.setAttachinfo(new DataHandler(new ByteArrayDataSource(data, "application/octet-stream")));
        xop.setAttachInfoRef(new DataHandler(new ByteArrayDataSource(data, "application/octet-stream")));
        xop.setAttachinfo2(bookXsd.getBytes());

        xop.setImage(ImageIO.read(getClass().getResource(
                "/org/apache/cxf/systest/jaxrs/resources/java.jpg")));
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

    @Path("/books/file/semicolon")
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    @POST
    public String addBookFileNameSemicolon(@Multipart("a") Attachment att) {
        return att.getObject(String.class)
            + ", filename:" + att.getContentDisposition().getParameter("filename");
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
        List<Attachment> newAtts = new ArrayList<>();
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
    @Path("/books/testnullpart")
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    public String testNullPart(@Multipart(value = "someid", required = false) String value) {
        if (value != null) {
            return value;
        }
        return "nobody home";
    }
    @POST
    @Path("/books/testnullparts")
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    public String testNullParts(@Multipart(value = "someid") String value,
                                @Multipart(value = "someid2", required = false) String value2) {
        if (value2 != null) {
            return value + value2;
        }
        return "nobody home2";
    }

    @POST
    @Path("/books/testnullpartprimitive")
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    public int testNullPart2(@Multipart(value = "someid", required = false) int value) {
        return value;
    }

    @POST
    @Path("/books/testnullpartFormParam")
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    public String testNullPartFormParam(@FormParam(value = "someid") String value) {
        return testNullPart(value);
    }


    @POST
    @Path("/books/jaxbjsonimage")
    @Consumes({"multipart/mixed", "multipart/related" })
    @Produces({"multipart/mixed", "multipart/related" })
    public Map<String, Object> addBookJaxbJsonImage(@Multipart("root.message@cxf.apache.org") Book jaxb,
                                                    @Multipart("1") Book json,
                                                    @Multipart("2") byte[] image) throws Exception {
        Map<String, Object> objects = new LinkedHashMap<>();
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
        Map<String, Object> objects = new LinkedHashMap<>();
        objects.put("application/xml", jaxb);
        objects.put("application/json", json);
        objects.put("application/octet-stream", new ByteArrayInputStream(image));
        return objects;

    }

    @POST
    @Path("/books/jsonimagestream")
    @Consumes("multipart/mixed")
    @Produces("multipart/mixed")
    public Map<String, Object> addBookJsonImageStream(
        @Multipart(value = "thejson", type = "application/json") Book json,
        @Multipart("theimage") InputStream image) throws Exception {
        Map<String, Object> objects = new LinkedHashMap<>();
        objects.put("application/json", json);
        objects.put("application/octet-stream", image);
        return objects;

    }

    @GET
    @Path("/books/jaxbjsonimage/read")
    @Produces("multipart/mixed")
    public Map<String, Book> getBookJaxbJson() throws Exception {
        Book jaxb = new Book("jaxb", 1L);
        Book json = new Book("json", 2L);
        Map<String, Book> objects = new LinkedHashMap<>();
        objects.put(MediaType.APPLICATION_XML, jaxb);
        objects.put(MediaType.APPLICATION_JSON, json);
        return objects;

    }

    @GET
    @Path("/books/jaxbjsonimage/read2")
    @Produces("multipart/mixed")
    public Map<String, Book> getBookJson() throws Exception {
        Book json = new Book("json", 1L);
        Map<String, Book> objects = new LinkedHashMap<>();
        objects.put(MediaType.APPLICATION_JSON, json);
        return objects;

    }

    @GET
    @Path("/books/jaxbjsonimage/read-object")
    @Produces("multipart/mixed")
    public Map<String, Object> getBookJaxbJsonObject() throws Exception {
        Book jaxb = new Book("jaxb", 1L);
        Book json = new Book("json", 2L);
        Map<String, Object> objects = new LinkedHashMap<>();
        objects.put(MediaType.APPLICATION_XML, jaxb);
        objects.put(MediaType.APPLICATION_JSON, json);
        return objects;

    }

    @POST
    @Path("/books/stream")
    @Produces("text/xml")
    public Response addBookFromStream(@Multipart StreamSource source) throws Exception {
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
    public Response addBookFromInputStream(@Multipart("rootPart") InputStream is) throws Exception {
        return readBookFromInputStream(is);
    }

    @POST
    @Path("/books/istream2")
    @Produces("text/xml")
    public Book addBookFromInputStreamReadItself(InputStream is) throws Exception {

        String body = IOUtils.readStringFromStream(is);
        if (!body.trim().startsWith("--")) {
            throw new RuntimeException();
        }

        return new Book("432", 432L);
    }

    @POST
    @Path("/books/dsource")
    @Produces("text/xml")
    public Response addBookFromDataSource(@Multipart DataSource ds) throws Exception {
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
    @Path("/books/details")
    @Consumes("multipart/form-data")
    @Produces("text/xml")
    public Response addBookWithDetails(@Multipart(value = "book", type = "application/xml") Book book,
            @Multipart("upfile1Detail") Attachment a1,
            @Multipart("upfile2Detail") Attachment a2,
            @Multipart("upfile3Detail") Attachment a3)
        throws Exception {

        if (a1.equals(a2) || a1.equals(a3) || a2.equals(a3)) {
            throw new WebApplicationException();
        }

        book.setName(a1.getContentId() + "," + a2.getContentId() + "," + a3.getContentId());
        return Response.ok(book).build();
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
        List<Book> books2 = new ArrayList<>();
        books2.addAll(books);
        return books2;
    }

    @POST
    @Path("/books/jaxbonly")
    @Consumes("multipart/mixed;type=text/xml")
    @Produces("multipart/mixed;type=text/xml")
    public List<Book> addBooksWithoutHeader(List<Book> books) {
        return addBooks(books);
    }

    @POST
    @Path("/books/jaxbjsonconsumes")
    @Consumes("multipart/related")
    @Produces("text/xml")
    public Book addBookJaxbJsonWithConsumes(
        @Multipart(value = "rootPart", type = "text/xml") Book2 b1,
        @Multipart(value = "book2", type = "application/json") Book b2) throws Exception {
        return addBookJaxbJson(b1, b2);
    }

    @POST
    @Path("/books/audiofiles")
    @Consumes("multipart/related")
    @Produces("text/xml")
    public Book addAudioBook(
            @Multipart(value = "book", type = "application/json") Book book,
            @Multipart(value = "audio") Attachment audioFile) throws Exception {
        String payload = String.valueOf(audioFile.getDataHandler().getContent().toString().getBytes()[0]);
        return new Book(book.getName() + " - " + payload, book.getId());
    }

    @POST
    @Path("/books/jaxbandsimpleparts")
    @Consumes("multipart/related")
    @Produces("text/xml")
    public Book testAddBookAndSimpleParts(
        @Multipart(value = "rootPart", type = "text/xml") Book b1,
        @Multipart(value = "simplePart1") String simplePart1,
        @Multipart(value = "simplePart2") Integer simplePart2) throws Exception {
        return new Book(b1.getName() + " - " + simplePart1 + simplePart2.toString(), b1.getId());
    }

    @POST
    @Path("/books/jaxbonly")
    @Consumes("multipart/related")
    @Produces("text/xml")
    public Book2 addBookJaxbOnlyWithConsumes(
        @Multipart(value = "rootPart", type = "text/xml") Book2 b1) throws Exception {
        if (!"CXF in Action".equals(b1.getName())) {
            throw new WebApplicationException();
        }
        return b1;
    }

    @POST
    @Path("/books/jaxbjson")
    @Produces("text/xml")
    public Book addBookJaxbJson(
        @Multipart(value = "rootPart", type = "text/xml") Book2 b1,
        @Multipart(value = "book2", type = "application/json") Book b2)
        throws Exception {
        if (!"CXF in Action".equals(b1.getName())
            || !"CXF in Action - 2".equals(b2.getName())) {
            throw new WebApplicationException();
        }
        b2.setId(124);
        return b2;
    }

    @POST
    @Path("/books/jsonform")
    @Produces("text/xml")
    @Consumes("multipart/form-data")
    public Response addBookJsonFromForm(@Multipart Book b1)
        throws Exception {
        b1.setId(124);
        return Response.ok(b1).build();
    }
    
    @POST
    @Path("/books/jsonstream")
    @Produces("text/xml")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public List<Book> addBookJsonTypeFromStreams(
            @Multipart(value = "part1", type = "application/octet-stream") InputStream in1,
            @Multipart(value = "part2", type = "application/octet-stream") InputStream in2) throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final Book[] array1 = mapper.readValue(in1, Book[].class);
        final Book[] array2 = mapper.readValue(in2, Book[].class);
        return Stream.concat(Arrays.stream(array1), Arrays.stream(array2)).collect(Collectors.toList());
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
            || !"CXF in Action - 2%".equals(b2.getName())
            || !"Larry".equals(name)) {
            throw new WebApplicationException();
        }
        b1.setId(124);
        b1.setName("CXF in Action - 2");
        return Response.ok(b1).build();
    }

    @POST
    @Path("/books/filesform2")
    @Produces("text/xml")
    @Consumes("multipart/form-data")
    public Response addBookFilesFormNoOwnerParam(@Multipart("files") List<Book> books)
        throws Exception {
        Attachment attOwner = AttachmentUtils.getFirstMatchingPart(context, "owner");
        return addBookFilesForm(attOwner.getObject(String.class), books);
    }

    @POST
    @Path("/books/filesform/singlefile")
    @Produces("text/xml")
    @Consumes("multipart/form-data")
    public Response addBookFilesFormSingleFile(@Multipart("owner") String name,
                                     @Multipart("file") List<Book> books)
        throws Exception {
        if (books.size() != 1) {
            throw new WebApplicationException();
        }
        Book b = books.get(0);
        if (!"CXF in Action - 1".equals(b.getName())
            || !"Larry".equals(name)) {
            throw new WebApplicationException();
        }
        b.setId(124);
        b.setName("CXF in Action - 2");
        return Response.ok(b).build();
    }

    @POST
    @Path("/books/filesform/mixup")
    @Produces("text/xml")
    @Consumes("multipart/form-data")
    public Response addBookFilesFormMixUp(@FormParam("owner") String name,
                                          @Multipart("files") List<Book> books)
        throws Exception {
        return addBookFilesForm(name, books);
    }

    @POST
    @Path("/books/fileform")
    @Produces("text/xml")
    @Consumes("multipart/form-data")
    public Response addBookFilesForm(MultipartBody body)
        throws Exception {
        String owner = body.getAttachmentObject("owner", String.class);
        Book book = body.getAttachmentObject("file", Book.class);
        if (!"CXF in Action - 1".equals(book.getName())
            || !"Larry".equals(owner)) {
            throw new WebApplicationException();
        }
        book.setId(124);
        book.setName("CXF in Action - 2");
        return Response.ok(book).build();
    }

    @POST
    @Path("/books/jaxbform")
    @Produces("text/xml")
    @Consumes("multipart/form-data")
    public Response addBookJaxbFromForm(@Multipart Book b1)
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
    public Response addBookFromDataHandler(@Multipart DataHandler dh) throws Exception {
        return readBookFromInputStream(dh.getInputStream());
    }

    @POST
    @Path("/books/attachment")
    @Produces("text/xml")
    public Response addBookFromAttachment(@Multipart Attachment a) throws Exception {
        return readBookFromInputStream(a.getDataHandler().getInputStream());
    }

    @POST
    @Path("/books/mchandlers")
    @Produces("text/xml")
    public Response addBookFromMessageContext() throws Exception {
        Map<String, Attachment> handlers = AttachmentUtils.getAttachmentsMap(context);
        for (Map.Entry<String, Attachment> entry : handlers.entrySet()) {
            if ("book2".equals(entry.getKey())) {
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
            if ("book2".equals(a.getContentId())) {
                return readBookFromInputStream(a.getDataHandler().getInputStream());
            }
        }
        throw new WebApplicationException(500);
    }

    @POST
    @Path("/books/jaxb")
    @Produces("text/xml")
    public Response addBook(@Multipart Book b) throws Exception {
        b.setId(124);
        return Response.ok(b).build();
    }

    @POST
    @Path("/books/mismatch1")
    @Consumes("multipart/related;type=\"bar/foo\"")
    @Produces("text/xml")
    public Response addBookMismatched(@Multipart Book b) {
        throw new WebApplicationException();
    }

    @POST
    @Path("/books/mismatch2")
    @Produces("text/xml")
    public Response addBookMismatched2(@Multipart(value = "rootPart", type = "f/b") Book b) {
        throw new WebApplicationException();
    }

    @POST
    @Path("/books/mixedmultivaluedmap")
    @Consumes("multipart/mixed")
    @Produces("text/xml")
    public Response addBookFromFormConsumesMixed(
        @Multipart(value = "mapdata", type = MediaType.APPLICATION_FORM_URLENCODED)
        MultivaluedMap<String, String> data,
        @Multipart(value = "test-cid", type = MediaType.APPLICATION_XML)
        String testXml) throws Exception {
        if (!"Dreams".equals(data.get("id-name").get(0))) {
            throw new Exception("Map entry 0 does not match");
        }
        if (!"True".equals(data.get("entity-name").get(0))) {
            throw new Exception("Map entry 1 does not match");
        }
        if (!"NAT5\n".equals(data.get("native-id").get(0))) {
            throw new Exception("Map entry 2 does not match");
        }
        if (data.size() != 3) {
            throw new Exception("Map size does not match");
        }
        if ("<hello>World2</hello>".equals(testXml)) {
            throw new Exception("testXml does not match");
        }

        Book b = new Book();
        b.setId(124);
        b.setName("CXF in Action - 2");
        return Response.ok(b).build();
    }
    
    @PUT
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.MULTIPART_FORM_DATA })
    public Response updateBook(@PathParam("id") long id, @Multipart("name") String name) {
        Book book = new Book(name, id);
        return Response.ok().entity(book).build();
    }

    private Response readBookFromInputStream(InputStream is) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Unmarshaller u = c.createUnmarshaller();
        Book b = (Book)u.unmarshal(is);
        b.setId(124);
        return Response.ok(b).build();
    }

}


