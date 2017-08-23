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

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;

public class SwaggerFeature extends AbstractSwaggerFeature {

    @Override
    @SuppressWarnings("deprecation")
    protected void addSwaggerResource(Server server, Bus bus) {
        ApiListingResourceJSON apiListingResource = new ApiListingResourceJSON();
        if (!isRunAsFilter()) {
            List<Object> serviceBeans = new ArrayList<Object>();
            serviceBeans.add(apiListingResource);
            ((JAXRSServiceFactoryBean)server.getEndpoint().get(JAXRSServiceFactoryBean.class.getName())).
                setResourceClassesFromBeans(serviceBeans);
        }
        List<Object> providers = new ArrayList<Object>();
        if (isRunAsFilter()) {
            providers.add(new SwaggerContainerRequestFilter(apiListingResource));
        }
        providers.add(new ResourceListingProvider());
        providers.add(new ApiDeclarationProvider());
        ((ServerProviderFactory)server.getEndpoint().get(
                ServerProviderFactory.class.getName())).setUserProviders(providers);
        
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setResourcePackage(getResourcePackage());
        String theVersion = getVersion();
        if (theVersion == null) {
            theVersion = "1.0.0";
        }
        beanConfig.setVersion(theVersion);
        beanConfig.setBasePath(getBasePath());
        // title
        String theTitle = getTitle();
        if (theTitle == null) {
            theTitle = "Sample REST Application";
        }
        beanConfig.setTitle(theTitle);
        String theDescription = getDescription();
        if (theDescription == null) {
            theDescription = "The Application";
        }  
        beanConfig.setDescription(theDescription);
        
        String theContact = getContact();
        if (theContact == null) {
            theContact = "users@cxf.apache.org";
        }
        beanConfig.setContact(theContact);

        // license
        String theLicense = getLicense();
        if (theLicense == null) {
            theLicense = DEFAULT_LICENSE_VALUE;
        }
        beanConfig.setLicense(theLicense);
        
        // license url
        String theLicenseUrl = null;
        if (!DEFAULT_LICENSE_VALUE.equals(theLicense)) {
            theLicenseUrl = getLicenseUrl();
        } else {
            theLicenseUrl = DEFAULT_LICENSE_URL;
        }
        beanConfig.setLicenseUrl(theLicenseUrl);

        beanConfig.setTermsOfServiceUrl(getTermsOfServiceUrl());
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
        SwaggerContainerRequestFilter(ApiListingResourceJSON apiListingResource) {
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
