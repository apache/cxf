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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.ibm.wsdl.extensions.soap.SOAPHeaderImpl;
import com.ibm.wsdl.extensions.soap.SOAPHeaderSerializer;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertiesLoaderUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.wsdl.JAXBExtensionHelper;
import org.apache.cxf.wsdl.WSDLBuilder;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.cxf.wsdl.WSDLExtensibilityPlugin;

public class WSDLDefinitionBuilder implements WSDLBuilder<Definition> {

    protected static final Logger LOG = LogUtils.getL7dLogger(WSDLDefinitionBuilder.class);
    private static final String EXTENSIONS_RESOURCE = "META-INF/extensions.xml";
    private static final String WSDL_PLUGIN_RESOURCE = "META-INF/wsdl.plugin.xml";

    protected WSDLReader wsdlReader;
    protected Definition wsdlDefinition;
    final WSDLFactory wsdlFactory;
    final ExtensionRegistry registry;
    private List<Definition> importedDefinitions = new ArrayList<Definition>();

    private final Map<String, WSDLExtensibilityPlugin> wsdlPlugins
        = new HashMap<String, WSDLExtensibilityPlugin>();

    private Bus bus;

    public WSDLDefinitionBuilder(Bus b) {
        this();
        this.bus = b;
    }

    public WSDLDefinitionBuilder() {
        try {
            wsdlFactory = WSDLFactory.newInstance();
            registry = wsdlFactory.newPopulatedExtensionRegistry();
            QName header11 = new QName(WSDLConstants.NS_SOAP11, "header");
            QName header12 = new QName(WSDLConstants.NS_SOAP12, "header");
            registry.registerDeserializer(MIMEPart.class,
                                          header11,
                                          new SOAPHeaderSerializer());
            registry.registerSerializer(MIMEPart.class,
                                        header11,
                                        new SOAPHeaderSerializer());
            registry.mapExtensionTypes(MIMEPart.class, header11, SOAPHeaderImpl.class);
            registry.registerDeserializer(MIMEPart.class,
                                          header12,
                                          new SOAPHeaderSerializer());
            registry.registerSerializer(MIMEPart.class,
                                        header12,
                                        new SOAPHeaderSerializer());
            registry.mapExtensionTypes(MIMEPart.class, header12, SOAPHeaderImpl.class);
            

            registerInitialExtensions();
            wsdlReader = wsdlFactory.newWSDLReader();
            wsdlReader.setExtensionRegistry(registry);
            
            // TODO enable the verbose if in verbose mode.
            wsdlReader.setFeature("javax.wsdl.verbose", false);
            wsdlReader.setFeature("javax.wsdl.importDocuments", true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public WSDLDefinitionBuilder(boolean requirePlugins) {
        this();
        if (requirePlugins) {
            registerWSDLExtensibilityPlugins();
        }
    }

    public void setBus(Bus b) {
        this.bus = b;
    }

    public Definition build(String wsdlURL) {
        parseWSDL(wsdlURL);
        return wsdlDefinition;
    }

    @SuppressWarnings("unchecked")
    protected void parseWSDL(String wsdlURL) {
        try {

            wsdlReader.setExtensionRegistry(registry);

            WSDLLocatorImpl wsdlLocator = new WSDLLocatorImpl(wsdlURL);
            wsdlLocator.setCatalogResolver(OASISCatalogManager.getCatalogManager(bus).getCatalog());
            wsdlDefinition = wsdlReader.readWSDL(wsdlLocator);

            parseImports(wsdlDefinition);

            if (wsdlDefinition.getServices().isEmpty()) {
                for (Definition def : importedDefinitions) {
                    Set<QName> services = def.getServices().keySet();
                    for (QName sName : services) {
                        if (!wsdlDefinition.getServices().keySet().contains(sName)) {
                            wsdlDefinition.getServices().put(sName, def.getService(sName));
                        }
                    }
                }
            }
        } catch (Exception we) {
            Message msg = new Message("FAIL_TO_CREATE_WSDL_DEFINITION",
                                      LOG,
                                      wsdlURL,
                                      we.getMessage());
            throw new RuntimeException(msg.toString(), we);
        }
    }

    public static Collection<Import> getImports(final Definition wsdlDef) {
        Collection<Import> importList = new ArrayList<Import>();
        Map imports = wsdlDef.getImports();
        for (Iterator iter = imports.keySet().iterator(); iter.hasNext();) {
            String uri = (String)iter.next();
            List<Import> lst = CastUtils.cast((List)imports.get(uri));
            importList.addAll(lst);
        }
        return importList;
    }

    private void parseImports(Definition def) {
        for (Import impt : getImports(def)) {
            if (!importedDefinitions.contains(impt.getDefinition())) {
                importedDefinitions.add(impt.getDefinition());
                parseImports(impt.getDefinition());
            }
        }
    }

    public List<Definition> getImportedDefinitions() {
        return importedDefinitions;
    }

    private void registerInitialExtensions() throws BusException {
        Properties initialExtensions = null;
        try {
            initialExtensions = PropertiesLoaderUtils.loadAllProperties(EXTENSIONS_RESOURCE, Thread
                            .currentThread().getContextClassLoader());
        } catch (IOException ex) {
            throw new BusException(ex);
        }

        for (Iterator it = initialExtensions.keySet().iterator(); it.hasNext();) {
            StringTokenizer st = new StringTokenizer(initialExtensions.getProperty((String) it.next()), "=");
            String parentType = st.nextToken();
            String elementType = st.nextToken();
            try {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Registering extension: " + elementType + " for parent: " + parentType);
                }
                JAXBExtensionHelper.addExtensions(registry, parentType, elementType, 
                                                  Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException ex) {
                LOG.log(Level.WARNING, "EXTENSION_ADD_FAILED_MSG", ex);
            } catch (JAXBException ex) {
                LOG.log(Level.WARNING, "EXTENSION_ADD_FAILED_MSG", ex);
            }
        }
    }

    public ExtensionRegistry getExtenstionRegistry() {
        return registry;
    }

    public WSDLFactory getWSDLFactory() {
        return wsdlFactory;
    }

    public WSDLReader getWSDLReader() {
        return wsdlReader;
    }

    public Map<String, WSDLExtensibilityPlugin> getWSDLPlugins() {
        return wsdlPlugins;
    }

    private void registerWSDLExtensibilityPlugins() {
        Properties initialExtensions = null;
        try {
            initialExtensions = PropertiesLoaderUtils.loadAllProperties(WSDL_PLUGIN_RESOURCE, Thread
                            .currentThread().getContextClassLoader());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        for (Iterator it = initialExtensions.keySet().iterator(); it.hasNext();) {
            String key = (String) it.next();
            String pluginClz = initialExtensions.getProperty(key);
            try {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Registering : " + pluginClz + " for type: " + key);
                }

                WSDLExtensibilityPlugin plugin
                    = (WSDLExtensibilityPlugin)Class.forName(pluginClz).newInstance();
                plugin.setExtensionRegistry(registry);
                wsdlPlugins.put(key, plugin);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "EXTENSION_ADD_FAILED_MSG", ex);
            }
        }
    }
}
