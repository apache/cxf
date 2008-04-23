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

package org.apache.cxf.wsdl11;

import javax.wsdl.xml.WSDLLocator;

import org.xml.sax.InputSource;

import org.apache.cxf.resource.ExtendedURIResolver;
import org.apache.xml.resolver.Catalog;

public class WSDLLocatorImpl implements WSDLLocator {

    private String wsdlUrl;
    private ExtendedURIResolver resolver;

    private String baseUri;
    private String importedUri;

    private Catalog catalogResolver;

    public WSDLLocatorImpl(String wsdlUrl) {
        this.wsdlUrl = wsdlUrl;
        this.baseUri = this.wsdlUrl;
        resolver = new ExtendedURIResolver();
    }

    public void setCatalogResolver(final Catalog cr) {
        this.catalogResolver = cr;
    }

    private InputSource resolve(final String target, final String base) {
        try {
            String resolvedLocation = null;
            if (catalogResolver != null) {
                resolvedLocation  = catalogResolver.resolveSystem(target);
                
                if (resolvedLocation == null) {
                    resolvedLocation = catalogResolver.resolveURI(target);
                }
                if (resolvedLocation == null) {
                    resolvedLocation = catalogResolver.resolvePublic(target, base);
                }                
            }
            if (resolvedLocation == null) {
                return this.resolver.resolve(target, base);
            } else {
                return this.resolver.resolve(resolvedLocation, base);
            }
        } catch (Exception e) {
            throw new RuntimeException("Catalog resolve failed: ", e);
        }
    }

    public InputSource getBaseInputSource() {
        InputSource result = resolve(baseUri, null);
        baseUri = resolver.getURI();
        return result;
    }
    public String getBaseURI() {
        return baseUri;
    }
    public String getLatestImportURI() {
        return resolver.getLatestImportURI();
    }
    public InputSource getImportInputSource(String parent, String importLocation) {
        this.baseUri = parent;
        this.importedUri = importLocation;
        return resolve(this.importedUri, this.baseUri);
    }
    public void close() {
        resolver.close();
    }
}
