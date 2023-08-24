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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.apache.cxf.Bus;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.DefaultMethod;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSClientServerUserResourceDefaultTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);

    public static class Server extends AbstractServerTestServerBase {

        @Override
        protected org.apache.cxf.endpoint.Server createServer(Bus bus) throws Exception {
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setInvoker(new CustomModelInvoker());
            sf.setProvider(new PreMatchContainerRequestFilter());
            sf.setAddress("http://localhost:" + PORT + "/");
            sf.getServiceFactory().setDefaultModelClass(DefaultResource.class);

            UserResource ur = new UserResource();
            ur.setPath("/default");
            UserOperation op = new UserOperation();
            op.setPath("/books/{id}");
            op.setName("getBook");
            op.setVerb("GET");
            Parameter param = new Parameter(ParameterType.PATH, "id");
            param.setJavaType(Long.class);
            op.setParameters(Collections.singletonList(param));

            UserOperation op2 = new UserOperation();
            op2.setPath("echobook");
            op2.setName("echo");
            op2.setVerb("POST");
            op2.setParameters(Collections.singletonList(new Parameter(ParameterType.REQUEST_BODY, null)));

            UserOperation op3 = new UserOperation();
            op3.setPath("echobookdefault");
            op3.setName("echoDefault");
            op3.setVerb("POST");
            Parameter echoDefaultParam = new Parameter(ParameterType.REQUEST_BODY, null);
            echoDefaultParam.setJavaType(SAXSource.class);
            op3.setParameters(Collections.singletonList(echoDefaultParam));

            List<UserOperation> ops = new ArrayList<>();
            ops.add(op);
            ops.add(op2);
            ops.add(op3);

            ur.setOperations(ops);

            sf.setModelBeans(ur);

            return sf.create();
        }

        public static void main(String[] args) throws Exception {
            new Server().start();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(Server.class, true));
        createStaticBus();
    }

    @Test
    public void testGetBookInvokeService() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/default/books/123",
                      "application/xml", 200, 123);
    }

    @Test
    public void testGetBookInvokerOnly() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/default/books/999",
                      "application/xml", 200, 999);
    }

    @Test
    public void testEchoBook() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/default/echobook");
        Book b = wc.type("application/xml").accept("application/xml").post(new Book("echo", 333L), Book.class);
        assertEquals("echo", b.getName());
        assertEquals(333L, b.getId());
    }

    @Test
    public void testEchoBookDefault() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/default/echobookdefault");
        Book b = wc.type("application/xml").accept("application/xml").post(new Book("echo", 444L), Book.class);
        assertEquals("echo", b.getName());
        assertEquals(444L, b.getId());
    }

    private void getAndCompare(String address,
                               String acceptType,
                               int expectedStatus,
                               long expectedId) throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(address);
        get.setHeader("Accept", acceptType);
        try {
            CloseableHttpResponse response = client.execute(get);
            assertEquals(expectedStatus, response.getStatusLine().getStatusCode());
            Book book = readBook(response.getEntity().getContent());
            assertEquals(expectedId, book.getId());
            assertEquals("CXF in Action", book.getName());
        } finally {
            get.releaseConnection();
        }
    }

    private Book readBook(InputStream is) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Unmarshaller u = c.createUnmarshaller();
        return (Book)u.unmarshal(is);
    }

    @Path("/")
    public static class DefaultResource {
        @Context
        private Request request;
        @Context
        private UriInfo ui;
        @Context
        private HttpHeaders headers;
        private Map<String, Book> books = Collections.singletonMap("123", new Book("CXF in Action", 123L));
        @Path("{a:.*}")
        @DefaultMethod
        public Response handle() {
            if (HttpMethod.GET.equals(request.getMethod())) {
                String id = ui.getPathParameters().getFirst("id");
                Book book = books.get(id);
                return Response.ok(book, headers.getAcceptableMediaTypes().get(0)).build();
            }
            throw new NotAllowedException("GET");
        }
        public Book echo(Book book) {
            return book;
        }
    }

    public static class CustomModelInvoker extends JAXRSInvoker {

        @Override
        public Object invoke(Exchange exchange, Object request, Object resourceObject) {
            MessageContext mc = new MessageContextImpl(exchange.getInMessage());
            List<Object> params = CastUtils.cast((List<?>)request);
            String path = mc.getUriInfo().getPath();
            if ("default/books/999".equals(path)) {
                Long bookId = (Long)params.get(0);
                Book book = new Book("CXF in Action", bookId);
                Response r = Response.ok(book,
                                         mc.getHttpHeaders().getAcceptableMediaTypes().get(0)).build();
                return new MessageContentsList(r);
            } else if ("default/echobookdefault".equals(path)) {
                Source source = (Source)params.get(0);
                Response r = Response.ok(source, MediaType.APPLICATION_ATOM_XML_TYPE).build();
                return new MessageContentsList(r);
            } else {
                return super.invoke(exchange, request, resourceObject);
            }

        }
    }
    @PreMatching
    private static final class PreMatchContainerRequestFilter implements ContainerRequestFilter {
        public void filter(ContainerRequestContext context) throws IOException {
            String path = context.getUriInfo().getPath();
            if (path.endsWith("123")) {
                // Setting this property makes sense only if we have a user model,
                // default service class, a number of method parameters described
                // in the model not matching a number of the matched method's parameters,
                // and this method is actually expected to be invoked (testGetBookInvokeService)
                // which is rare for a model-only case, typically a custom invoker would manage
                // the actual processing of the request
                JAXRSUtils.getCurrentMessage().put("org.apache.cxf.preferMethodParameters", true);
            }
        }

    }
}
