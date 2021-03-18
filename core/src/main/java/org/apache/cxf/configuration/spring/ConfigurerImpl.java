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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.extension.BusExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.wiring.BeanConfigurerSupport;
import org.springframework.beans.factory.wiring.BeanWiringInfo;
import org.springframework.beans.factory.wiring.BeanWiringInfoResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

@NoJSR250Annotations
public class ConfigurerImpl extends BeanConfigurerSupport
    implements Configurer, ApplicationContextAware, BusExtension {

    private static final Logger LOG = LogUtils.getL7dLogger(ConfigurerImpl.class);

    private Set<ApplicationContext> appContexts;
    private final Map<String, Collection<PatternHolder>> wildCardBeanDefinitions = new HashMap<>();
    private BeanFactory beanFactory;

    static class PatternHolder {
        final String wildCardId;
        final Pattern pattern;
        PatternHolder(String orig, Pattern pattern) {
            wildCardId = orig;
            this.pattern = pattern;
        }
    }

    public ConfigurerImpl() {
        // complete
    }

    public ConfigurerImpl(ApplicationContext ac) {
        setApplicationContext(ac);
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        super.setBeanFactory(beanFactory);
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
                            final String name = n.charAt(0) != '*' ? n : "." + n; // old wildcard
                            try {
                                wildCardBeanDefinitions.computeIfAbsent(className, c -> new ArrayList<>())
                                    .add(new PatternHolder(n, Pattern.compile(name)));
                            } catch (PatternSyntaxException npe) {
                                //not a valid patter, we'll ignore
                            }
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
            if (null == bn) {
                return;
            }
        }

        if (checkWildcards) {
            configureWithWildCard(bn, beanInstance);
        }

        final String beanName = bn;
        setBeanWiringInfoResolver(new BeanWiringInfoResolver() {
            public BeanWiringInfo resolveWiringInfo(Object instance) {
                if (!beanName.isEmpty()) {
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
            //this will prevent a call into the AbstractBeanFactory.markBeanAsCreated(...)
            //which saves ALL the names into a HashSet.  For URL based configuration,
            //this can leak memory
            if (beanFactory instanceof AbstractBeanFactory) {
                ((AbstractBeanFactory)beanFactory).getMergedBeanDefinition(bn);
            }
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
            Class<?> clazz = beanInstance.getClass();
            while (!Object.class.equals(clazz)) {
                String className = clazz.getName();
                Collection<PatternHolder> patterns = wildCardBeanDefinitions.get(className);
                if (patterns != null) {
                    for (PatternHolder p : patterns) {
                        if (p.pattern.matcher(bn).matches()) {
                            configureBean(p.wildCardId, beanInstance, false);
                            return;
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        }
    }

    private static boolean isWildcardBeanName(String bn) {
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
            LogUtils.log(LOG, Level.FINE, "COULD_NOT_DETERMINE_BEAN_NAME_MSG",
                         beanInstance.getClass().getName());
        }

        return beanName;
    }

    public final void setApplicationContext(ApplicationContext ac) {
        appContexts = new CopyOnWriteArraySet<>();
        addApplicationContext(ac);
        this.beanFactory = ac.getAutowireCapableBeanFactory();
        super.setBeanFactory(this.beanFactory);
    }

    public final void addApplicationContext(ApplicationContext ac) {
        if (!appContexts.contains(ac)) {
            appContexts.add(ac);
            Collection<ApplicationContext> inactiveApplicationContexts = new ArrayList<>(appContexts.size());
            Iterator<ApplicationContext> it = appContexts.iterator();
            while (it.hasNext()) {
                ApplicationContext c = it.next();
                if (c instanceof ConfigurableApplicationContext
                    && !((ConfigurableApplicationContext)c).isActive()) {
                    inactiveApplicationContexts.add(c);
                }
            }
            // Remove the inactive application context here can avoid the UnsupportedOperationException
            for (ApplicationContext context : inactiveApplicationContexts) {
                appContexts.remove(context);
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

    protected Set<ApplicationContext> getAppContexts() {
        return appContexts;
    }

}
