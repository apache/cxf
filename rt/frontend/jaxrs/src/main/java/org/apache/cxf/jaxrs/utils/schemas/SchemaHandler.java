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

package org.apache.cxf.jaxrs.utils.schemas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.common.util.ClasspathScanner;
import org.apache.cxf.common.xmlschema.LSInputImpl;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.ws.commons.schema.constants.Constants;

public class SchemaHandler {

    static final String DEFAULT_CATALOG_LOCATION = "classpath:META-INF/jax-rs-catalog.xml";

    private Schema schema;
    private Bus bus;
    private String catalogLocation;

    public SchemaHandler() {

    }

    public void setBus(Bus b) {
        bus = b;
    }

    @Deprecated
    public void setSchemas(List<String> locations) {
        setSchemaLocations(locations);
    }

    public void setSchemaLocations(List<String> locations) {
        schema = createSchema(locations, catalogLocation,
                              bus == null ? BusFactory.getThreadDefaultBus() : bus);
    }

    public void setCatalogLocation(String name) {
        this.catalogLocation = name;
    }

    public Schema getSchema() {
        return schema;
    }

    public static Schema createSchema(List<String> locations, String catalogLocation, final Bus bus) {

        SchemaFactory factory = SchemaFactory.newInstance(Constants.URI_2001_SCHEMA_XSD);
        try {
            List<Source> sources = new ArrayList<>();
            for (String loc : locations) {
                final List<URL> schemaURLs;
                if (loc.lastIndexOf('.') == -1 || loc.lastIndexOf('*') != -1) {
                    schemaURLs = ClasspathScanner.findResources(loc, "xsd");
                } else {
                    URL url = ResourceUtils.getResourceURL(loc, bus);
                    schemaURLs = url != null ? Collections.singletonList(url) : Collections.emptyList();
                }
                if (schemaURLs.isEmpty()) {
                    throw new IllegalArgumentException("Cannot find XML schema location: " + loc);
                }
                for (URL schemaURL : schemaURLs) {
                    Reader r = new BufferedReader(
                                   new InputStreamReader(schemaURL.openStream(), StandardCharsets.UTF_8));
                    StreamSource source = new StreamSource(r);
                    source.setSystemId(schemaURL.toString());
                    sources.add(source);
                }
            }
            if (sources.isEmpty()) {
                return null;
            }
            final OASISCatalogManager catalogResolver = OASISCatalogManager.getCatalogManager(bus);
            if (catalogResolver != null) {
                catalogLocation = catalogLocation == null
                    ? SchemaHandler.DEFAULT_CATALOG_LOCATION : catalogLocation;
                URL catalogURL = ResourceUtils.getResourceURL(catalogLocation, bus);
                if (catalogURL != null) {
                    try {
                        catalogResolver.loadCatalog(catalogURL);
                        factory.setResourceResolver(new LSResourceResolver() {

                            public LSInput resolveResource(String type, String namespaceURI, String publicId,
                                                           String systemId, String baseURI) {
                                try {
                                    String resolvedLocation = catalogResolver.resolveSystem(systemId);

                                    if (resolvedLocation == null) {
                                        resolvedLocation = catalogResolver.resolveURI(namespaceURI);
                                    }
                                    if (resolvedLocation == null) {
                                        resolvedLocation = catalogResolver.resolvePublic(
                                            publicId != null ? publicId : namespaceURI, systemId);
                                    }
                                    if (resolvedLocation != null) {
                                        InputStream resourceStream =
                                            ResourceUtils.getResourceStream(resolvedLocation, bus);
                                        if (resourceStream != null) {
                                            return new LSInputImpl(publicId, systemId, resourceStream);
                                        }
                                    }
                                } catch (Exception ex) {
                                    // ignore
                                }
                                return null;
                            }

                        });
                    } catch (IOException ex) {
                        throw new IllegalArgumentException("Catalog " + catalogLocation + " can not be loaded", ex);
                    }
                }
            }
            return factory.newSchema(sources.toArray(new Source[0]));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to load XML schema : " + ex.getMessage(), ex);
        }
    }

}
