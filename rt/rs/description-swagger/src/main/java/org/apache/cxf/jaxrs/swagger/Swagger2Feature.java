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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.DefaultReaderConfig;
import io.swagger.jaxrs.config.ReaderConfig;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.jaxrs.listing.ApiListingResource;

@Provider(value = Type.Feature, scope = Scope.Server)
public class Swagger2Feature extends AbstractSwaggerFeature {
    private String host;

    private String[] schemes;

    private boolean prettyPrint;

    private boolean scanAllResources;

    private String ignoreRoutes;
    
    private Swagger2Serializers swagger2Serializers;

    private boolean supportSwaggerUi = true;

    private String swaggerUiVersion;

    private Map<String, String> swaggerUiMediaTypes;

    private boolean usePathBasedConfig;

    @Override
    protected void addSwaggerResource(Server server, Bus bus) {
        JAXRSServiceFactoryBean sfb =
            (JAXRSServiceFactoryBean) server.getEndpoint().get(JAXRSServiceFactoryBean.class.getName());
        
        ApplicationInfo appInfo = null;
        if (!isScan()) {
            ServerProviderFactory factory = 
                (ServerProviderFactory)server.getEndpoint().get(ServerProviderFactory.class.getName());
            appInfo = factory.getApplicationProvider();
            if (appInfo == null) {
                Set<Class<?>> serviceClasses = new HashSet<>();
                for (ClassResourceInfo cri : sfb.getClassResourceInfo()) {
                    serviceClasses.add(cri.getServiceClass());
                }
                appInfo = new ApplicationInfo(new DefaultApplication(serviceClasses), bus);
                server.getEndpoint().put(Application.class.getName(), appInfo);
            }
        } 
        
        List<Object> swaggerResources = new LinkedList<>();
        ApiListingResource apiListingResource = new ApiListingResource();
        swaggerResources.add(apiListingResource);
        
        SwaggerUIService swaggerUiService = null;
        if (supportSwaggerUi) {
            String swaggerUiRoot = SwaggerUiResolver.findSwaggerUiRoot(swaggerUiVersion);
            if (swaggerUiRoot != null) {
                swaggerUiService = new SwaggerUIService(swaggerUiRoot, swaggerUiMediaTypes);
                swaggerResources.add(swaggerUiService);
                bus.setProperty("swagger.service.ui.available", "true");
            }
        }
        
        sfb.setResourceClassesFromBeans(swaggerResources);
        
        List<ClassResourceInfo> cris = sfb.getClassResourceInfo();

        List<Object> providers = new ArrayList<>();
        if (runAsFilter) {
            providers.add(new SwaggerContainerRequestFilter());
        } else {
            for (ClassResourceInfo cri : cris) {
                if (ApiListingResource.class == cri.getResourceClass()) {
                    InjectionUtils.injectContextProxies(cri, apiListingResource);
                }
            }
        }
        if (swaggerUiService != null) {
            providers.add(new SwaggerUIFilter());
        }

        if (swagger2Serializers == null) {
            swagger2Serializers = new DefaultSwagger2Serializers();
        }
        swagger2Serializers.setClassResourceInfos(cris);
        providers.add(swagger2Serializers);

        providers.add(new ReaderConfigFilter());
        
        if (usePathBasedConfig) {
            providers.add(new ServletConfigProvider());
        }

        ((ServerProviderFactory) server.getEndpoint().get(
                ServerProviderFactory.class.getName())).setUserProviders(providers);
        BeanConfig beanConfig = appInfo == null 
            ? new BeanConfig() 
            : new ApplicationBeanConfig(appInfo.getProvider().getClasses());
        beanConfig.setResourcePackage(getResourcePackage());
        beanConfig.setUsePathBasedConfig(isUsePathBasedConfig());
        beanConfig.setVersion(getVersion());
        String basePath = getBasePath();
        beanConfig.setBasePath(basePath);
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

    public boolean isUsePathBasedConfig() {
        return usePathBasedConfig;
    }
    
    public void setUsePathBasedConfig(boolean usePathBasedConfig) {
        this.usePathBasedConfig = usePathBasedConfig;
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

    public void setSwagger2Serializers(final Swagger2Serializers swagger2Serializers) {
        this.swagger2Serializers = swagger2Serializers;
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

    public void setSwaggerUiVersion(String swaggerUiVersion) {
        this.swaggerUiVersion = swaggerUiVersion;
    }

    public void setSupportSwaggerUi(boolean supportSwaggerUi) {
        this.supportSwaggerUi = supportSwaggerUi;
    }

    public void setSwaggerUiMediaTypes(Map<String, String> swaggerUiMediaTypes) {
        this.swaggerUiMediaTypes = swaggerUiMediaTypes;
    }

    @javax.ws.rs.ext.Provider
    private class ServletConfigProvider implements ContextProvider<ServletConfig> {

        @Override
        public ServletConfig createContext(Message message) {
            final ServletConfig sc = (ServletConfig)message.get("HTTP.CONFIG");
            
            if (sc != null && sc.getInitParameter(SwaggerContextService.USE_PATH_BASED_CONFIG) == null) {
                return new ServletConfig() {
                    @Override
                    public String getServletName() {
                        return sc.getServletName();
                    }
                    
                    @Override
                    public ServletContext getServletContext() {
                        return sc.getServletContext();
                    }
                    
                    @Override
                    public Enumeration<String> getInitParameterNames() {
                        return sc.getInitParameterNames();
                    }
                    
                    @Override
                    public String getInitParameter(String name) {
                        if (Objects.equals(SwaggerContextService.USE_PATH_BASED_CONFIG, name)) {
                            return "true";
                        } else {
                            return sc.getInitParameter(name);
                        }
                    }
                };
            }
            
            return sc;
        }
    }
    
    @PreMatching
    protected static class SwaggerContainerRequestFilter extends ApiListingResource implements ContainerRequestFilter {

        protected static final MediaType APPLICATION_YAML_TYPE = JAXRSUtils.toMediaType("application/yaml");

        protected static final String APIDOCS_LISTING_PATH = "swagger";

        protected static final String APIDOCS_LISTING_PATH_JSON = APIDOCS_LISTING_PATH + ".json";

        protected static final String APIDOCS_LISTING_PATH_YAML = APIDOCS_LISTING_PATH + ".yaml";

        @Context
        protected MessageContext mc;

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            UriInfo ui = mc.getUriInfo();

            List<MediaType> mediaTypes = mc.getHttpHeaders().getAcceptableMediaTypes();

            Response response = null;
            if ((ui.getPath().endsWith(APIDOCS_LISTING_PATH)
                    && !JAXRSUtils.intersectMimeTypes(mediaTypes, MediaType.APPLICATION_JSON_TYPE).isEmpty())
                    || ui.getPath().endsWith(APIDOCS_LISTING_PATH_JSON)) {

                response = getListingJsonResponse(
                        null, mc.getServletContext(), mc.getServletConfig(), mc.getHttpHeaders(), ui);
            } else if ((ui.getPath().endsWith(APIDOCS_LISTING_PATH)
                    && !JAXRSUtils.intersectMimeTypes(mediaTypes, APPLICATION_YAML_TYPE).isEmpty())
                    || ui.getPath().endsWith(APIDOCS_LISTING_PATH_YAML)) {

                response = getListingYamlResponse(
                        null, mc.getServletContext(), mc.getServletConfig(), mc.getHttpHeaders(), ui);
            }

            if (response != null) {
                requestContext.abortWith(response);
            }
        }
    }

    protected class ReaderConfigFilter implements ContainerRequestFilter {

        @Context
        protected MessageContext mc;

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

        protected void addReaderConfig(String ignoreRoutesParam) {
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
    @Path("api-docs")
    public static class SwaggerUIService {
        private static final String FAVICON = "favicon";
        private static final Map<String, String> DEFAULT_MEDIA_TYPES;
        
        static {
            DEFAULT_MEDIA_TYPES = new HashMap<>();
            DEFAULT_MEDIA_TYPES.put("html", "text/html");
            DEFAULT_MEDIA_TYPES.put("png", "image/png");
            DEFAULT_MEDIA_TYPES.put("gif", "image/gif");
            DEFAULT_MEDIA_TYPES.put("css", "text/css");
            DEFAULT_MEDIA_TYPES.put("js", "application/javascript");
            DEFAULT_MEDIA_TYPES.put("eot", "application/vnd.ms-fontobject");
            DEFAULT_MEDIA_TYPES.put("ttf", "application/font-sfnt");
            DEFAULT_MEDIA_TYPES.put("svg", "image/svg+xml");
            DEFAULT_MEDIA_TYPES.put("woff", "application/font-woff");
            DEFAULT_MEDIA_TYPES.put("woff2", "application/font-woff2");
        }

        private final String swaggerUiRoot;

        private final Map<String, String> mediaTypes;

        public SwaggerUIService(String swaggerUiRoot, Map<String, String> mediaTypes) {
            this.swaggerUiRoot = swaggerUiRoot;
            this.mediaTypes = mediaTypes;
        }
        
        @GET
        @Path("{resource:.*}")
        public Response getResource(@Context UriInfo uriInfo, @PathParam("resource") String resourcePath) {
            if (resourcePath.contains(FAVICON)) {        
                return Response.status(404).build();
            }
            if (StringUtils.isEmpty(resourcePath) || "/".equals(resourcePath)) {        
                resourcePath = "index.html";
            }
            if (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            
            try {
                URL resourceURL = URI.create(swaggerUiRoot + resourcePath).toURL();
                
                String mediaType = null;
                int ind = resourcePath.lastIndexOf('.');
                if (ind != -1 && ind < resourcePath.length()) {
                    String resourceExt = resourcePath.substring(ind + 1);
                    if (mediaTypes != null && mediaTypes.containsKey(resourceExt)) {
                        mediaType = mediaTypes.get(resourceExt);
                    } else {
                        mediaType = DEFAULT_MEDIA_TYPES.get(resourceExt);
                    }
                }
                
                ResponseBuilder rb = Response.ok(resourceURL.openStream());
                if (mediaType != null) {
                    rb.type(mediaType);
                }
                return rb.build();
            } catch (IOException ex) {
                throw new NotFoundException(ex);
            }
        }
        
    }
    @PreMatching
    protected static class SwaggerUIFilter implements ContainerRequestFilter {
        private static final Pattern PATTERN = 
            Pattern.compile(".*[.]js|/css/.*|/images/.*|/lib/.*|.*ico|/fonts/.*"); 
        
        @Override
        public void filter(ContainerRequestContext rc) throws IOException {
            if (HttpMethod.GET.equals(rc.getRequest().getMethod())) {
                UriInfo ui = rc.getUriInfo();
                String path = "/" + ui.getPath();
                if (PATTERN.matcher(path).matches()) {
                    rc.setRequestUri(URI.create("api-docs" + path));
                }
            }
        }
    }
    
    protected static class DefaultApplication extends Application {
        Set<Class<?>> serviceClasses;
        DefaultApplication(Set<Class<?>> serviceClasses) {
            this.serviceClasses = serviceClasses;
        }
        @Override
        public Set<Class<?>> getClasses() {
            return serviceClasses;
        }
    }
    
}
