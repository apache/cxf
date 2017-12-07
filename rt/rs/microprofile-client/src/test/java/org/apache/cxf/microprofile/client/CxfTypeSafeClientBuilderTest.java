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
import java.net.URL;
import javax.ws.rs.core.Response;
import org.apache.cxf.microprofile.client.mock.HighPriorityClientReqFilter;
import org.apache.cxf.microprofile.client.mock.HighPriorityMBW;
import org.apache.cxf.microprofile.client.mock.LowPriorityClientReqFilter;
import org.apache.cxf.microprofile.client.mock.MyClient;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class CxfTypeSafeClientBuilderTest extends Assert {

    @Test
    public void testConfigMethods() {
        RestClientBuilder builder = RestClientBuilder.newBuilder();

        assertEquals("y", builder.property("x", "y").getConfiguration().getProperty("x"));

        assertTrue(builder.register(HighPriorityMBW.class).getConfiguration().isRegistered(HighPriorityMBW.class));

        HighPriorityMBW mbw = new HighPriorityMBW(1);
        assertTrue(builder.register(mbw).getConfiguration().isRegistered(mbw));

    }

    @Ignore
    @Test
    public void testConfigPriorityOverrides() throws Exception {
        RestClientBuilder builder = RestClientBuilder.newBuilder();
        builder.register(HighPriorityClientReqFilter.class); // annotation priority of 10
        builder.register(LowPriorityClientReqFilter.class, 5); // overriding priority to be 5 (preferred)
        MyClient c = builder.baseUrl(new URL("http://localhost/null")).build(MyClient.class);
        Response r = c.get();
        assertEquals("low", r.readEntity(String.class));
    }
/** using for test coverage
    @Override
    public RestClientBuilder register(Class<?> componentClass, int priority) {
      configImpl.register(componentClass, priority);
      return this;
    }

    @Override
    public RestClientBuilder register(Class<?> componentClass, Class<?>... contracts) {
      configImpl.register(componentClass, contracts);
      return this;
    }

    @Override
    public RestClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
      configImpl.register(componentClass, contracts);
      return this;
    }

    @Override
    public RestClientBuilder register(Object component, int priority) {
      configImpl.register(component, priority);
      return this;
    }

    @Override
    public RestClientBuilder register(Object component, Class<?>... contracts) {
      configImpl.register(component, contracts);
      return this;
    }

    @Override
    public RestClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {
      configImpl.register(component, contracts);
      return this;
    }
**/
}
