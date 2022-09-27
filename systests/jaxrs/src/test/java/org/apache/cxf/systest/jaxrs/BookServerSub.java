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

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;


public class BookServerSub extends AbstractServerTestServerBase {
    public static final String PORT = allocatePort(BookServerSub.class);

    @Provider
    public static class ExceptionMatcher implements ExceptionMapper<WebApplicationException> {
        @Override
        public Response toResponse(WebApplicationException exception) {
            Response response = exception.getResponse();
            int status = response == null ? Status.INTERNAL_SERVER_ERROR.getStatusCode() : response.getStatus();
            if (response != null && response.getEntity() != null) {
                return response;
            }
        
            switch (status) {
            case 404:
            case 405:
            case 406:
            case 415:
                return Response.status(status).entity(String.valueOf(status)).build();
            default:
                return response;
            }
        }
    }
    
    @Override
    protected Server createServer(Bus bus) throws Exception {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setStaticSubresourceResolution(true);
        sf.setProvider(new ExceptionMatcher());
        sf.setResourceClasses(BookStoreSubObject.class);
        sf.setResourceProvider(BookStoreSubObject.class,
                               new SingletonResourceProvider(new BookStoreSubObject(), true));
        sf.setAddress("http://localhost:" + PORT + "/");
        return sf.create();
    }

    public static void main(String[] args) throws Exception {
        new BookServerSub().start();
    }

}
