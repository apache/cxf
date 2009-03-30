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
package org.apache.cxf.catalog;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.wsdl.xml.WSDLLocator;

import org.xml.sax.InputSource;

import org.apache.cxf.Bus;
import org.apache.cxf.resource.ExtendedURIResolver;
import org.apache.cxf.transport.TransportURIResolver;

/**
 * Resolves WSDL URIs using Apache Commons Resolver API.
 */
public class CatalogWSDLLocator implements WSDLLocator {

    private String wsdlUrl;
    private ExtendedURIResolver resolver;
    private OASISCatalogManager manager;
    private String baseUri;
    
    public CatalogWSDLLocator(String wsdlUrl) {
        this.baseUri = wsdlUrl;
        this.resolver = new ExtendedURIResolver();        
    }
    public CatalogWSDLLocator(String wsdlUrl, OASISCatalogManager catalogManager) {
        this.baseUri = wsdlUrl;
        this.manager = catalogManager;
        this.resolver = new ExtendedURIResolver();
    }
    public CatalogWSDLLocator(String wsdlUrl, Bus b) {
        this.baseUri = wsdlUrl;
        this.manager = OASISCatalogManager.getCatalogManager(b);
        this.resolver = new TransportURIResolver(b);
    }

    public InputSource getBaseInputSource() {
        InputSource result = null;
        if (manager != null) {
            try {
                String s = manager.resolveSystem(baseUri);
                if (s != null) {
                    result = resolver.resolve(s, null);
                }
            } catch (MalformedURLException e) {
                //ignore
            } catch (IOException e) {
                //ignore
            }
        }
        if (result == null) {
            result = resolver.resolve(baseUri, null);
        }
        if (wsdlUrl == null
            && result != null) {
            wsdlUrl = result.getSystemId();
        }
        baseUri = resolver.getURI();
        return result;
    }

    public String getBaseURI() {
        if (wsdlUrl == null) {
            InputSource is = getBaseInputSource();
            if (is.getByteStream() != null) {
                try {
                    is.getByteStream().close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return wsdlUrl;
    }

    public String getLatestImportURI() {
        return resolver.getLatestImportURI();
    }

    public InputSource getImportInputSource(String parent, String importLocation) {
        String resolvedImportLocation = null;
        if (manager != null) {
            try {
                resolvedImportLocation = this.manager.resolveSystem(importLocation);
                if (resolvedImportLocation == null) {
                    resolvedImportLocation = manager.resolveURI(importLocation);
                }
                if (resolvedImportLocation == null) {
                    resolvedImportLocation = manager.resolvePublic(importLocation, parent);
                }
            } catch (IOException e) {
                throw new RuntimeException("Catalog resolution failed", e);
            }
        }

        InputSource in = null;
        if (resolvedImportLocation == null) {
            in = this.resolver.resolve(importLocation, parent);
        } else {
            in = this.resolver.resolve(resolvedImportLocation, null);
        }

        // XXX: If we return null (as per javadoc), a NPE is raised in WSDL4J code.
        // So let's return new InputSource() and let WSDL4J fail. Optionally, 
        // we can throw a similar exception as in CatalogXmlSchemaURIResolver.
        if (in == null) {
            in = new InputSource();
        }

        return in;
    }

    public void close() {
        resolver.close();
    }
}
