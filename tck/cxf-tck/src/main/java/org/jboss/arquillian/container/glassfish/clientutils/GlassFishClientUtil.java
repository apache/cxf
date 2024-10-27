/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Z.Paulovics
 */
package org.jboss.arquillian.container.glassfish.clientutils;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.filter.CsrfProtectionFilter;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.jboss.arquillian.container.glassfish.CommonGlassFishConfiguration;

import jakarta.ws.rs.core.MediaType;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static jakarta.ws.rs.core.HttpHeaders.USER_AGENT;

/**
 * This is a copy of the org.jboss.arquillian.container.glassfish.GlassFishClientUtil from 
 * arquillian-glassfish-common-jakarta-7.0.10.jar that overrides {@link ClientBuilder#newBuilder()} usages
 * with {@link JerseyClientBuilder} instance.
 */
public class GlassFishClientUtil {

    /**
     * Status for a successful GlassFish exit code deployment.
     */
    public static final String SUCCESS = "SUCCESS";

    /**
     * Status for a GlassFish exit code deployment which ended in warning.
     */
    public static final String WARNING = "WARNING";

    private CommonGlassFishConfiguration configuration;

    private String adminBaseUrl;

    private static final Logger log = Logger.getLogger(GlassFishClientUtil.class.getName());

    public GlassFishClientUtil(CommonGlassFishConfiguration configuration, String adminBaseUrl) {
        this.configuration = configuration;
        this.adminBaseUrl = adminBaseUrl;
    }

    public CommonGlassFishConfiguration getConfiguration() {
        return configuration;
    }

    public Map<String, String> getAttributes(String additionalResourceUrl) {
        Map<String, Object> responseMap = GETRequest(additionalResourceUrl);
        Map<String, String> attributes = new HashMap<String, String>();

        Map<String, Map<String, String>> resultExtraProperties = (Map<String, Map<String, String>>) responseMap.get("extraProperties");
        if (resultExtraProperties != null) {
            attributes = resultExtraProperties.get("entity");
        }

        return attributes;
    }

    public Map<String, String> getChildResources(String additionalResourceUrl) throws GlassFishClientException {
        Map<String, Object> responseMap = GETRequest(additionalResourceUrl);
        Map<String, String> childResources = new HashMap<String, String>();

        Map<String, Object> resultExtraProperties = (Map<String, Object>) responseMap.get("extraProperties");
        if (resultExtraProperties != null) {
            childResources = (Map<String, String>) resultExtraProperties.get("childResources");
        }

        return childResources;
    }

    /**
     * Create a WebTarget for accessing a subpath under the admin endpoint that has authorization, logging
     * and CSRF setup.
     *
     * @return WebTarget for admin base endpoint
     */
    public WebTarget prepareGET() {
        ClientBuilder builder = new JerseyClientBuilder();
        ClientConfig jerseyConfig = new ClientConfig();
        if (configuration.isAuthorisation()) {
            jerseyConfig.register(HttpAuthenticationFeature.basic(configuration.getAdminUser(), configuration.getAdminPassword()));
        }

        builder.withConfig(jerseyConfig);
        builder.register(new CsrfProtectionFilter());
        Client client = builder.build();
        return client.target(this.adminBaseUrl);
    }

    /**
     * Invoke a GET request against the adminSubPath
     * @param adminSubPath - subpath of the admin command
     * @return map of the parsed XML response
     */
    public Map<String, Object> GETRequest(String adminSubPath) {
        try {
            Invocation.Builder getBuilder = prepareClient(adminSubPath, false);
            Response response = getBuilder.buildGet().invoke();
            Map<String, Object> responseMap = getResponseMap(response);

            return responseMap;
        } catch (Exception e) {
            throw new GlassFishClientException(e);
        }
    }

    public List<Map<String,Object>> getInstancesList(String additionalResourceUrl) throws GlassFishClientException {
        Map<String, Object> responseMap = GETRequest(additionalResourceUrl);
        List<Map<String,Object>> instancesList = new ArrayList<Map<String,Object>>();

        Map<String, Object> resultExtraProperties = (Map<String, Object>) responseMap.get("extraProperties");
        if (resultExtraProperties != null) {
            instancesList = (List<Map<String,Object>>) resultExtraProperties.get("instanceList");
        }

        return instancesList;
    }

    public Map<String, Object> POSTMultiPartRequest(String additionalResourceUrl, FormDataMultiPart form) {
        try {
            Response response = prepareClient(additionalResourceUrl, true)
                .accept(MediaType.MULTIPART_FORM_DATA_TYPE)
                .buildPost(Entity.entity(form, MediaType.MULTIPART_FORM_DATA))
                .invoke();
            Map<String, Object> responseMap = getResponseMap(response);

        return responseMap;
        } catch (Exception e) {
            throw new GlassFishClientException(e);
        }
    }

    /**
     * Basic REST call preparation, with the additional resource url appended
     *
     * @param additionalResourceUrl
     *     url portion past the base to use
     *
     * @return the resource builder to execute
     */
    private Invocation.Builder prepareClient(String additionalResourceUrl, boolean multiPart) {
        ClientBuilder builder = new JerseyClientBuilder();
        ClientConfig jerseyConfig = new ClientConfig();
        if (configuration.isAuthorisation()) {
            jerseyConfig.register(HttpAuthenticationFeature.basic(configuration.getAdminUser(), configuration.getAdminPassword()));
        }
        if (multiPart) {
            jerseyConfig.register(MultiPartFeature.class);
        }

        builder.withConfig(jerseyConfig);
        builder.register(new CsrfProtectionFilter());
        Client client = builder.build();
        return client.target(this.adminBaseUrl)
            .path(additionalResourceUrl)
            .request(MediaType.APPLICATION_XML_TYPE)
            .header(USER_AGENT, GlassFishClientService.USER_AGENT_VALUE);
    }

