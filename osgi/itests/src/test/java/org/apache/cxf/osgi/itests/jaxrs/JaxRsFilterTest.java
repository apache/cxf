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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response.Status;
import org.apache.cxf.osgi.itests.CXFOSGiTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JaxRsFilterTest extends CXFOSGiTestSupport {

    private static final String BASE_URL_1 = "http://localhost:8181/cxf/firstBundle/bookstore";
    private static final String BASE_URL_2 = "http://localhost:8181/cxf/secondBundle/bookstore";

    private final WebTarget wt1;
    private final WebTarget wt2;

    public JaxRsFilterTest() {
        Client client = ClientBuilder.newClient();
        wt1 = client.target(BASE_URL_1);
        wt2 = client.target(BASE_URL_2);
    }
    
    @Test
    public void testJaxRsPut() throws Exception {
        Book book = new Book();
        book.setId(123);
        book.setName("Updated Book");
        Assert.assertEquals(
                Status.OK.getStatusCode(),
                wt1
                        .path("/books/123")
                        .request("application/xml")
                        .put(Entity.xml(book))
                        .getStatus());
        Assert.assertEquals(
                Status.OK.getStatusCode(),
                wt2
                        .path("/books/123")
                        .request("application/xml")
                        .put(Entity.xml(book))
                        .getStatus());
        final Bundle bundle = findBundle("filter-bundle");
        bundle.stop();
        bundle.start();
        Assert.assertEquals(
                Status.OK.getStatusCode(),
                wt2
                        .path("/books/123")
                        .request("application/xml")
                        .put(Entity.xml(book))
                        .getStatus());
    }

    private Bundle findBundle(String symbolicName) {
        for (Bundle b : bundleContext.getBundles()) {
            if (symbolicName.equals(b.getSymbolicName())) {
                return b;
            }
        }
        throw new IllegalStateException();
    }


    @Configuration
    public Option[] config() {
        return new Option[] {
            cxfBaseConfig(),
            features(cxfUrl, "cxf-core", "cxf-wsdl", "cxf-jaxrs", "http",
                    "cxf-bean-validation-core",
                    "cxf-bean-validation"),
            testUtils(),
            logLevel(LogLevel.INFO),
            provision(filterBundle()),
            provision(clientBundle())
        };
    }



    private InputStream filterBundle() {
        return TinyBundles.bundle()
                .set(Constants.BUNDLE_SYMBOLICNAME, "filter-bundle")
                .set(Constants.DYNAMICIMPORT_PACKAGE, "*")
                .set(Constants.EXPORT_PACKAGE, "org.apache.cxf.osgi.itests.jaxrs")
                .add(Book.class)
                .add(BookStore.class)
                .add(BookFilter.class)
                .add(BookFilterImpl.class)
                .add(FilterBundleActivator.class)
                .set(Constants.BUNDLE_ACTIVATOR, FilterBundleActivator.class.getName())
                .build(TinyBundles.withBnd());
    }

    private InputStream clientBundle() {
        return TinyBundles.bundle()
                .set(Constants.DYNAMICIMPORT_PACKAGE, "*")
                .add(ClientBundleActivator.class)
                .set(Constants.BUNDLE_ACTIVATOR, ClientBundleActivator.class.getName())
                .build(TinyBundles.withBnd());
    }

}
