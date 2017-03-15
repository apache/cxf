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

package org.apache.cxf.service.factory;


import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;

/**
 *
 */
public class OldLoggingFactoryBeanListener implements FactoryBeanListener {
    static final Logger LOG = LogUtils.getL7dLogger(OldLoggingFactoryBeanListener.class);

    /** {@inheritDoc}*/
    @SuppressWarnings("deprecation")
    public void handleEvent(Event ev, AbstractServiceFactoryBean factory, Object... args) {
        switch (ev) {
        case ENDPOINT_SELECTED: {
            Class<?> cls = (Class<?>)args[2];
            Endpoint ep = (Endpoint)args[1];
            Bus bus = factory.getBus();
            // To avoid the NPE
            if (cls == null) {
                return;
            }
            addLoggingSupport(ep, bus, cls.getAnnotation(org.apache.cxf.annotations.Logging.class));
            break;
        }
        case SERVER_CREATED: {
            Class<?> cls = (Class<?>)args[2];
            if (cls == null) {
                return;
            }
            Server server = (Server)args[0];
            Bus bus = factory.getBus();
            addLoggingSupport(server.getEndpoint(), bus, cls.getAnnotation(org.apache.cxf.annotations.Logging.class));
            break;
        }
        default:
            //do nothing
        }
    }


    @SuppressWarnings("deprecation")
    private void addLoggingSupport(Endpoint endpoint, Bus bus, org.apache.cxf.annotations.Logging annotation) {
        if (annotation != null) {
            org.apache.cxf.feature.LoggingFeature lf = new org.apache.cxf.feature.LoggingFeature(annotation);
            LOG.warning("Deprecated logging interceptors being used, switch to cxf-rt-ext-logging based logging.");
            lf.initialize(endpoint, bus);
        }
    }


}
