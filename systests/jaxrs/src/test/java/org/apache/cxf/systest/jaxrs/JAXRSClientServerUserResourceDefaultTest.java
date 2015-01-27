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

import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
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
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSClientServerUserResourceDefaultTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);

    @Ignore
    public static class Server extends AbstractBusTestServerBase {        
        org.apache.cxf.endpoint.Server server;
        protected void run() {
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
            op.setParameters(Collections.singletonList(new Parameter(ParameterType.PATH, "id")));
            
            List<UserOperation> ops = new ArrayList<UserOperation>();
            ops.add(op);
            
            ur.setOperations(ops);
            
            sf.setModelBeans(ur);
            
            server = sf.create();
        }

        @Override
        public void tearDown() {
            server.stop();
            server.destroy();
            server = null;
        }
        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
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
    
    private void getAndCompare(String address, 
                               String acceptType,
                               int expectedStatus,
                               long expectedId) throws Exception {
        GetMethod get = new GetMethod(address);
        get.setRequestHeader("Accept", acceptType);
        HttpClient httpClient = new HttpClient();
        try {
            int result = httpClient.executeMethod(get);
            assertEquals(expectedStatus, result);
            Book book = readBook(get.getResponseBodyAsStream());
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
        public Response handle() {
            if (HttpMethod.GET.equals(request.getMethod())) {
                String id = ui.getPathParameters().getFirst("id");
                Book book = books.get(id);
                return Response.ok(book, headers.getAcceptableMediaTypes().get(0)).build();
            } else {
                throw new NotAllowedException("GET");
            }
        }
    }
    
    public static class CustomModelInvoker extends JAXRSInvoker {

        @Override
        public Object invoke(Exchange exchange, Object request, Object resourceObject) {
            MessageContext mc = new MessageContextImpl(exchange.getInMessage());
            List<Object> params = CastUtils.cast((List<?>)request);
            if (params.size() > 0) {
                Long bookId = Long.valueOf(params.get(0).toString());
                Book book = new Book("CXF in Action", bookId);
                Response r = Response.ok(book, 
                                         mc.getHttpHeaders().getAcceptableMediaTypes().get(0)).build();
                return new MessageContentsList(r);
            } else { 
                return super.invoke(exchange, request, resourceObject);
            }
            
        }
    }
    @PreMatching
    private static class PreMatchContainerRequestFilter implements ContainerRequestFilter {
        public void filter(ContainerRequestContext context) throws IOException {
            if (context.getUriInfo().getRequestUri().toString().endsWith("999")) {
                JAXRSUtils.getCurrentMessage().put("org.apache.cxf.preferModelParameters", true);
            }
        }
        
    }
}
