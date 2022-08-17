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

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiConfig;
import org.apache.cxf.osgi.itests.AbstractServerActivator;

public class OpenApiTestActivator extends AbstractServerActivator {

    @Override
    protected Server createServer() {
        Bus bus = BusFactory.newInstance().createBus();
        bus.setExtension(OpenApiTestActivator.class.getClassLoader(), ClassLoader.class);
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setBus(bus);
        sf.setResourceClasses(OpenApiBookStore.class);

        OpenApiFeature openApiFeature = new OpenApiFeature();
        openApiFeature.setScan(false);
        SwaggerUiConfig swaggerUiConfig = new SwaggerUiConfig();
        swaggerUiConfig.setUrl("/cxf/jaxrs/openapi.json");
        openApiFeature.setSwaggerUiConfig(swaggerUiConfig);

        sf.getFeatures().add(openApiFeature);
        sf.setAddress("/jaxrs");
        return sf.create();
    }
}
