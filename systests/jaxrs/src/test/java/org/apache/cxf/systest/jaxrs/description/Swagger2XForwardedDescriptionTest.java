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
package org.apache.cxf.systest.jaxrs.description;

import org.apache.cxf.jaxrs.model.AbstractResourceInfo;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class Swagger2XForwardedDescriptionTest extends AbstractSwagger2ServiceDescriptionTest {
    private static final String PORT = Swagger2Server.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(Swagger2Server.class));
        createStaticBus();
    }

    @Test
    public void testApiListingIsProperlyReturnedJSON() throws Exception {
        doTestApiListingIsProperlyReturnedJSON();
    }

    @Test
    public void testApiListingIsProperlyReturnedJSONXForwarded() throws Exception {
        doTestApiListingIsProperlyReturnedJSON(XForwarded.ONE_HOST);
    }

    @Test
    public void testApiListingIsProperlyReturnedJSONXForwardedManyHosts() throws Exception {
        doTestApiListingIsProperlyReturnedJSON(XForwarded.MANY_HOSTS);
    }

    @Override
    protected String getPort() {
        return PORT;
    }

    @Override
    protected String getExpectedFileYaml() {
        // TODO Auto-generated method stub
        return null;
    }
}
