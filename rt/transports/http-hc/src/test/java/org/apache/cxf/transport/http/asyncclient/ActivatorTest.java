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
package org.apache.cxf.transport.http.asyncclient;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.osgi.framework.BundleContext;

import org.easymock.EasyMock;
import org.junit.Test;

import static org.easymock.EasyMock.mock;

public class ActivatorTest {

    @Test
    public void testConduitConfigurerUpdates() {
        final Bus bus = BusFactory.getThreadDefaultBus();
        final AsyncHTTPConduitFactory conduitFactory = mock(AsyncHTTPConduitFactory.class);
        bus.setExtension(conduitFactory, AsyncHTTPConduitFactory.class);

        final Activator.ConduitConfigurer configurer = new Activator.ConduitConfigurer(mock(BundleContext.class)) {
            @Override
            public Object[] getServices() {
                return new Object[] {bus};
            }
        };

        //Dummy service properties that are expected to be passed to conduitFactory
        final Map<String, Object> properties = Collections.singletonMap("foo", "bar");
        conduitFactory.update(properties);
        EasyMock.replay(conduitFactory);

        //act and verify
        configurer.updated(new Hashtable<>(properties));
        EasyMock.verify(conduitFactory);
    }

}
