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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.common.openapi.DefaultApplicationFactory;
import org.apache.cxf.jaxrs.common.openapi.DelegatingServletConfig;
import org.apache.cxf.jaxrs.common.openapi.SwaggerProperties;
import org.apache.cxf.jaxrs.common.openapi.SyntheticServletConfig;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiConfig;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiSupport;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.message.Message;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.DefaultReaderConfig;
import io.swagger.jaxrs.config.ReaderConfig;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Swagger;
import io.swagger.models.auth.SecuritySchemeDefinition;

@Provider(value = Type.Feature, scope = Scope.Server)
public class Swagger2Feature extends AbstractSwaggerFeature implements SwaggerUiSupport, SwaggerProperties {
    private static final String SCHEMES_PROPERTY = "schemes";
    private static final String HOST_PROPERTY = "host";
    private static final String USE_PATH_CFG_PROPERTY = "use.path.based.config";

    private boolean scan;
    private boolean scanAllResources;

    private String ignoreRoutes;

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
    // Swagger UI configuration parameters (to be passed as query string).
    private SwaggerUiConfig swaggerUiConfig;

    @Override
    protected void calculateDefaultBasePath(Server server) {
        dynamicBasePath = true;
        super.calculateDefaultBasePath(server);
    }

    @Override
    protected void addSwaggerResource(Server server, Bus bus) {
        JAXRSServiceFactoryBean sfb =
            (JAXRSServiceFactoryBean) server.getEndpoint().get(JAXRSServiceFactoryBean.class.getName());

        ServerProviderFactory factory =
            (ServerProviderFactory)server.getEndpoint().get(ServerProviderFactory.class.getName());
        final ApplicationInfo appInfo = DefaultApplicationFactory.createApplicationInfoOrDefault(server, 
            factory, sfb, bus, isScan());

        List<Object> swaggerResources = new LinkedList<>();

        if (customizer == null) {
            customizer = new Swagger2Customizer();
        }
        ApiListingResource apiListingResource = new Swagger2ApiListingResource(customizer);
        swaggerResources.add(apiListingResource);

        List<Object> providers = new ArrayList<>();
        providers.add(new SwaggerSerializers());

        if (isRunAsFilter()) {
            providers.add(new SwaggerContainerRequestFilter(appInfo == null ? null : appInfo.getProvider(),
                                                            customizer));
        }

        final Properties swaggerProps = getSwaggerProperties(propertiesLocation, bus);
        final Registration swaggerUiRegistration = getSwaggerUi(bus, swaggerProps, isRunAsFilter());

        if (!isRunAsFilter()) {
            swaggerResources.addAll(swaggerUiRegistration.getResources());
        }

        providers.addAll(swaggerUiRegistration.getProviders());
        sfb.setResourceClassesFromBeans(swaggerResources);

        List<ClassResourceInfo> cris = sfb.getClassResourceInfo();
        if (!isRunAsFilter()) {
            for (ClassResourceInfo cri : cris) {
                if (ApiListingResource.class.isAssignableFrom(cri.getResourceClass())) {
                    InjectionUtils.injectContextProxies(cri, apiListingResource);
                }
            }
        }
        customizer.setClassResourceInfos(cris);
        customizer.setDynamicBasePath(dynamicBasePath);

        BeanConfig beanConfig = appInfo == null
            ? new BeanConfig()
            : new ApplicationBeanConfig(appInfo.getProvider());
        initBeanConfig(beanConfig, swaggerProps);

        Swagger swagger = beanConfig.getSwagger();
        if (swagger != null && securityDefinitions != null) {
            swagger.setSecurityDefinitions(securityDefinitions);
        }
        customizer.setBeanConfig(beanConfig);

        providers.add(new ReaderConfigFilter());

        if (beanConfig.isUsePathBasedConfig()) {
            providers.add(new ServletConfigProvider());
        }

        factory.setUserProviders(providers);
    }

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
        beanConfig.setTitle(theTitle);

        // description
        String theDescription = getDescription();
        if (theDescription == null && props != null) {
            theDescription = props.getProperty(DESCRIPTION_PROPERTY);
        }
        beanConfig.setDescription(theDescription);

        // contact
        String theContact = getContact();
        if (theContact == null && props != null) {
            theContact = props.getProperty(CONTACT_PROPERTY);
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

    @Override
    public Boolean isSupportSwaggerUi() {
        return supportSwaggerUi;
    }

    public void setSwaggerUiMediaTypes(Map<String, String> swaggerUiMediaTypes) {
        this.swaggerUiMediaTypes = swaggerUiMediaTypes;
    }

    @Override
    public Map<String, String> getSwaggerUiMediaTypes() {
        return swaggerUiMediaTypes;
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

    public boolean isScan() {
        return scan;
    }

    public void setScan(boolean scan) {
        this.scan = scan;
    }
    
    public void setSwaggerUiConfig(final SwaggerUiConfig swaggerUiConfig) {
        this.swaggerUiConfig = swaggerUiConfig;
    }
    
    @Override
    public SwaggerUiConfig getSwaggerUiConfig() {
        return swaggerUiConfig;
    }

    @Override
    public String findSwaggerUiRoot() {
        return SwaggerUi.findSwaggerUiRoot(swaggerUiMavenGroupAndArtifact, swaggerUiVersion);
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
                    return new SyntheticServletConfig(context) {
                        @Override
                        public String getInitParameter(String name) {
                            if (Objects.equals(SwaggerContextService.USE_PATH_BASED_CONFIG, name)) {
                                return "true";
                            } else {
                                return super.getInitParameter(name);
                            }
                        }
                    };
                }
            } else if (sc.getInitParameter(SwaggerContextService.USE_PATH_BASED_CONFIG) == null) {
                return new DelegatingServletConfig(sc) {
                    @Override
                    public String getInitParameter(String name) {
                        if (Objects.equals(SwaggerContextService.USE_PATH_BASED_CONFIG, name)) {
                            return "true";
                        } else {
                            return super.getInitParameter(name);
                        }
                    }
                };
            }

            return sc;
        }
    }

    @PreMatching
    protected static class SwaggerContainerRequestFilter extends Swagger2ApiListingResource
        implements ContainerRequestFilter {

        protected static final String APIDOCS_LISTING_PATH_JSON = "swagger.json";
        protected static final String APIDOCS_LISTING_PATH_YAML = "swagger.yaml";

        @Context
        protected MessageContext mc;

        private Application app;
        public SwaggerContainerRequestFilter(Application app, Swagger2Customizer customizer) {
            super(customizer);
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
                for (String route : ignoreRoutesParam.split(",")) {
                    routes.add(route.trim());
                }
                rc.setIgnoredRoutes(routes);
            }
            mc.getServletContext().setAttribute(ReaderConfig.class.getName(), rc);
        }
    }
}
