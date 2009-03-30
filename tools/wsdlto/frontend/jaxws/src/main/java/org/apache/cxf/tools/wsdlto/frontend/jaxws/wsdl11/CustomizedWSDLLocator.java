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
package org.apache.cxf.tools.wsdlto.frontend.jaxws.wsdl11;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import org.xml.sax.InputSource;

import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.resource.ExtendedURIResolver;

public class CustomizedWSDLLocator implements javax.wsdl.xml.WSDLLocator {
    private String wsdlUrl;
    private ExtendedURIResolver resolver;

    private String baseUri;
    private String importedUri;
 
    private OASISCatalogManager catalogResolver;
   
    private Map<String, Element> elementMap;
    private String latestImportURI;
    private Map<String, String> resolvedMap = new HashMap<String, String>();
    private boolean resolveFromMap;
    
    public CustomizedWSDLLocator(String wsdlUrl, Map<String, Element> map) {
        this.wsdlUrl = wsdlUrl;
        this.baseUri = this.wsdlUrl;
        resolver = new ExtendedURIResolver();
        elementMap = map; 
    }

    public void setCatalogResolver(final OASISCatalogManager cr) {
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
                resolvedMap.put(target, resolvedLocation);
                return this.resolver.resolve(resolvedLocation, base);
            }
        } catch (Exception e) {
            throw new RuntimeException("Catalog resolve failed: ", e);
        }
    }

    public InputSource getBaseInputSource() {
        if (elementMap.get(baseUri) != null) {
            Element ele = elementMap.get(baseUri);
            String content = XMLUtils.toString(ele);
            InputSource ins = new InputSource(new StringReader(content));
            ins.setSystemId(baseUri);
            return ins;
            
        }
        InputSource result = resolve(baseUri, null);
        baseUri = resolver.getURI();
        return result;
    }
    public String getBaseURI() {
        return baseUri;
    }
    public String getLatestImportURI() {
        if (this.resolveFromMap) {
            return this.latestImportURI;
            
        }
        return resolver.getLatestImportURI();
    }
    public InputSource getImportInputSource(String parent, String importLocation) {
        baseUri = parent;
        importedUri = importLocation;
        try {
            URI importURI = new URI(importLocation);
            if (!importURI.isAbsolute()) {
                URI parentURI = new URI(parent);
                importURI = parentURI.resolve(importURI);
            }
            
            if (elementMap.get(importURI.toString()) != null) {
                Element ele = elementMap.get(importURI.toString());
                String content = XMLUtils.toString(ele);

                InputSource ins = new InputSource(new StringReader(content));
                ins.setSystemId(importURI.toString());
                this.resolveFromMap = true;
                this.latestImportURI = importURI.toString();
                return ins;    
            }
            
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to Resolve " + importLocation, e);        
        } 
        resolveFromMap = false;
        return resolve(importedUri, baseUri);
    }
    public void close() {
        resolver.close();
    }
    
    public Map<String, String> getResolvedMap() {
        return resolvedMap;
    }

}
