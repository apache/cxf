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
package org.apache.cxf.jaxrs.swagger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.InjectionUtils;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;

public class Swagger2Feature extends AbstractSwaggerFeature {

    @Override
    protected void addSwaggerResource(Server server) {
        if (!runAsFilter) {
            List<Object> serviceBeans = new ArrayList<Object>();
            ApiListingResource apiListingResource = new ApiListingResource();
            serviceBeans.add(apiListingResource);
            JAXRSServiceFactoryBean sfb = 
                (JAXRSServiceFactoryBean)server.getEndpoint().get(JAXRSServiceFactoryBean.class.getName());
            sfb.setResourceClassesFromBeans(serviceBeans);
            for (ClassResourceInfo cri : sfb.getClassResourceInfo()) {
                if (ApiListingResource.class == cri.getResourceClass()) {
                    InjectionUtils.injectContextProxiesAndApplication(cri, apiListingResource, null);
                }
            }
        }
        List<Object> providers = new ArrayList<Object>();
        if (runAsFilter) {
            providers.add(new SwaggerContainerRequestFilter());
        }
        providers.add(new SwaggerSerializers());
        ((ServerProviderFactory)server.getEndpoint().get(
            ServerProviderFactory.class.getName())).setUserProviders(providers);
        
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setResourcePackage(getResourcePackage());
        beanConfig.setVersion(getVersion());
        beanConfig.setBasePath(getBasePath());
        beanConfig.setTitle(getTitle());
        beanConfig.setDescription(getDescription());
        beanConfig.setContact(getContact());
        beanConfig.setLicense(getLicense());
        beanConfig.setLicenseUrl(getLicenseUrl());
        beanConfig.setScan(isScan());
    }

    @PreMatching
    private static class SwaggerContainerRequestFilter extends ApiListingResource implements ContainerRequestFilter {
        private static final String APIDOCS_LISTING_PATH_JSON = "swagger.json";
        private static final String APIDOCS_LISTING_PATH_YAML = "swagger.yaml";
        
        @Context
        private MessageContext mc;

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            UriInfo ui = mc.getUriInfo();
            if (ui.getPath().endsWith(APIDOCS_LISTING_PATH_JSON)) {
                Response r = getListingJson(null, mc.getServletConfig(), mc.getHttpHeaders(), ui);
                requestContext.abortWith(r);
            } else if (ui.getPath().endsWith(APIDOCS_LISTING_PATH_YAML)) {
                Response r = getListingYaml(null, mc.getServletConfig(), mc.getHttpHeaders(), ui); 
                requestContext.abortWith(r);
            }
        }
    }
}
