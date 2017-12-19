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
package org.apache.cxf.microprofile.client;

import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;

import org.junit.Assert;
import org.junit.Test;

public class ValidatorTest extends Assert {

    public abstract static class NotAnInterface {
        @GET
        public abstract Response get();
    }

    public interface MultiVerbMethod {
        @GET
        Response get();
        @PUT
        Response put(String x);
        @POST
        @DELETE
        Response postAndDelete();
    }

    @Path("/rest/{class}")
    public interface UnresolvedClassUriTemplate {
        @GET
        Response myUnresolvedMethod();
    }

    @Path("/rest")
    public interface UnresolvedMethodUriTemplate {
        @Path("/{method}")
        @GET
        Response myOtherUnresolvedMethod();
    }

    @Path("/rest/{class}")
    public interface PartiallyResolvedUriTemplate {
        @GET
        Response get(@PathParam("class")String className);

        @PUT
        @Path("/{method}")
        Response put(@PathParam("method")String methodName);
    }

    @Path("/rest/{class}")
    public interface PartiallyResolvedUriTemplate2 {
        @DELETE
        Response delete(@PathParam("class")String className);

        @POST
        @Path("/{method}")
        Response post(@PathParam("class")String className);
    }

    private static RestClientBuilder newBuilder() {
        RestClientBuilder builder = RestClientBuilder.newBuilder();
        try {
            builder = builder.baseUrl(new URL("http://localhost:8080/test"));
        } catch (MalformedURLException e) {
            fail("MalformedURL - bad testcase");
        }
        return builder;
    }

    @Test
    public void testNotAnInterface() {
        test(NotAnInterface.class, "is not an interface", "NotAnInterface");
    }

    @Test
    public void testMethodWithMultipleVerbs() {
        test(MultiVerbMethod.class, "more than one HTTP method", "postAndDelete", "javax.ws.rs.POST",
            "javax.ws.rs.DELETE");
    }

    @Test
    public void testUnresolvedUriTemplates() {
        test(UnresolvedClassUriTemplate.class, "unresolved path template variables", "UnresolvedClassUriTemplate",
            "myUnresolvedMethod");
        test(UnresolvedMethodUriTemplate.class, "unresolved path template variables", "UnresolvedMethodUriTemplate",
            "myOtherUnresolvedMethod");
        test(PartiallyResolvedUriTemplate.class, "unresolved path template variables", "PartiallyResolvedUriTemplate",
            "put");
        test(PartiallyResolvedUriTemplate2.class, "unresolved path template variables", "PartiallyResolvedUriTemplate2",
            "post");
    }

    private void test(Class<?> clientInterface, String...expectedMessageTexts) {
        try {
            newBuilder().build(clientInterface);
            fail("Expected RestClientDefinitionException");
        } catch (RestClientDefinitionException ex) {
            String msgText = ex.getMessage();
            assertNotNull("No message text in RestClientDefinitionException", msgText);
            for (String expectedMessageText : expectedMessageTexts) {
                assertTrue("Exception text does not contain expected message: " + expectedMessageText, 
                           msgText.contains(expectedMessageText));
            }
        }
    }
}
