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
package org.apache.cxf.osgi.itests.jaxrs;

import java.io.InputStream;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.osgi.itests.AbstractServerActivator;
import org.apache.cxf.osgi.itests.CXFOSGiTestSupport;
import org.osgi.framework.Constants;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.tinybundles.core.TinyBundles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JaxRsServiceTest extends CXFOSGiTestSupport {

    private static final String BASE_URL = "http://localhost:8181/cxf/jaxrs/bookstore";

    private final WebTarget wt = ClientBuilder.newClient().target(BASE_URL);

    @Test
    public void testJaxRsGet() throws Exception {
        Book book = wt.path("/books/123").request("application/xml").get(Book.class);
        assertNotNull(book);
    }

    @Test
    public void testJaxRsPost() throws Exception {
        Book book = new Book("New Book", 321);
        Response response = wt.path("/books/").request("application/xml").post(Entity.xml(book));
        assertStatus(Status.CREATED, response);
        assertNotNull(response.getLocation());
    }
    
    @Test
    public void postWithValidationError() throws Exception {
        Book book = new Book(null, -1);
        Response response = wt.path("/books-validate/").request("application/xml").post(Entity.xml(book));
        assertStatus(Status.BAD_REQUEST, response);
    }

    @Test
    public void postWithValidation() throws Exception {
        Book book = new Book("A Book", 3212);
        Response response = wt.path("/books-validate/").request("application/xml").post(Entity.xml(book));
        assertStatus(Status.CREATED, response);
        assertNotNull(response.getLocation());
    }

    @Test
    public void testJaxRsDelete() throws Exception {
        Response response = wt.path("/books/123").request("application/xml").delete();
        assertStatus(Status.OK, response);
    }

    @Test
    public void testJaxRsPut() throws Exception {
        Book book = new Book("Updated Book", 123);
        Response response = wt.path("/books/123").request("application/xml").put(Entity.xml(book));
        assertStatus(Status.OK, response);
    }

    private static void assertStatus(Status expectedStatus, Response response) {
        assertEquals(expectedStatus.getStatusCode(), response.getStatus());
    }

    @Configuration
    public Option[] config() {
        return OptionUtils.combine(
            cxfBaseConfig(),
            features(cxfUrl, "cxf-core", "cxf-wsdl", "cxf-jaxrs", "cxf-bean-validation-core", "cxf-bean-validation"),
            logLevel(LogLevel.INFO),
            provision(serviceBundle())
        );
    }

    private static InputStream serviceBundle() {
        if (JavaUtils.isJava11Compatible()) {
            return TinyBundles.bundle()
                  .add(AbstractServerActivator.class)
                  .add(JaxRsTestActivator.class)
                  .add(Book.class)
                  .add(BookStore.class)
                  .set(Constants.BUNDLE_ACTIVATOR, JaxRsTestActivator.class.getName())
                  .set("Require-Capability", "osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=11))\"")
                  .build(TinyBundles.withBnd());
        } else {
            return TinyBundles.bundle()
                .add(AbstractServerActivator.class)
                .add(JaxRsTestActivator.class)
                .add(Book.class)
                .add(BookStore.class)
                .set(Constants.BUNDLE_ACTIVATOR, JaxRsTestActivator.class.getName())
                .build(TinyBundles.withBnd());
        }
    }

}