    Map<String, Object> getResponseMap(Response response) throws GlassFishClientException {
        Map<String, Object> responseMap = new HashMap<>();
        String message = "";
        final String xmlDoc = response.readEntity(String.class);

        // Marshalling the XML format response to a java Map
        if (xmlDoc != null && !xmlDoc.isEmpty()) {
            responseMap = xmlToMap(xmlDoc);

            message = "exit_code: " + responseMap.get("exit_code")
                + ", message: " + responseMap.get("message");
        }

        Response.StatusType status = response.getStatusInfo();
        if (status.getFamily() == Response.Status.Family.SUCCESSFUL) {
            // O.K. the jersey call was successful, what about the GlassFish server response?
            if (responseMap.get("exit_code") == null) {
                throw new GlassFishClientException(message);
            } else if (WARNING.equals(responseMap.get("exit_code"))) {
                // Warning is not a failure - some warnings in GlassFish are inevitable (i.e. persistence-related: ARQ-606)
                log.warning("Deployment resulted in a warning: " + message);
            } else if (!SUCCESS.equals(responseMap.get("exit_code"))) {
                // Response is not a warning nor success - it's surely a failure.
                throw new GlassFishClientException(message);
            }
        } else if (status.getReasonPhrase().contains("Not Found")) {
            // the REST resource can not be found (for optional resources it can be O.K.)
            message += " [status: " + status.getFamily() + " reason: " + status.getReasonPhrase() + "]";
            log.warning(message);
        } else {
            message += " [status: " + status.getFamily() + " reason: " + status.getReasonPhrase() + "]";
            log.severe(message);
            throw new GlassFishClientException(message);
        }

        return responseMap;
    }

    /**
     * Marshalling a Glassfish Mng API response XML document to a java Map object
     *
     * @param document XML
     *
     * @return map containing the XML doc representation in java map format
     */
    public Map<String, Object> xmlToMap(String document) {

        if (document == null) {
            return new HashMap<>();
        }

        InputStream input = null;
        Map<String, Object> map = null;
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_VALIDATING, false);
            input = new ByteArrayInputStream(document.trim().getBytes("UTF-8"));
            XMLStreamReader stream = factory.createXMLStreamReader(input);
            while (stream.hasNext()) {
                int currentEvent = stream.next();
                if (currentEvent == XMLStreamConstants.START_ELEMENT) {
                    if ("map".equals(stream.getLocalName())) {
                        map = resolveXmlMap(stream);
                    }
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        } finally {
            try {
                input.close();
            } catch (IOException ex) {
                log.log(Level.SEVERE, null, ex);
            }
        }

        return map;
    }

    private Map<String, Object> resolveXmlMap(XMLStreamReader stream) throws XMLStreamException {

        boolean endMapFlag = false;
        Map<String, Object> entry = new HashMap<String, Object>();
        String key = null;
        String elementName = null;

        while (!endMapFlag) {

            int currentEvent = stream.next();
            if (currentEvent == XMLStreamConstants.START_ELEMENT) {

                if ("entry".equals(stream.getLocalName())) {
                    key = stream.getAttributeValue(null, "key");
                    String value = stream.getAttributeValue(null, "value");
                    if (value != null) {
                        entry.put(key, value);
                        key = null;
                    }
                } else if ("map".equals(stream.getLocalName())) {
                    Map value = resolveXmlMap(stream);
                    entry.put(key, value);
                } else if ("list".equals(stream.getLocalName())) {
                    List value = resolveXmlList(stream);
                    entry.put(key, value);
                } else {
                    elementName = stream.getLocalName();
                }
            } else if (currentEvent == XMLStreamConstants.END_ELEMENT) {

                if ("map".equals(stream.getLocalName())) {
                    endMapFlag = true;
                }
                elementName = null;
            } else {

                String document = stream.getText();
                if (elementName != null) {
                    if ("number".equals(elementName)) {
                        if (document.contains(".")) {
                            entry.put(key, Double.parseDouble(document));
                        } else {
                            entry.put(key, Long.parseLong(document));
                        }
                    } else if ("string".equals(elementName)) {
                        entry.put(key, document);
                    }
                    elementName = null;
                }
            } // end if
        } // end while
        return entry;
    }

    private List resolveXmlList(XMLStreamReader stream) throws XMLStreamException {

        boolean endListFlag = false;
        List list = new ArrayList();
        String elementName = null;

        while (!endListFlag) {

            int currentEvent = stream.next();
            if (currentEvent == XMLStreamConstants.START_ELEMENT) {
                if ("map".equals(stream.getLocalName())) {
                    list.add(resolveXmlMap(stream));
                } else {
                    elementName = stream.getLocalName();
                }
            } else if (currentEvent == XMLStreamConstants.END_ELEMENT) {

                if ("list".equals(stream.getLocalName())) {
                    endListFlag = true;
                }
                elementName = null;
            } else {

                String document = stream.getText();
                if (elementName != null) {
                    if ("number".equals(elementName)) {
                        if (document.contains(".")) {
                            list.add(Double.parseDouble(document));
                        } else {
                            list.add(Long.parseLong(document));
                        }
                    } else if ("string".equals(elementName)) {
                        list.add(document);
                    }
                    elementName = null;
                }
            } // end if
        } // end while
        return list;
    }
}
