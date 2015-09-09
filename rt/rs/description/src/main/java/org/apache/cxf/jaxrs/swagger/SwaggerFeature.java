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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.jaxrs.config.BeanConfig;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;

public class SwaggerFeature extends AbstractSwaggerFeature {

    @Override
    protected void addSwaggerResource(Server server) {
        ApiListingResourceJSON apiListingResource = new ApiListingResourceJSON();
        if (!runAsFilter) {
            List<Object> serviceBeans = new ArrayList<Object>();
            serviceBeans.add(apiListingResource);
            ((JAXRSServiceFactoryBean)server.getEndpoint().get(JAXRSServiceFactoryBean.class.getName())).
                setResourceClassesFromBeans(serviceBeans);
        }
        List<Object> providers = new ArrayList<Object>();
        if (runAsFilter) {
            providers.add(new SwaggerContainerRequestFilter(apiListingResource));
        }
        providers.add(new ResourceListingProvider());
        providers.add(new ApiDeclarationProvider());
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
        beanConfig.setTermsOfServiceUrl(getTermOfServiceUrl());
        beanConfig.setScan(isScan());
        beanConfig.setFilterClass(getFilterClass());
    }    

    @Override
    protected void setBasePathByAddress(String address) {
        setBasePath(address);
    }

    @PreMatching
    private static class SwaggerContainerRequestFilter implements ContainerRequestFilter {
        private static final String APIDOCS_LISTING_PATH = "api-docs";
        private static final Pattern APIDOCS_RESOURCE_PATH = Pattern.compile(APIDOCS_LISTING_PATH + "(/.+)");
        
        private ApiListingResourceJSON apiListingResource;
        @Context
        private MessageContext mc;
        public SwaggerContainerRequestFilter(ApiListingResourceJSON apiListingResource) {
            this.apiListingResource = apiListingResource;
        }

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            UriInfo ui = mc.getUriInfo();
            if (ui.getPath().endsWith(APIDOCS_LISTING_PATH)) {
                Response r = 
                    apiListingResource.resourceListing(null, mc.getServletConfig(), mc.getHttpHeaders(), ui);
                requestContext.abortWith(r);
            } else {
                final Matcher matcher = APIDOCS_RESOURCE_PATH.matcher(ui.getPath());
                
                if (matcher.find()) {
                    Response r = 
                        apiListingResource.apiDeclaration(matcher.group(1), 
                            null, mc.getServletConfig(), mc.getHttpHeaders(), ui);
                    requestContext.abortWith(r);                
                }
            }
        }
        
    }
}
