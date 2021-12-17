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
package org.apache.cxf.jaxrs.model.wadl;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.bind.annotation.XmlTransient;
import org.apache.cxf.aegis.type.java5.IgnoreProperty;
import org.apache.cxf.jaxrs.ext.ResponseStatus;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.ext.xml.ElementClass;
import org.apache.cxf.jaxrs.ext.xml.XMLName;
import org.apache.cxf.jaxrs.model.wadl.jaxb.Book;
import org.apache.cxf.jaxrs.model.wadl.jaxb.Chapter;
import org.apache.cxf.jaxrs.model.wadl.jaxb.packageinfo.Book2;

@Path("/bookstore/{id}")
@Consumes({"application/xml", "application/json" })
@Produces({"application/xml", "application/json" })
public class BookStore extends AbstractStore<Book> implements BookDescription {

    @Descriptions({
        @Description(value = "Attachments, max < 10", target = DocTarget.PARAM)
    })
    @POST
    @Consumes("multipart/form-data")
    public void formdata(MultipartBody body) {

    }

    @GET
    @Produces("application/xml")
    @XMLName("{http://superbooks}books")
    @Descriptions({
        @Description(value = "Get Books", target = DocTarget.METHOD)
    })
    public List<Book> getBooks(@PathParam("id") Long id, @BeanParam TheBeanParam beanParam) {
        return Collections.emptyList();
    }

    @GET
    @Path("thebooks2")
    @Produces("application/xml")
    @Descriptions({
        @Description(value = "Get Books2", target = DocTarget.METHOD)
    })
    public List<Book2> getBooks2(@PathParam("id") Long id) {
        return Collections.emptyList();
    }

    @GET
    @Produces("text/plain")
    public String getName(@PathParam("id") Long id, @BeanParam TheBeanParam beanParam,
                          @QueryParam("") QueryBean query) {
        return "store";
    }

    @PUT
    @Consumes("text/plain")
    public void setName(@PathParam("id") Long id, String name) {
    }

    @Path("books/\"{bookid}\"")
    public Object addBook(@PathParam("id") int id,
                        @PathParam("bookid") int bookId,
                        @MatrixParam("mid") int matrixId) {
        return new Book(1);
    }


    @Descriptions({
        @Description(value = "Update the books collection", target = DocTarget.METHOD),
        @Description(value = "Requested Book", target = DocTarget.RETURN),
        @Description(value = "Request", target = DocTarget.REQUEST),
        @Description(value = "Response", target = DocTarget.RESPONSE),
        @Description(value = "Resource books/{bookid}", target = DocTarget.RESOURCE)
    })
    @ResponseStatus({Status.CREATED, Status.OK })
    //CHECKSTYLE:OFF
    @POST
    @Path("books/{bookid}")
    public Book addBook(@Description("book id") //NOPMD
                        @PathParam("id") int id,
                        @PathParam("bookid") int bookId,
                        @MatrixParam("mid") @DefaultValue("mid > 5") String matrixId,
                        @Description("header param")
                        @HeaderParam("hid") int headerId,
                        @CookieParam("cid") int cookieId,
                        @QueryParam("provider.bar") int queryParam,
                        @QueryParam("bookstate") BookEnum state,
                        @QueryParam("orderstatus") BookOrderEnum status,
                        @QueryParam("a") List<String> queryList,
                        @Context HttpHeaders headers,
                        @Description("InputBook")
                        @XMLName(value = "{http://books}thesuperbook2")
                        Book2 b) {
        return new Book(1);
    }

    @PUT
    @Path("books/{bookid}")
    @Description("Update the book")
    public void addBook(@PathParam("id") int id,
                        @PathParam("bookid") int bookId,
                        @MatrixParam("mid") int matrixId,
                        Book b) {
    }

    //CHECKSTYLE:ON
    @Path("booksubresource")
    public Book getBook(@PathParam("id") int id,
                        @MatrixParam("mid") int matrixId) {
        return new Book(1);
    }

    @GET
    @Path("chapter")
    public Chapter getChapter() {
        return new Chapter(1);
    }

    @GET
    @Path("chapter2")
    @ElementClass(response = Chapter.class)
    public void getChapterAsync(@Suspended AsyncResponse async) {
        async.resume(Response.ok().entity(new Chapter(1)).build());
    }

    @Path("form")
    public FormInterface getForm() {
        return new Book(1);
    }

    @Path("itself")
    public BookStore getItself() {
        return this;
    }

    @Path("book2")
    @GET
    @XMLName(value = "{http://books}thesuperbook2", prefix = "p1")
    public Book2 getBook2() {
        return new Book2();
    }

    public static class TheBeanParam {
        private int a;
        @QueryParam("b")
        private int b;
        public int getA() {
            return a;
        }
        @PathParam("a")
        public void setA(int aa) {
            this.a = aa;
        }
        public int getB() {
            return b;
        }
        public void setB(int bb) {
            this.b = bb;
        }
    }

    public static class QueryBean {
        private int a;
        private int b;
        private QueryBean2 bean;

        public int getAProp() {
            return a;
        }

        @IgnoreProperty
        public int getB() {
            return b;
        }

        public QueryBean2 getC() {
            return bean;
        }

        public TestEnum getE() {
            return TestEnum.A;
        }

    }

    public static class QueryBean2 {
        private int a;
        private int b;
        private QueryBean3 bean;

        public int getA() {
            return a;
        }

        public int getB() {
            return b;
        }

        public QueryBean3 getD() {
            return bean;
        }

        public QueryBean3 getD2() {
            return bean;
        }

        public QueryBean2 getIt() {
            return this;
        }
    }

    public static class QueryBean3 {
        private boolean a;
        private int b;

        public boolean isA() {
            return a;
        }

        @XmlTransient
        public int getB() {
            return b;
        }
    }

    public enum TestEnum {
        A;
    }
}
