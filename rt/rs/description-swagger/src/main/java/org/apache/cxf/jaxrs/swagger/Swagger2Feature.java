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
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Priority;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.DefaultReaderConfig;
import io.swagger.jaxrs.config.ReaderConfig;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.models.Swagger;
import io.swagger.models.auth.SecuritySchemeDefinition;

@Provider(value = Type.Feature, scope = Scope.Server)
public class Swagger2Feature extends AbstractSwaggerFeature {

    private static final String DEFAULT_PROPS_LOCATION = "/swagger.properties";
    private static final String RESOURCE_PACKAGE_PROPERTY = "resource.package";
    private static final String TITLE_PROPERTY = "title";
    private static final String SCHEMES_PROPERTY = "schemes";
    private static final String VERSION_PROPERTY = "version";
    private static final String DESCRIPTION_PROPERTY = "description";
    private static final String CONTACT_PROPERTY = "contact";
    private static final String LICENSE_PROPERTY = "license";
    private static final String LICENSE_URL_PROPERTY = "license.url";
    private static final String TERMS_URL_PROPERTY = "terms.url";
    private static final String PRETTY_PRINT_PROPERTY = "pretty.print";
    private static final String FILTER_CLASS_PROPERTY = "filter.class";
    private static final String HOST_PROPERTY = "host";
    private static final String USE_PATH_CFG_PROPERTY = "use.path.based.config";
    private static final String SUPPORT_UI_PROPERTY = "support.swagger.ui";
    
    private boolean scanAllResources;

    private String ignoreRoutes;
    
    private Swagger2Serializers swagger2Serializers;

    private Boolean supportSwaggerUi;

    private String swaggerUiVersion;
    
    private String swaggerUiMavenGroupAndArtifact;

    private Map<String, String> swaggerUiMediaTypes;

    private boolean dynamicBasePath;
    
    private Map<String, SecuritySchemeDefinition> securityDefinitions;
    
    private Swagger2Customizer customizer;
    
    private String host;
    private String[] schemes;
    private Boolean prettyPrint;
    private Boolean usePathBasedConfig;
    
    private String propertiesLocation = DEFAULT_PROPS_LOCATION;

    @Override
    protected void calculateDefaultBasePath(Server server) {
        dynamicBasePath = true;
        super.calculateDefaultBasePath(server);
    }

    @SuppressWarnings("deprecation")
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
        ApiListingResource apiListingResource = new Swagger2ApiListingResource(customizer);
        swaggerResources.add(apiListingResource);
        
        List<Object> providers = new ArrayList<>();
        if (isRunAsFilter()) {
            providers.add(new SwaggerContainerRequestFilter(appInfo == null ? null : appInfo.getProvider()));
        }

        Properties swaggerProps = getSwaggerProperties(bus);
        if (checkSupportSwaggerUiProp(swaggerProps)) {
            String swaggerUiRoot = SwaggerUiResolver.findSwaggerUiRoot(swaggerUiMavenGroupAndArtifact, 
                                                                       swaggerUiVersion);
            if (swaggerUiRoot != null) {
                final SwaggerUiResourceLocator locator = new SwaggerUiResourceLocator(swaggerUiRoot);
                final SwaggerUIService swaggerUiService = new SwaggerUIService(locator, swaggerUiMediaTypes);
                if (!isRunAsFilter()) {
                    swaggerResources.add(swaggerUiService);
                } else {
                    providers.add(new SwaggerUIServiceFilter(swaggerUiService));
                }
                providers.add(new SwaggerUIResourceFilter(locator));
                
                bus.setProperty("swagger.service.ui.available", "true");
            }
        }
        
        sfb.setResourceClassesFromBeans(swaggerResources);
        
        List<ClassResourceInfo> cris = sfb.getClassResourceInfo();
        if (!isRunAsFilter()) {
            for (ClassResourceInfo cri : cris) {
                if (ApiListingResource.class.isAssignableFrom(cri.getResourceClass())) {
                    InjectionUtils.injectContextProxies(cri, apiListingResource);
                }
            }
        }
        if (customizer == null) {
            if (swagger2Serializers == null) {
                swagger2Serializers = new DefaultSwagger2Serializers();
            }
            swagger2Serializers.setClassResourceInfos(cris);
            swagger2Serializers.setDynamicBasePath(dynamicBasePath);
            providers.add(swagger2Serializers);
        } else {
            customizer.setClassResourceInfos(cris);
            customizer.setDynamicBasePath(dynamicBasePath);
        }

