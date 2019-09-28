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

package org.apache.cxf.ext.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URI;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Logging;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.ext.logging.event.PrintWriterEventSender;
import org.apache.cxf.service.factory.AbstractServiceFactoryBean;
import org.apache.cxf.service.factory.FactoryBeanListener;
import org.apache.cxf.service.factory.FactoryBeanListenerManager;

/**
 *
 */
@Deprecated
public class OldLoggingFactoryBeanListener implements FactoryBeanListener {

    public OldLoggingFactoryBeanListener(Bus b) {
        FactoryBeanListenerManager m = b.getExtension(FactoryBeanListenerManager.class);
        for (FactoryBeanListener f : m.getListeners()) {
            if ("OldLoggingFactoryBeanListener".equals(f.getClass().getSimpleName())) {
                m.removeListener(f);
            }
        }
    }

    /** {@inheritDoc}*/
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
            addLoggingSupport(ep, bus, cls.getAnnotation(Logging.class));
            break;
        }
        case SERVER_CREATED: {
            Class<?> cls = (Class<?>)args[2];
            if (cls == null) {
                return;
            }
            Server server = (Server)args[0];
            Bus bus = factory.getBus();
            addLoggingSupport(server.getEndpoint(), bus, cls.getAnnotation(Logging.class));
            break;
        }
        default:
            //do nothing
        }
    }
    
    private LogEventSender createEventSender(String location) {
        if (StringUtils.isEmpty(location)) {
            return null;
        }
        if ("<stdout>".equals(location)) {
            return new PrintWriterEventSender(System.out);
        } else if ("<stderr>".equals(location)) {
            return new PrintWriterEventSender(System.err);                
        } else if (location.startsWith("file:")) {
            try {
                URI uri = new URI(location);
                File file = new File(uri);
                PrintWriter writer = new PrintWriter(new FileWriter(file, true), true);
                return new PrintWriterEventSender(writer);
            } catch (Exception ex) {
                //stick with default
            }
        }
        return null;
    }

    private void addLoggingSupport(Endpoint endpoint, Bus bus, Logging annotation) {
        if (annotation != null) {
            LoggingFeature lf = new LoggingFeature();
            lf.setPrettyLogging(annotation.pretty());
            lf.setLimit(annotation.limit());
            lf.setLogBinary(annotation.showBinary());
            
            lf.setLogBinary(annotation.showBinary());
            
            LogEventSender in = createEventSender(annotation.outLocation());
            if (in != null) {
                lf.setOutSender(in);
            }
            LogEventSender out = createEventSender(annotation.inLocation());
            if (out != null) {
                lf.setInSender(out);
            }

            lf.initialize(endpoint, bus);
        }
    }
    


}
