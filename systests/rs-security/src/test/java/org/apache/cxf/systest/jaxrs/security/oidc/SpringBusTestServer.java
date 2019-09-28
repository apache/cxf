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

package org.apache.cxf.systest.jaxrs.security.oidc;

import java.net.URL;

import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;

import static org.junit.Assert.assertNotNull;

public class SpringBusTestServer extends AbstractBusTestServerBase {
    private final String port;
    private final URL serverConfigFile;

    public SpringBusTestServer(String template) {
        port = TestUtil.getPortNumber(template);
        serverConfigFile = getClass().getResource(template + ".xml");
        assertNotNull(serverConfigFile);
    }

    public String getPort() {
        return port;
    }

    protected void run() {
        setBus(new SpringBusFactory().createBus(serverConfigFile));
    }

}