        BeanConfig beanConfig = appInfo == null 
            ? new BeanConfig() 
            : new ApplicationBeanConfig(appInfo.getProvider());
        initBeanConfig(beanConfig, swaggerProps);

        Swagger swagger = beanConfig.getSwagger();
        if (swagger != null && securityDefinitions != null) {
            swagger.setSecurityDefinitions(securityDefinitions);
        }
        
        if (customizer == null) {
            swagger2Serializers.setBeanConfig(beanConfig);
        } else {
            customizer.setBeanConfig(beanConfig);
        }
        
        providers.add(new ReaderConfigFilter());

        if (beanConfig.isUsePathBasedConfig()) {
            providers.add(new ServletConfigProvider());
        }

        ((ServerProviderFactory) server.getEndpoint().get(
                ServerProviderFactory.class.getName())).setUserProviders(providers);
    }

    protected boolean checkSupportSwaggerUiProp(Properties props) {
        Boolean theSupportSwaggerUI = this.supportSwaggerUi;
        if (theSupportSwaggerUI == null && props != null && props.containsKey(SUPPORT_UI_PROPERTY)) {
            theSupportSwaggerUI = PropertyUtils.isTrue(props.get(SUPPORT_UI_PROPERTY));
        }
        if (theSupportSwaggerUI == null) {
            theSupportSwaggerUI = true;
        }
        return theSupportSwaggerUI;
    }

    protected Properties getSwaggerProperties(Bus bus) {
        InputStream is = ResourceUtils.getClasspathResourceStream(propertiesLocation, 
                                                 AbstractSwaggerFeature.class, 
                                                 bus);
        Properties props = null;
        if (is != null) {
            props = new Properties();
            try {
                props.load(is);
            } catch (IOException ex) {
                props = null;
            } finally {
                try {
                    is.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }

        return props;
    }

    @SuppressWarnings("deprecation")
    protected void initBeanConfig(BeanConfig beanConfig, Properties props) {
        
        // resource package
        String theResourcePackage = getResourcePackage();
        if (theResourcePackage == null && props != null) {
            theResourcePackage = props.getProperty(RESOURCE_PACKAGE_PROPERTY);
        }
        beanConfig.setResourcePackage(theResourcePackage);
        
        // use path based configuration
        Boolean theUsePathBasedConfig = isUsePathBasedConfig();
        if (theUsePathBasedConfig == null && props != null) {
            theUsePathBasedConfig = PropertyUtils.isTrue(props.get(USE_PATH_CFG_PROPERTY));
        }
        if (theUsePathBasedConfig == null) {
            theUsePathBasedConfig = false;
        }
        beanConfig.setUsePathBasedConfig(theUsePathBasedConfig);
        
        // version 
        String theVersion = getVersion();
        if (theVersion == null && props != null) {
            theVersion = props.getProperty(VERSION_PROPERTY);
        }
        if (theVersion == null) {
            theVersion = "1.0.0";
        }
        beanConfig.setVersion(theVersion);
        
        // host
        String theHost = getHost();
        if (theHost == null && props != null) {
            theHost = props.getProperty(HOST_PROPERTY);
        }
        beanConfig.setHost(theHost);
        
        // schemes
        String[] theSchemes = getSchemes();
        if (theSchemes == null && props != null && props.containsKey(SCHEMES_PROPERTY)) {
            theSchemes = props.getProperty(SCHEMES_PROPERTY).split(",");
        }
        beanConfig.setSchemes(theSchemes);
        
        // title
        String theTitle = getTitle();
        if (theTitle == null && props != null) {
            theTitle = props.getProperty(TITLE_PROPERTY);
        }
        if (theTitle == null) {
            theTitle = "Sample REST Application";
        }
        beanConfig.setTitle(theTitle);
        
        // description
        String theDescription = getDescription();
        if (theDescription == null && props != null) {
            theDescription = props.getProperty(DESCRIPTION_PROPERTY);
        }
        if (theDescription == null) {
            theDescription = "The Application";
        }  
        beanConfig.setDescription(theDescription);
        
        // contact
        String theContact = getContact();
        if (theContact == null && props != null) {
            theContact = props.getProperty(CONTACT_PROPERTY);
        }
        if (theContact == null) {
            theContact = "users@cxf.apache.org";
        }
        beanConfig.setContact(theContact);
        
        // license
        String theLicense = getLicense();
        if (theLicense == null && !licenseWasSet) {
            if (props != null) {
                theLicense = props.getProperty(LICENSE_PROPERTY);
                if (theLicense != null && theLicense.isEmpty()) {
                    theLicense = null;
                }
            } else {
                theLicense = DEFAULT_LICENSE_VALUE;
            }
        }
        beanConfig.setLicense(theLicense);
        
        // license url
        String theLicenseUrl = getLicenseUrl();
        if (theLicenseUrl == null && props != null) {
            theLicenseUrl = props.getProperty(LICENSE_URL_PROPERTY);
        }
        if (theLicenseUrl == null && DEFAULT_LICENSE_VALUE.equals(theLicense)) {
            theLicenseUrl = DEFAULT_LICENSE_URL;
        }
        beanConfig.setLicenseUrl(theLicenseUrl);
        
        // terms of service url
        String theTermsUrl = getTermsOfServiceUrl();
        if (theTermsUrl == null && props != null) {
            theTermsUrl = props.getProperty(TERMS_URL_PROPERTY);
        }
        beanConfig.setTermsOfServiceUrl(theTermsUrl);
        
        // pretty print
        Boolean thePrettyPrint = isPrettyPrint();
        if (thePrettyPrint == null && props != null) {
            thePrettyPrint = PropertyUtils.isTrue(props.get(PRETTY_PRINT_PROPERTY));
        }
        if (thePrettyPrint == null) {
            thePrettyPrint = false;
        }
        beanConfig.setPrettyPrint(thePrettyPrint);
        
        // filter class
        String theFilterClass = getFilterClass();
        if (theFilterClass == null && props != null) {
            theFilterClass = props.getProperty(FILTER_CLASS_PROPERTY);
        }
        beanConfig.setFilterClass(theFilterClass);
        
        // scan 
        beanConfig.setScan(isScan());
        
        // base path is calculated dynamically
        beanConfig.setBasePath(getBasePath());
        
    }

    public Boolean isUsePathBasedConfig() {
        return usePathBasedConfig;
    }

    public void setUsePathBasedConfig(Boolean usePathBasedConfig) {
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

    public Boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(Boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }
    
    public Swagger2Customizer getCustomizer() {
        return customizer;
    }

    public void setCustomizer(Swagger2Customizer customizer) {
        this.customizer = customizer;
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
            if (getHost() == null) {
                setHost(u.getPort() < 0 ? u.getHost() : u.getHost() + ":" + u.getPort());
            }
        } else {
            setBasePath(address);
        }
    }

    /**
     * Set SwaggerUI Maven group and artifact using the "groupId/artifactId" format. 
     * @param swaggerUiMavenGroupAndArtifact
     */
    public void setSwaggerUiMavenGroupAndArtifact(String swaggerUiMavenGroupAndArtifact) {
        this.swaggerUiMavenGroupAndArtifact = swaggerUiMavenGroupAndArtifact;
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

    public void setSecurityDefinitions(Map<String, SecuritySchemeDefinition> securityDefinitions) {
        this.securityDefinitions = securityDefinitions;
    }

    public String getPropertiesLocation() {
        return propertiesLocation;
    }

    public void setPropertiesLocation(String propertiesLocation) {
        this.propertiesLocation = propertiesLocation;
    }
    
    private class ServletConfigProvider implements ContextProvider<ServletConfig> {

        @Override
        public ServletConfig createContext(Message message) {
            final ServletConfig sc = (ServletConfig)message.get("HTTP.CONFIG");
            
            // When deploying into OSGi container, it is possible to use embedded Jetty
            // transport. In this case, the ServletConfig is not available and Swagger 
            // does not take into account certain configuration parameters. To overcome 
            // that, the ServletConfig is synthesized from ServletContext instance.
            if (sc == null) {
                final ServletContext context = (ServletContext)message.get("HTTP.CONTEXT");
                if (context != null) {
                    return new SyntheticServletConfig(context);
                }
            } else if (sc != null && sc.getInitParameter(SwaggerContextService.USE_PATH_BASED_CONFIG) == null) {
                return new DelegatingServletConfig(sc);
            }
            
            return sc;
        }
    }
    
    @PreMatching
    protected static class SwaggerContainerRequestFilter extends ApiListingResource implements ContainerRequestFilter {

        protected static final String APIDOCS_LISTING_PATH_JSON = "swagger.json";
        protected static final String APIDOCS_LISTING_PATH_YAML = "swagger.yaml";

        @Context
        protected MessageContext mc;

        private Application app;
        public SwaggerContainerRequestFilter(Application app) {
            this.app = app;
        }

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            UriInfo ui = mc.getUriInfo();

            Response response = null;
            if (ui.getPath().endsWith(APIDOCS_LISTING_PATH_JSON)) {

                response = getListingJsonResponse(
                        app, mc.getServletContext(), mc.getServletConfig(), mc.getHttpHeaders(), ui);
            } else if (ui.getPath().endsWith(APIDOCS_LISTING_PATH_YAML)) {

                response = getListingYamlResponse(
                        app, mc.getServletContext(), mc.getServletConfig(), mc.getHttpHeaders(), ui);
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

        private final SwaggerUiResourceLocator locator;
        private final Map<String, String> mediaTypes;

        public SwaggerUIService(SwaggerUiResourceLocator locator, Map<String, String> mediaTypes) {
            this.locator = locator;
            this.mediaTypes = mediaTypes;
        }
        
        @GET
        @Path("{resource:.*}")
        public Response getResource(@Context UriInfo uriInfo, @PathParam("resource") String resourcePath) {
            if (resourcePath.contains(FAVICON)) {
                return Response.status(404).build();
            }
            
            try {
                final URL resourceURL = locator.locate(resourcePath);
                final String path = resourceURL.getPath();

                String mediaType = null;
                int ind = path.lastIndexOf('.');
                if (ind != -1 && ind < path.length()) {
                    String resourceExt = path.substring(ind + 1);
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
    @Priority(Priorities.USER)
    protected static class SwaggerUIResourceFilter implements ContainerRequestFilter {
        private static final Pattern PATTERN =
            Pattern.compile(
                  ".*[.]js|.*[.]gz|.*[.]map|oauth2*[.]html|.*[.]png|.*[.]css|.*[.]ico|"
                  + "/css/.*|/images/.*|/lib/.*|/fonts/.*"
            );
        
        private final SwaggerUiResourceLocator locator;
        
        SwaggerUIResourceFilter(SwaggerUiResourceLocator locator) {
            this.locator = locator;
        }

        @Override
        public void filter(ContainerRequestContext rc) throws IOException {
            if (HttpMethod.GET.equals(rc.getRequest().getMethod())) {
                UriInfo ui = rc.getUriInfo();
                String path = "/" + ui.getPath();
                if (PATTERN.matcher(path).matches() && locator.exists(path)) {
                    rc.setRequestUri(URI.create("api-docs" + path));
                }
            }
        }
    }
    @PreMatching
    @Priority(Priorities.USER + 1)
    protected static class SwaggerUIServiceFilter implements ContainerRequestFilter {
        private SwaggerUIService uiService;
        SwaggerUIServiceFilter(SwaggerUIService uiService) {
            this.uiService = uiService;
        }
        @Override
        public void filter(ContainerRequestContext rc) throws IOException {
            if (HttpMethod.GET.equals(rc.getRequest().getMethod())) {
                UriInfo ui = rc.getUriInfo();
                String path = ui.getPath();
                int uiPathIndex = path.lastIndexOf("api-docs");
                if (uiPathIndex >= 0) {
                    String resourcePath = uiPathIndex + 8 < path.length() 
                        ? path.substring(uiPathIndex + 8) : "";
                    rc.abortWith(uiService.getResource(ui, resourcePath));    
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
