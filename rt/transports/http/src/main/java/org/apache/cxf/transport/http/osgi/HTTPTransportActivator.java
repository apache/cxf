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

package org.apache.cxf.transport.http.osgi;

import java.util.Properties;

import org.apache.cxf.bus.blueprint.BlueprintNameSpaceHandlerFactory;
import org.apache.cxf.bus.blueprint.NamespaceHandlerRegisterer;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.cxf.transport.http.blueprint.HttpBPHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;

public class HTTPTransportActivator 
    implements BundleActivator {
    
    ServiceRegistration reg;
    ServiceRegistration reg2;
    
    public void start(BundleContext context) throws Exception {
        ConfigAdminHttpConduitConfigurer conduitConfigurer = new ConfigAdminHttpConduitConfigurer();

        Properties servProps = new Properties();
        servProps.put(Constants.SERVICE_PID, ConfigAdminHttpConduitConfigurer.FACTORY_PID);  
        reg2 = context.registerService(ManagedServiceFactory.class.getName(),
                                       conduitConfigurer, servProps);
        
        servProps = new Properties();
        servProps.put(Constants.SERVICE_PID,  "org.apache.cxf.http.conduit-configurer");  
        reg = context.registerService(HTTPConduitConfigurer.class.getName(),
                                conduitConfigurer, servProps);
        BlueprintNameSpaceHandlerFactory factory = new BlueprintNameSpaceHandlerFactory() {
            
            @Override
            public Object createNamespaceHandler() {
                return new HttpBPHandler();
            }
        };
        NamespaceHandlerRegisterer.register(context, factory,
                                            "http://cxf.apache.org/transports/http/configuration");  
    }

    public void stop(BundleContext context) throws Exception {
        reg.unregister();
        reg2.unregister();
    }

}
