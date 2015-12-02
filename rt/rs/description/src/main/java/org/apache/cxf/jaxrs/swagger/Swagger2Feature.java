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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.doc.DocumentationProvider;
import org.apache.cxf.jaxrs.model.doc.JavaDocProvider;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.InjectionUtils;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.DefaultReaderConfig;
import io.swagger.jaxrs.config.ReaderConfig;
import io.swagger.jaxrs.listing.ApiListingResource;

public class Swagger2Feature extends AbstractSwaggerFeature {

    private String host;

    private String[] schemes;

    private boolean prettyPrint;

    private boolean scanAllResources;

    private String ignoreRoutes;

    private boolean dynamicBasePath;

    private boolean replaceTags;

    private DocumentationProvider javadocProvider;

    @Override
    protected void addSwaggerResource(Server server) {
        List<ClassResourceInfo> cris = Collections.emptyList();
        if (!runAsFilter) {
            List<Object> serviceBeans = new ArrayList<>();
            ApiListingResource apiListingResource = new ApiListingResource();
            serviceBeans.add(apiListingResource);
            JAXRSServiceFactoryBean sfb =
                    (JAXRSServiceFactoryBean) server.getEndpoint().get(JAXRSServiceFactoryBean.class.getName());
            sfb.setResourceClassesFromBeans(serviceBeans);
            cris = sfb.getClassResourceInfo();
            for (ClassResourceInfo cri : cris) {
                if (ApiListingResource.class == cri.getResourceClass()) {
                    InjectionUtils.injectContextProxiesAndApplication(cri, apiListingResource, null);
                }
            }
        }
        List<Object> providers = new ArrayList<>();
        if (runAsFilter) {
            providers.add(new SwaggerContainerRequestFilter());
        }
        providers.add(new Swagger2Serializers(dynamicBasePath, replaceTags, javadocProvider, cris));
        providers.add(new ReaderConfigFilter());
        ((ServerProviderFactory) server.getEndpoint().get(
                ServerProviderFactory.class.getName())).setUserProviders(providers);

        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setResourcePackage(getResourcePackage());
        beanConfig.setVersion(getVersion());
        beanConfig.setBasePath(getBasePath());
        beanConfig.setHost(getHost());
        beanConfig.setSchemes(getSchemes());
        beanConfig.setTitle(getTitle());
        beanConfig.setDescription(getDescription());
        beanConfig.setContact(getContact());
        beanConfig.setLicense(getLicense());
        beanConfig.setLicenseUrl(getLicenseUrl());
        beanConfig.setTermsOfServiceUrl(getTermsOfServiceUrl());
        beanConfig.setScan(isScan());
        beanConfig.setPrettyPrint(isPrettyPrint());
        beanConfig.setFilterClass(getFilterClass());
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String[] getSchemes() {
        return schemes;
    }

    public void setSchemes(String[] schemes) {
        this.schemes = schemes;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public boolean isScanAllResources() {
        return scanAllResources;
    }

    public void setScanAllResources(boolean scanAllResources) {
        this.scanAllResources = scanAllResources;
    }

    public String getIgnoreRoutes() {
        return ignoreRoutes;
    }

    public void setIgnoreRoutes(String ignoreRoutes) {
        this.ignoreRoutes = ignoreRoutes;
    }

    public void setDynamicBasePath(final boolean dynamicBasePath) {
        this.dynamicBasePath = dynamicBasePath;
    }

    public void setReplaceTags(final boolean replaceTags) {
        this.replaceTags = replaceTags;
    }

    public void setJavaDocPath(final String javaDocPath) throws Exception {
        this.javadocProvider = new JavaDocProvider(javaDocPath);
    }

    public void setJavaDocPaths(final String... javaDocPaths) throws Exception {
        this.javadocProvider = new JavaDocProvider(javaDocPaths);
    }

    public void setJavaDocURLs(final URL[] javaDocURLs) {
        this.javadocProvider = new JavaDocProvider(javaDocURLs);
    }

    @Override
    protected void setBasePathByAddress(String address) {
        if (!address.startsWith("/")) {
            // get the path part
            URI u = URI.create(address);
            setBasePath(u.getPath());
            setHost(u.getPort() < 0 ? u.getHost() : u.getHost() + ":" + u.getPort());
        } else {
            setBasePath(address);
        }
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

    private class ReaderConfigFilter implements ContainerRequestFilter {

        @Context
        private MessageContext mc;

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            ServletContext servletContext = mc.getServletContext();
            if (servletContext != null && servletContext.getAttribute(ReaderConfig.class.getName()) == null) {
                if (mc.getServletConfig() != null
                        && Boolean.valueOf(mc.getServletConfig().getInitParameter("scan.all.resources"))) {
                    addReaderConfig(mc.getServletConfig().getInitParameter("ignore.routes"));
                } else if (isScanAllResources()) {
                    addReaderConfig(getIgnoreRoutes());
                }
            }
        }

        private void addReaderConfig(String ignoreRoutesParam) {
            DefaultReaderConfig rc = new DefaultReaderConfig();
            rc.setScanAllResources(true);
            if (ignoreRoutesParam != null) {
                Set<String> routes = new LinkedHashSet<>();
                for (String route : StringUtils.split(ignoreRoutesParam, ",")) {
                    routes.add(route.trim());
                }
                rc.setIgnoredRoutes(routes);
            }
            mc.getServletContext().setAttribute(ReaderConfig.class.getName(), rc);
        }
    }
}
