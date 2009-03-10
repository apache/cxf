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

package org.apache.cxf.bus.spring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.SystemUtils;
import org.apache.cxf.configuration.Configurer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.BeansDtdResolver;
import org.springframework.beans.factory.xml.DefaultNamespaceHandlerResolver;
import org.springframework.beans.factory.xml.PluggableSchemaResolver;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class BusApplicationContext extends ClassPathXmlApplicationContext {
    
    private static final String DEFAULT_CXF_CFG_FILE = "META-INF/cxf/cxf.xml";
    private static final String DEFAULT_CXF_EXT_CFG_FILE = "classpath*:META-INF/cxf/cxf.extension";

    private static final Logger LOG = LogUtils.getL7dLogger(BusApplicationContext.class);
    
    private DefaultNamespaceHandlerResolver nsHandlerResolver;
    private boolean includeDefaults;
    private String[] cfgFiles;
    private URL[] cfgFileURLs;
    
    public BusApplicationContext(String cf, boolean include) {
        this(cf, include, null);
    }
    public BusApplicationContext(String[] cfs, boolean include) {
        this(cfs, include, null);
    }
    
    public BusApplicationContext(URL url, boolean include) {
        this(url, include, null);
    }
    public BusApplicationContext(URL[] urls, boolean include) {
        this(urls, include, null);
    }

    public BusApplicationContext(String cf, boolean include, ApplicationContext parent) {
        this(new String[] {cf}, include, parent);
    }
    
    public BusApplicationContext(URL url, boolean include, ApplicationContext parent) {
        this(new URL[] {url}, include, parent);
    } 
    public BusApplicationContext(String[] cf, boolean include, ApplicationContext parent) {
        super(new String[0], false, parent);
        cfgFiles = cf;
        includeDefaults = include;
        refresh();
    }
    
    public BusApplicationContext(URL[] url, boolean include, ApplicationContext parent) {
        super(new String[0], false, parent);
        cfgFileURLs = url;
        includeDefaults = include;
        refresh();
    } 
    
    @Override
    protected Resource[] getConfigResources() {
  
        List<Resource> resources = new ArrayList<Resource>();
       
        if (includeDefaults) {
            try {
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(Thread
                    .currentThread().getContextClassLoader());
                
                Collections.addAll(resources, resolver.getResources(DEFAULT_CXF_CFG_FILE));

                Resource[] exts = resolver.getResources(DEFAULT_CXF_EXT_CFG_FILE);
                for (Resource r : exts) {
                    InputStream is = r.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    String line = rd.readLine();
                    while (line != null) {
                        if (!"".equals(line)) {
                            resources.add(resolver.getResource(line));
                        }
                        line = rd.readLine();
                    }
                    is.close();
                }

            } catch (IOException ex) {
                // ignore  
            }  
        }
        
        boolean usingDefault = false;
        if (null == cfgFiles) {
            String cfgFile = System.getProperty(Configurer.USER_CFG_FILE_PROPERTY_NAME);
            if (cfgFile != null) {
                cfgFiles = new String[] {cfgFile};
            }
        }        
        if (null == cfgFiles) {
            cfgFiles = new String[] {Configurer.DEFAULT_USER_CFG_FILE};
            usingDefault = true;
        }
        for (String cfgFile : cfgFiles) {
            Resource cpr = findResource(cfgFile);
            if (cpr != null && cpr.exists()) {
                resources.add(cpr);
            } else {
                if (!usingDefault) {
                    LogUtils.log(LOG, Level.WARNING, "USER_CFG_FILE_NOT_LOADED", cfgFile);
                    String message = (new Message("USER_CFG_FILE_NOT_LOADED", LOG, cfgFile)).toString();
                    throw new ApplicationContextException(message);
                } else {
                    LogUtils.log(LOG, Level.INFO, "USER_CFG_FILE_NOT_SPECIFIED_MSG", cfgFile);
                }
            }
        }
            
        if (null != cfgFileURLs) {
            for (URL cfgFileURL : cfgFileURLs) {
                UrlResource ur = new UrlResource(cfgFileURL);
                if (ur.exists()) {
                    resources.add(ur);
                } else {
                    LogUtils.log(LOG, Level.WARNING, "USER_CFG_FILE_URL_NOT_FOUND_MSG", cfgFileURL);
                }
            }
        } 
        
        String sysCfgFileUrl = System.getProperty(Configurer.USER_CFG_FILE_PROPERTY_URL);
        if (null != sysCfgFileUrl) {
            try {
                UrlResource ur = new UrlResource(sysCfgFileUrl);
                if (ur.exists()) {
                    resources.add(ur);
                } else {
                    LogUtils.log(LOG, Level.WARNING, "USER_CFG_FILE_URL_NOT_FOUND_MSG", sysCfgFileUrl);
                }            
            } catch (MalformedURLException e) {            
                LogUtils.log(LOG, Level.WARNING, "USER_CFG_FILE_URL_ERROR_MSG", sysCfgFileUrl);
            }
        }
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Creating application context with resources: " + resources);
        }
        
        if (0 == resources.size()) {
            return null;
        }
        Resource[] res = new Resource[resources.size()];
        res = resources.toArray(res);
        return res;
    }
    
    protected Resource findResource(String cfgFile) {
        Resource cpr = new ClassPathResource(cfgFile);
        if (cpr.exists()) {
            return cpr;
        }
        try {
            //see if it's a URL
            URL url = new URL(cfgFile);
            cpr = new UrlResource(url);
            if (cpr.exists()) {
                return cpr;
            }
        } catch (MalformedURLException e) {
            //ignore
        }
        //try loading it our way
        URL url = ClassLoaderUtils.getResource(cfgFile, this.getClass());
        if (url != null) {
            cpr = new UrlResource(url);
            if (cpr.exists()) {
                return cpr;
            }
        }
        cpr = new FileSystemResource(cfgFile);
        if (cpr.exists()) {
            return cpr;
        }
        return null;
    }
    
    @Override
    protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
        // Spring always creates a new one of these, which takes a fair amount
        // of time on startup (nearly 1/2 second) as it gets created for every
        // spring context on the classpath
        if (nsHandlerResolver == null) {
            nsHandlerResolver = new DefaultNamespaceHandlerResolver();
        }
        reader.setNamespaceHandlerResolver(nsHandlerResolver);
        
        String mode = SystemUtils.getSpringValidationMode();
        if (null != mode) {
            reader.setValidationModeName(mode);
        }
        reader.setNamespaceAware(true); 
        
        setEntityResolvers(reader);        
    }
    
    void setEntityResolvers(XmlBeanDefinitionReader reader) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        reader.setEntityResolver(new BusEntityResolver(new BeansDtdResolver(),
            new PluggableSchemaResolver(cl)));
    }
    @Override
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException {
            // Create a new XmlBeanDefinitionReader for the given BeanFactory.
        XmlBeanDefinitionReader beanDefinitionReader = 
            new ControlledValidationXmlBeanDefinitionReader(beanFactory);
        
        // Configure the bean definition reader with this context's
        // resource loading environment.
        beanDefinitionReader.setResourceLoader(this);
        beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

        // Allow a subclass to provide custom initialization of the reader,
        // then proceed with actually loading the bean definitions.
        initBeanDefinitionReader(beanDefinitionReader);
        loadBeanDefinitions(beanDefinitionReader);
    }
    
}
