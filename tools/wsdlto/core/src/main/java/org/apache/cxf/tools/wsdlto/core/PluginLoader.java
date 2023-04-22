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

package org.apache.cxf.tools.wsdlto.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Document;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.FrontEndGenerator;
import org.apache.cxf.tools.common.Processor;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolContainer;
import org.apache.cxf.tools.plugin.DataBinding;
import org.apache.cxf.tools.plugin.FrontEnd;
import org.apache.cxf.tools.plugin.Generator;
import org.apache.cxf.tools.plugin.Plugin;

public final class PluginLoader {
    public static final String DEFAULT_PROVIDER_NAME = "cxf.apache.org";
    public static final Logger LOG = LogUtils.getL7dLogger(PluginLoader.class);
    private static PluginLoader pluginLoader;
    private static final String PLUGIN_FILE_NAME = "META-INF/tools-plugin.xml";

    private Map<String, Plugin> plugins = new LinkedHashMap<>();

    private Map<String, FrontEnd> frontends = new LinkedHashMap<>();

    private Map<String, DataBinding> databindings = new TreeMap<>();

    private JAXBContext jaxbContext;

    private PluginLoader() {
        init();
    }

    private void init() {
        try {
            jaxbContext = JAXBContext.newInstance("org.apache.cxf.tools.plugin");
            loadPlugins(ClassLoaderUtils.getResources(PLUGIN_FILE_NAME, getClass()));
        } catch (JAXBException e) {
            Message msg = new Message("JAXB_CONTEXT_INIT_FAIL", LOG);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg);
        } catch (IOException ioe) {
            Message msg = new Message("LOAD_PLUGIN_EXCEPTION", LOG);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg);
        }
    }

    public void refresh() {
        init();
    }

    private void loadPlugins(List<URL> pluginFiles) throws IOException {
        if (pluginFiles == null) {
            LOG.log(Level.WARNING, "FOUND_NO_PLUGINS");
            return;
        }

        for (URL url : pluginFiles) {
            loadPlugin(url);
        }
    }
    public static PluginLoader newInstance() {
        return new PluginLoader();
    }
    public static PluginLoader getInstance() {
        if (pluginLoader == null) {
            pluginLoader = new PluginLoader();
        }
        return pluginLoader;
    }

    public static void unload() {
        pluginLoader = null;
    }

    public void loadPlugin(URL url) throws IOException {
        try {
            LOG.log(Level.FINE, "PLUGIN_LOADING", url);
            loadPlugin(getPlugin(url));
        } catch (JAXBException e) {
            Message msg = new Message("PLUGIN_LOAD_FAIL", LOG, url);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg, e);
        }
    }

    public void loadPlugin(String resource) {
        try {
            LOG.log(Level.FINE, "PLUGIN_LOADING", resource);
            loadPlugin(getPlugin(resource));
        } catch (FileNotFoundException fe) {
            Message msg = new Message("PLUGIN_FILE_NOT_FOUND", LOG, resource);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg, fe);
        } catch (JAXBException | IOException e) {
            Message msg = new Message("PLUGIN_LOAD_FAIL", LOG, resource);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg, e);
        }

    }

    protected void loadPlugin(Plugin plugin) {
        if (plugin.getFrontend().size() > 0) {
            LOG.log(Level.FINE, "FOUND_FRONTENDS", new Object[]{plugin.getName(),
                                                                plugin.getFrontend().size()});
        }

        for (FrontEnd frontend : plugin.getFrontend()) {
            LOG.log(Level.FINE, "LOADING_FRONTEND", new Object[]{frontend.getName(), plugin.getName()});
            if (StringUtils.isEmpty(frontend.getName())) {
                LOG.log(Level.WARNING, "FRONTEND_MISSING_NAME", plugin.getName());
                continue;
            }

            if (frontends.containsKey(frontend.getName())
                && DEFAULT_PROVIDER_NAME.equals(plugin.getProvider())) {
                Message msg = new Message("REPLACED_DEFAULT_FRONTEND", LOG, frontend.getName());
                LOG.log(Level.INFO, msg.toString());
                continue;
            }
            frontends.put(frontend.getName(), frontend);
        }

        if (plugin.getDatabinding().size() > 0) {
            LOG.log(Level.FINE, "FOUND_DATABINDINGS", new Object[]{plugin.getName(),
                                                                   plugin.getDatabinding().size()});
        }

        for (DataBinding databinding : plugin.getDatabinding()) {
            LOG.log(Level.FINE, "LOADING_DATABINDING", new Object[]{databinding.getName(), plugin.getName()});
            if (StringUtils.isEmpty(databinding.getName())) {
                LOG.log(Level.WARNING, "DATABINDING_MISSING_NAME", plugin.getName());
                continue;
            }
            if (databindings.containsKey(databinding.getName())
                && DEFAULT_PROVIDER_NAME.equals(plugin.getProvider())) {
                Message msg = new Message("REPLACED_DEFAULT_DATABINDING", LOG, databinding.getName());
                LOG.log(Level.INFO, msg.toString());
                continue;
            }
            databindings.put(databinding.getName(), databinding);
        }
    }

    protected Plugin getPlugin(URL url) throws IOException, JAXBException, FileNotFoundException {
        Plugin plugin = plugins.get(url.toString());
        if (plugin == null) {
            try (InputStream is = url.openStream()) {
                plugin = getPlugin(is);
            }
            if (plugin == null || StringUtils.isEmpty(plugin.getName())) {
                Message msg = new Message("PLUGIN_LOAD_FAIL", LOG, url);
                LOG.log(Level.SEVERE, msg.toString());
                throw new ToolException(msg);
            }
            plugins.put(url.toString(), plugin);
        }
        return plugin;
    }

    protected Plugin getPlugin(String resource) throws JAXBException, IOException, FileNotFoundException {
        Plugin plugin = plugins.get(resource);
        if (plugin == null) {
            File resourceFile = new File(resource);

            try (InputStream is = resourceFile.exists()
                ? new BufferedInputStream(new FileInputStream(resourceFile))
                : getClass().getResourceAsStream(resource)) {

                if (is == null) {
                    Message msg = new Message("PLUGIN_MISSING", LOG, resource);
                    LOG.log(Level.SEVERE, msg.toString());
                    throw new ToolException(msg);
                }
                plugin = getPlugin(is);
            }

            if (plugin == null || StringUtils.isEmpty(plugin.getName())) {
                Message msg = new Message("PLUGIN_LOAD_FAIL", LOG, resource);
                LOG.log(Level.SEVERE, msg.toString());
                throw new ToolException(msg);
            }
            plugins.put(resource, plugin);
        }
        return plugin;
    }

    private Plugin getPlugin(InputStream is) throws JAXBException {
        try {
            Document doc = StaxUtils.read(is);
            return JAXBUtils.unmarshall(jaxbContext, doc.getDocumentElement(), Plugin.class).getValue();
        } catch (XMLStreamException xse) {
            throw new JAXBException(xse);
        }
    }

    public FrontEnd getFrontEnd(String name) {
        FrontEnd frontend = frontends.get(name);

        if (frontend == null) {
            Message msg = new Message("FRONTEND_MISSING", LOG, name);
            throw new ToolException(msg);
        }
        return frontend;
    }

    private String getGeneratorClass(FrontEnd frontend, Generator generator) {
        String fullPackage = generator.getPackage();
        if (StringUtils.isEmpty(fullPackage)) {
            fullPackage = frontend.getGenerators().getPackage();
        }
        if (StringUtils.isEmpty(fullPackage)) {
            fullPackage = frontend.getPackage();
        }
        return fullPackage + '.' + generator.getName();
    }

    private List<FrontEndGenerator> getFrontEndGenerators(FrontEnd frontend) {
        List<FrontEndGenerator> generators = new ArrayList<>();

        String fullClzName = null;
        try {
            for (Generator generator : frontend.getGenerators().getGenerator()) {
                fullClzName = getGeneratorClass(frontend, generator);
                Class<?> clz = ClassLoaderUtils.loadClass(fullClzName, this.getClass());
                generators.add((FrontEndGenerator)clz.getDeclaredConstructor().newInstance());
            }
        } catch (Exception e) {
            Message msg = new Message("FRONTEND_PROFILE_LOAD_FAIL", LOG, fullClzName);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg, e);
        }

        return generators;
    }

    private FrontEndProfile loadFrontEndProfile(String fullClzName) {
        final FrontEndProfile profile;
        try {
            Class<?> clz = ClassLoaderUtils.loadClass(fullClzName, this.getClass());
            profile = (FrontEndProfile)clz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Message msg = new Message("FRONTEND_PROFILE_LOAD_FAIL", LOG, fullClzName);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg, e);
        }
        return profile;
    }

    private Processor loadProcessor(String fullClzName) {
        final Processor processor;
        try {
            processor = (Processor) ClassLoaderUtils.loadClass(fullClzName, getClass())
                .getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Message msg = new Message("LOAD_PROCESSOR_FAILED", LOG, fullClzName);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg, e);
        }
        return processor;
    }

    private Class<? extends ToolContainer> loadContainerClass(String fullClzName) {
        final Class<?> clz;
        try {
            clz = ClassLoaderUtils.loadClass(fullClzName, getClass());
        } catch (Exception e) {
            Message msg = new Message("LOAD_CONTAINER_CLASS_FAILED", LOG, fullClzName);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg, e);
        }

        if (!ToolContainer.class.isAssignableFrom(clz)) {
            Message message = new Message("CLZ_SHOULD_IMPLEMENT_INTERFACE", LOG, clz.getName());
            LOG.log(Level.SEVERE, message.toString());
            throw new ToolException(message);
        }

        return clz.asSubclass(ToolContainer.class);
    }

    private String getFrontEndProfileClass(FrontEnd frontend) {
        if (StringUtils.isEmpty(frontend.getProfile())) {
            return "org.apache.cxf.tools.wsdlto.core.FrontEndProfile";
        }
        return frontend.getPackage() + '.' + frontend.getProfile();
    }

    private String getProcessorClass(FrontEnd frontend) {
        String pkgName = frontend.getProcessor().getPackage();
        if (StringUtils.isEmpty(pkgName)) {
            pkgName = frontend.getPackage();
        }
        return pkgName + '.' + frontend.getProcessor().getName();
    }

    private String getContainerClass(FrontEnd frontend) {
        return getContainerPackage(frontend) + '.' + frontend.getContainer().getName();
    }

    private String getContainerPackage(FrontEnd frontend) {
        String pkgName = frontend.getContainer().getPackage();
        if (StringUtils.isEmpty(pkgName)) {
            pkgName = frontend.getPackage();
        }
        return pkgName;
    }

    private String getToolspec(FrontEnd frontend) {
        String toolspec = frontend.getContainer().getToolspec();
        return '/' + getContainerPackage(frontend).replace('.', '/') + '/' + toolspec;
    }

    private AbstractWSDLBuilder loadBuilder(String fullClzName) {
        final AbstractWSDLBuilder builder;
        try {
            builder = (AbstractWSDLBuilder) ClassLoaderUtils
                .loadClass(fullClzName, getClass()).getDeclaredConstructor().newInstance();

        } catch (Exception e) {
            Message msg = new Message("LOAD_PROCESSOR_FAILED", LOG, fullClzName);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg, e);
        }
        return builder;
    }

    private String getBuilderClass(FrontEnd frontend) {
        String pkgName = frontend.getBuilder().getPackage();
        if (StringUtils.isEmpty(pkgName)) {
            pkgName = frontend.getPackage();
        }
        return pkgName + '.' + frontend.getBuilder().getName();
    }

    public FrontEndProfile getFrontEndProfile(String name) {
        FrontEnd frontend = getFrontEnd(name);

        FrontEndProfile profile = loadFrontEndProfile(getFrontEndProfileClass(frontend));

        for (FrontEndGenerator generator : getFrontEndGenerators(frontend)) {
            profile.registerGenerator(generator);
        }

        if (frontend.getProcessor() != null) {
            profile.setProcessor(loadProcessor(getProcessorClass(frontend)));
        }
        if (frontend.getContainer() != null) {
            profile.setContainerClass(loadContainerClass(getContainerClass(frontend)));
            profile.setToolspec(getToolspec(frontend));
        }
        if (frontend.getBuilder() != null) {
            profile.setWSDLBuilder(loadBuilder(getBuilderClass(frontend)));
        }
        return profile;
    }

    public DataBinding getDataBinding(String name) {
        DataBinding databinding = databindings.get(name);
        if (databinding == null) {
            Message msg = new Message("DATABINDING_MISSING", LOG, name);
            throw new ToolException(msg);
        }
        return databinding;
    }

    private DataBindingProfile loadDataBindingProfile(String fullClzName) {
        final DataBindingProfile profile;
        try {
            profile = (DataBindingProfile)ClassLoaderUtils.loadClass(fullClzName,
                                                                     getClass()).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Message msg = new Message("DATABINDING_PROFILE_LOAD_FAIL", LOG, fullClzName);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg);
        }
        return profile;
    }

    public DataBindingProfile getDataBindingProfile(String name) {
        DataBinding databinding = getDataBinding(name);
        return loadDataBindingProfile(databinding.getPackage() + '.' + databinding.getProfile());
    }

    public Map<String, FrontEnd> getFrontEnds() {
        return this.frontends;
    }

    public Map<String, DataBinding> getDataBindings() {
        return this.databindings;
    }

    public Map<String, Plugin> getPlugins() {
        return this.plugins;
    }
}
