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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;

@NoJSR250Annotations(unlessNull = "bus")
public class OASISCatalogManager {
    public static final String DEFAULT_CATALOG_NAME = "META-INF/jax-ws-catalog.xml";
    public static final String CATALOG_DEBUG_KEY = "OASISCatalogManager.catalog.debug.level";

    private static final Logger LOG =
        LogUtils.getL7dLogger(OASISCatalogManager.class);
    private static final String DEBUG_LEVEL = System.getProperty(CATALOG_DEBUG_KEY);
    

    private Object resolver;
    private Set<URL> loadedCatalogs = Collections.synchronizedSet(new HashSet<URL>());
    private Bus bus;

    public OASISCatalogManager() {
        resolver = getResolver();
    }
    
    public OASISCatalogManager(Bus b) {
        bus = b;
        resolver = getResolver();
        loadContextCatalogs(DEFAULT_CATALOG_NAME);
    }
    
    private static Object getResolver() {
        try {
            CatalogManager catalogManager = new CatalogManager();
            if (DEBUG_LEVEL != null) {
                catalogManager.debug.setDebug(Integer.parseInt(DEBUG_LEVEL));
            }
            catalogManager.setUseStaticCatalog(false);
            catalogManager.setIgnoreMissingProperties(true);
            CatalogResolver catalogResolver = new CatalogResolver(catalogManager);
            return catalogResolver.getCatalog();
        } catch (Throwable t) {
            //ignore
        }        
        return null;
    }

    public Bus getBus() {
        return bus;
    }

    @Resource
    public void setBus(Bus bus) {
        this.bus = bus;
        if (null != bus) {
            bus.setExtension(this, OASISCatalogManager.class);
        }
        loadContextCatalogs();
    }

    public void loadContextCatalogs() {
        loadContextCatalogs(DEFAULT_CATALOG_NAME);
    }
    public final void loadContextCatalogs(String name) {
        try {
            loadCatalogs(Thread.currentThread().getContextClassLoader(), name);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error loading " + name + " catalog files", e);
        }
    }

    public final void loadCatalogs(ClassLoader classLoader, String name) throws IOException {
        if (classLoader == null || resolver == null) {
            return;
        }

        Enumeration<URL> catalogs = classLoader.getResources(name);
        while (catalogs.hasMoreElements()) {
            URL catalogURL = catalogs.nextElement();
            if (!loadedCatalogs.contains(catalogURL)) {
                ((Catalog)resolver).parseCatalog(catalogURL);
                loadedCatalogs.add(catalogURL);
            }
        }
    }

    public final void loadCatalog(URL catalogURL) throws IOException {
        if (!loadedCatalogs.contains(catalogURL) && resolver != null) {
            if ("file".equals(catalogURL.getProtocol())) {
                try {
                    File file = new File(catalogURL.toURI());
                    if (!file.exists()) {
                        throw new FileNotFoundException(file.getAbsolutePath());
                    }
                } catch (URISyntaxException e) {
                    //just process as is
                }
            }

            ((Catalog)resolver).parseCatalog(catalogURL);

            loadedCatalogs.add(catalogURL);
        }
    }

    private static OASISCatalogManager getContextCatalog() {
        try {
            OASISCatalogManager oasisCatalog = new OASISCatalogManager();
            oasisCatalog.loadContextCatalogs();
            return oasisCatalog;
        } catch (Throwable ex) {
            return null;
        }
    }

    public static OASISCatalogManager getCatalogManager(Bus bus) {
        if (bus == null) {
            return getContextCatalog();
        }
        OASISCatalogManager catalog = bus.getExtension(OASISCatalogManager.class);
        if (catalog == null) {
            catalog = getContextCatalog();
            if (catalog != null) {
                bus.setExtension(catalog, OASISCatalogManager.class);
            }
        } 
        return catalog;
        
    }

    public String resolveSystem(String sys) throws MalformedURLException, IOException {
        if (resolver == null) {
            return null;
        }
        return ((Catalog)resolver).resolveSystem(sys);
    }

    public String resolveURI(String uri) throws MalformedURLException, IOException {
        if (resolver == null) {
            return null;
        }
        return ((Catalog)resolver).resolveURI(uri);
    }
    public String resolvePublic(String uri, String parent) throws MalformedURLException, IOException {
        if (resolver == null) {
            return null;
        }
        return ((Catalog)resolver).resolvePublic(uri, parent);
    }

}
