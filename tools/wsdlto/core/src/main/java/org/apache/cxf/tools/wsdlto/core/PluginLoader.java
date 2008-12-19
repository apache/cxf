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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.tools.common.FrontEndGenerator;
import org.apache.cxf.tools.common.Processor;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolContainer;
import org.apache.cxf.tools.plugin.DataBinding;
import org.apache.cxf.tools.plugin.FrontEnd;
import org.apache.cxf.tools.plugin.Generator;
import org.apache.cxf.tools.plugin.Plugin;

public final class PluginLoader {
    public static final Logger LOG = LogUtils.getL7dLogger(PluginLoader.class);
    public static final String DEFAULT_PROVIDER_NAME = "cxf.apache.org";
    private static PluginLoader pluginLoader;
    private static final String PLUGIN_FILE_NAME = "META-INF/tools-plugin.xml";

    private Map<String, Plugin> plugins = new LinkedHashMap<String, Plugin>();

    private Map<String, FrontEnd> frontends = new LinkedHashMap<String, FrontEnd>();

    private Map<String, DataBinding> databindings = new TreeMap<String, DataBinding>();

    private Unmarshaller unmarshaller;

    private ClassLoader classLoader = getClass().getClassLoader();

    private PluginLoader() {
        init();
    }

    private PluginLoader(final ClassLoader l) {
        this.classLoader = l;
        init();
    }

    private void init() {
        try {
            JAXBContext jc = JAXBContext.newInstance("org.apache.cxf.tools.plugin");
            unmarshaller = jc.createUnmarshaller();
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

    public void setClassLoader(final ClassLoader l) {
        this.classLoader = l;
    }

    public ClassLoader getClassLoader() {
        return this.classLoader;
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

    public static PluginLoader getInstance() {
        if (pluginLoader == null) {
            pluginLoader = new PluginLoader();
        }
        return pluginLoader;
    }

    public static PluginLoader getInstance(final ClassLoader cl) {
        if (pluginLoader == null) {
            pluginLoader = new PluginLoader(cl);
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
        } catch (JAXBException e) {
            Message msg = new Message("PLUGIN_LOAD_FAIL", LOG, resource);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg, e);
        } catch (FileNotFoundException fe) {
            Message msg = new Message("PLUGIN_FILE_NOT_FOUND", LOG, resource);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg, fe);
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
        InputStream is = null;
        if (plugin == null) {
            is = url.openStream();
            plugin = getPlugin(is);
            if (plugin == null || StringUtils.isEmpty(plugin.getName())) {
                Message msg = new Message("PLUGIN_LOAD_FAIL", LOG, url);
                LOG.log(Level.SEVERE, msg.toString());
                throw new ToolException(msg);
            }
            plugins.put(url.toString(), plugin);
        }
        if (is == null) {
            return getPlugin(url.toString());
        }
        return plugin;
    }

    protected Plugin getPlugin(String resource) throws JAXBException, FileNotFoundException {
        Plugin plugin = plugins.get(resource);
        if (plugin == null) {
            InputStream is = null;
            if (new File(resource).exists()) {
                is = new BufferedInputStream(new FileInputStream(new File(resource)));
            } else {
                is = getClass().getResourceAsStream(resource);
            }

            if (is == null) {
                Message msg = new Message("PLUGIN_MISSING", LOG, resource);
                LOG.log(Level.SEVERE, msg.toString());
                throw new ToolException(msg);
            }
            plugin = getPlugin(is);
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
        return (Plugin) ((JAXBElement<?>)unmarshaller.unmarshal(is)).getValue();
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
        return fullPackage + "." + generator.getName();
    }

    private List<FrontEndGenerator> getFrontEndGenerators(FrontEnd frontend) {
        List<FrontEndGenerator> generators = new ArrayList<FrontEndGenerator>();

        String fullClzName = null;
        try {
            for (Generator generator : frontend.getGenerators().getGenerator()) {
                fullClzName = getGeneratorClass(frontend, generator);
                Class clz = this.classLoader.loadClass(fullClzName);
                generators.add((FrontEndGenerator)clz.newInstance());
            }
        } catch (Exception e) {
            Message msg = new Message("FRONTEND_PROFILE_LOAD_FAIL", LOG, fullClzName);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg, e);
        }

        return generators;
    }

    private FrontEndProfile loadFrontEndProfile(String fullClzName) {
        FrontEndProfile profile = null;
        try {
            Class clz = this.classLoader.loadClass(fullClzName);
            profile = (FrontEndProfile)clz.newInstance();
        } catch (Exception e) {
            Message msg = new Message("FRONTEND_PROFILE_LOAD_FAIL", LOG, fullClzName);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg, e);
        }
        return profile;
    }

    private Processor loadProcessor(String fullClzName) {
        Processor processor = null;
        try {
            processor = (Processor) ClassLoaderUtils.loadClass(fullClzName, getClass()).newInstance();
        } catch (Exception e) {
            Message msg = new Message("LOAD_PROCESSOR_FAILED", LOG, fullClzName);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg, e);
        }
        return processor;
    }

    private Class<? extends ToolContainer> loadContainerClass(String fullClzName) {
        Class<?> clz = null;
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
        return frontend.getPackage() + "." + frontend.getProfile();
    }

    private String getProcessorClass(FrontEnd frontend) {
        String pkgName = frontend.getProcessor().getPackage();
        if (StringUtils.isEmpty(pkgName)) {
            pkgName = frontend.getPackage();
        }
        return pkgName + "." + frontend.getProcessor().getName();
    }

    private String getContainerClass(FrontEnd frontend) {
        return getContainerPackage(frontend) + "." + frontend.getContainer().getName();
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
        return "/" + getContainerPackage(frontend).replace(".", "/") + "/" + toolspec;
    }

    @SuppressWarnings("unchecked")
    private AbstractWSDLBuilder<? extends Object> loadBuilder(String fullClzName) {
        AbstractWSDLBuilder<? extends Object> builder = null;
        try {
            builder = (AbstractWSDLBuilder<? extends Object>) ClassLoaderUtils
                .loadClass(fullClzName, getClass()).newInstance();

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
        return pkgName + "." + frontend.getBuilder().getName();
    }

    public FrontEndProfile getFrontEndProfile(String name) {
        FrontEndProfile profile = null;
        FrontEnd frontend = getFrontEnd(name);

        profile = loadFrontEndProfile(getFrontEndProfileClass(frontend));

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
        DataBindingProfile profile = null;
        try {
            profile = (DataBindingProfile)ClassLoaderUtils.loadClass(fullClzName,
                                                                     getClass()).newInstance();
        } catch (Exception e) {
            Message msg = new Message("DATABINDING_PROFILE_LOAD_FAIL", LOG, fullClzName);
            LOG.log(Level.SEVERE, msg.toString());
            throw new ToolException(msg);
        }
        return profile;
    }

    public DataBindingProfile getDataBindingProfile(String name) {
        DataBindingProfile profile = null;
        DataBinding databinding = getDataBinding(name);
        profile = loadDataBindingProfile(databinding.getPackage() + "." + databinding.getProfile());
        return profile;
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
