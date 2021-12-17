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

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ValidatorTest {

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

    @Path("/rest")
    public interface ExtraParamTemplate {
        @GET
        Response get(@PathParam("any") String any);
    }

    public interface ClientHeaderParamNoName {
        @ClientHeaderParam(name = "", value = "something")
        @GET
        Response get();
    }

    public interface ClientHeaderParamNoComputeMethod {
        @ClientHeaderParam(name = "SomeHeader", value = "{missingComputeMethod}")
        @GET
        Response get();
    }

    public interface ClientHeaderParamNonDefaultComputeMethod {
        @ClientHeaderParam(name = "SomeHeader", value = "{nonDefaultComputeMethod}")
        @GET
        Response get();

        String nonDefaultComputeMethod();
    }

    public interface ClientHeaderParamComputeMethodDoesNotExist {
        @ClientHeaderParam(name = "SomeHeader", value = "{nonExistentComputeMethod}")
        @GET
        Response get();
    }

    public interface ClientHeaderParamInaccessibleComputeMethod {
        @ClientHeaderParam(name = "SomeHeader",
            value = "{org.apache.cxf.microprofile.client.mock.HeaderGenerator.generateHeaderPrivate}")
        @GET
        Response get();
    }

    public interface ClientHeaderParamNoValidComputeMethodSignatures {
        @ClientHeaderParam(name = "SomeHeader", value = "{computeMethod}")
        @GET
        Response get();

        default String computeMethod(String x, String y) {
            return "must only contain one String argument";
        }
        default String computeMethod(ClientRequestContext x, ClientRequestContext y) {
            return "must only contain one ClientRequestContext argument";
        }
        default Integer computeMethod() {
            return 5; // must return a String
        }
        default void computeMethod(String headerName) { } // must return a String
        default String computeMethod(java.util.Date date) {
            return "unexpected argument";
        }
        default String computeMethod(String headerName, ClientRequestContext context, int extra) {
            return "too many arguments";
        }
    }

    public interface PathRegexTestClient {

        // Only books with id consisting of 3 or 4 digits of the numbers between 5 and 9 are accepted
        @POST
        @Path("/echoxmlbookregex/{id : [5-9]{3,4}}")
        void testRegex(@PathParam("id") String id);
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
        test(MultiVerbMethod.class, "more than one HTTP method", "postAndDelete", "jakarta.ws.rs.POST",
            "jakarta.ws.rs.DELETE");
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

    @Test
    public void testMissingTemplate() {
        test(ExtraParamTemplate.class, "extra path segments", "ExtraParamTemplate");
    }

    @Test
    public void testClientHeaderParamNoName() {
        test(ClientHeaderParamNoName.class, ClientHeaderParamNoName.class.getName(), "null or empty name");
    }

    @Test
    public void testClientHeaderParamNoComputeMethod() {
        test(ClientHeaderParamNoComputeMethod.class, ClientHeaderParamNoComputeMethod.class.getName(),
             "value attribute specifies a method", "that does not exist");
    }

    @Test
    public void testClientHeaderParamNonDefaultComputeMethod() {
        test(ClientHeaderParamNonDefaultComputeMethod.class,
             ClientHeaderParamNonDefaultComputeMethod.class.getName(),
             " is not accessible");
    }

    @Test
    public void testClientHeaderParamComputeMethodDoesNotExist() {
        test(ClientHeaderParamNonDefaultComputeMethod.class,
             ClientHeaderParamNonDefaultComputeMethod.class.getName(),
             " does not exist");
    }

    @Test
    public void testClientHeaderParamNoValidComputeMethodSignatures() {
        test(ClientHeaderParamNoValidComputeMethodSignatures.class,
             ClientHeaderParamNoValidComputeMethodSignatures.class.getName(),
             " contains an incorrect signature");
    }

    @Test
    public void testPathRegularExpression() {
        assertNotNull(newBuilder().build(PathRegexTestClient.class));
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