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

package org.apache.cxf.configuration.spring;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.extension.BusExtension;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.wiring.BeanConfigurerSupport;
import org.springframework.beans.factory.wiring.BeanWiringInfo;
import org.springframework.beans.factory.wiring.BeanWiringInfoResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

public class ConfigurerImpl extends BeanConfigurerSupport 
    implements Configurer, ApplicationContextAware, BusExtension {
    
    private static final Logger LOG = LogUtils.getL7dLogger(ConfigurerImpl.class);

    private Set<ApplicationContext> appContexts;
    private final Map<String, List<MatcherHolder>> wildCardBeanDefinitions
        = new HashMap<String, List<MatcherHolder>>();
    
    static class MatcherHolder {
        Matcher matcher;
        String wildCardId;
        public MatcherHolder(String orig, Matcher matcher) {
            wildCardId = orig;
            this.matcher = matcher;
        }
    }
    
    public ConfigurerImpl() {
        // complete
    }
    
    public ConfigurerImpl(ApplicationContext ac) {
        setApplicationContext(ac);
    }
        
    private void initWildcardDefinitionMap() {
        if (null != appContexts) {
            for (ApplicationContext appContext : appContexts) {
                for (String n : appContext.getBeanDefinitionNames()) {
                    if (isWildcardBeanName(n)) {
                        AutowireCapableBeanFactory bf = appContext.getAutowireCapableBeanFactory();
                        BeanDefinitionRegistry bdr = (BeanDefinitionRegistry) bf;
                        BeanDefinition bd = bdr.getBeanDefinition(n);
                        String className = bd.getBeanClassName();
                        if (null != className) {
                            String orig = n;
                            if (n.charAt(0) == '*') {
                                //old wildcard
                                n = "." + n.replaceAll("\\.", "\\."); 
                            }
                            Matcher matcher = Pattern.compile(n).matcher("");
                            List<MatcherHolder> m = wildCardBeanDefinitions.get(className);
                            if (m == null) {
                                m = new ArrayList<MatcherHolder>();
                                wildCardBeanDefinitions.put(className, m);
                            }
                            MatcherHolder holder = new MatcherHolder(orig, matcher);
                            m.add(holder);
                        } else {
                            LogUtils.log(LOG, Level.WARNING, "WILDCARD_BEAN_ID_WITH_NO_CLASS_MSG", n); 
                        }
                    }
                }
            }
        }
    }

    public void configureBean(Object beanInstance) {
        configureBean(null, beanInstance, true);
    }
    
    public void configureBean(String bn, Object beanInstance) {
        configureBean(bn, beanInstance, true);
    }
    public synchronized void configureBean(String bn, Object beanInstance, boolean checkWildcards) {

        if (null == appContexts) {
            return;
        }
        
        if (null == bn) {
            bn = getBeanName(beanInstance);
        }
        
        if (null == bn) {
            return;
        }
        if (checkWildcards) {
            configureWithWildCard(bn, beanInstance);
        }
        
        final String beanName = bn;
        setBeanWiringInfoResolver(new BeanWiringInfoResolver() {
            public BeanWiringInfo resolveWiringInfo(Object instance) {
                if (null != beanName && !"".equals(beanName)) {
                    return new BeanWiringInfo(beanName);
                }
                return null;
            }
        });
        
        for (ApplicationContext appContext : appContexts) {
            if (appContext.containsBean(bn)) {
                this.setBeanFactory(appContext.getAutowireCapableBeanFactory());
            }
        }
        
        try {
            super.configureBean(beanInstance);
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Successfully performed injection.");
            }
        } catch (NoSuchBeanDefinitionException ex) {
            // users often wonder why the settings in their configuration files seem
            // to have no effect - the most common cause is that they have been using
            // incorrect bean ids
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "NO_MATCHING_BEAN_MSG", beanName);
            }
        }
    }
    
    private void configureWithWildCard(String bn, Object beanInstance) {
        if (!wildCardBeanDefinitions.isEmpty()) {
            String className = beanInstance.getClass().getName();
            List<MatcherHolder> matchers = wildCardBeanDefinitions.get(className);
            if (matchers != null) {
                for (MatcherHolder m : matchers) {
                    synchronized (m.matcher) {
                        m.matcher.reset(bn);
                        if (m.matcher.matches()) {
                            configureBean(m.wildCardId, beanInstance, false);
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean isWildcardBeanName(String bn) {
        return bn.indexOf('*') != -1 || bn.indexOf('?') != -1
            || (bn.indexOf('(') != -1 && bn.indexOf(')') != -1);
    }

    protected String getBeanName(Object beanInstance) {
        if (beanInstance instanceof Configurable) {
            return ((Configurable)beanInstance).getBeanName();
        }
        String beanName = null;
        Method m = null;
        try {
            m = beanInstance.getClass().getDeclaredMethod("getBeanName", (Class[])null);
        } catch (NoSuchMethodException ex) {
            try {
                m = beanInstance.getClass().getMethod("getBeanName", (Class[])null);
            } catch (NoSuchMethodException e) {
                //ignore
            }
        }
        if (m != null) {
            try {
                beanName = (String)(m.invoke(beanInstance));
            } catch (Exception ex) {
                LogUtils.log(LOG, Level.WARNING, "ERROR_DETERMINING_BEAN_NAME_EXC", ex);
            }
        }
        
        if (null == beanName) {
            LogUtils.log(LOG, Level.INFO, "COULD_NOT_DETERMINE_BEAN_NAME_MSG",
                         beanInstance.getClass().getName());
        }
      
        return beanName;
    }
    
    public final void setApplicationContext(ApplicationContext ac) {
        appContexts = new CopyOnWriteArraySet<ApplicationContext>();
        addApplicationContext(ac);
        setBeanFactory(ac.getAutowireCapableBeanFactory());
    }
    
    public final void addApplicationContext(ApplicationContext ac) {
        if (!appContexts.contains(ac)) {
            appContexts.add(ac);
            
            Iterator<ApplicationContext> it = appContexts.iterator();
            while (it.hasNext()) {
                ApplicationContext c = it.next();
                if (c instanceof ConfigurableApplicationContext
                    && !((ConfigurableApplicationContext)c).isActive()) {
                    it.remove();
                }
            }
            
            initWildcardDefinitionMap();
        }
    }
    
    public void destroy() {
        super.destroy();       
        appContexts.clear();
    }

    public Class<?> getRegistrationType() {
        return Configurer.class;
    }
}
