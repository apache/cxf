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

package org.apache.cxf.bus.blueprint;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.aries.blueprint.services.ExtendedBlueprintContainer;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.Configurer;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.NoSuchComponentException;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;

/**
 *
 */
public class ConfigurerImpl implements Configurer {
    private static final Logger LOG = LogUtils.getL7dLogger(ConfigurerImpl.class);
    BlueprintContainer container;

    private final Map<String, Collection<PatternHolder>> wildCardBeanDefinitions = new HashMap<>();

    static class PatternHolder {
        final String wildCardId;
        final Pattern pattern;
        PatternHolder(String orig, Pattern pattern) {
            wildCardId = orig;
            this.pattern = pattern;
        }
    }

    public ConfigurerImpl(BlueprintContainer con) {
        container = con;
        initializeWildcardMap();
    }
    private static boolean isWildcardBeanName(String bn) {
        return bn.indexOf('*') != -1 || bn.indexOf('?') != -1
            || (bn.indexOf('(') != -1 && bn.indexOf(')') != -1);
    }

    private void initializeWildcardMap() {
        for (String s : container.getComponentIds()) {
            if (isWildcardBeanName(s)) {
                ComponentMetadata cmd = container.getComponentMetadata(s);
                Class<?> cls = BlueprintBeanLocator.getClassForMetaData(container, cmd);
                if (cls != null) {
                    final String cid = s.charAt(0) != '*' ? s : "." + s; //old wildcard
                    wildCardBeanDefinitions.computeIfAbsent(cls.getName(), c -> new ArrayList<>())
                        .add(new PatternHolder(s, Pattern.compile(cid)));
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
        if (null == bn) {
            bn = getBeanName(beanInstance);
        }

        if (null == bn) {
            return;
        }
        if (checkWildcards) {
            configureWithWildCard(bn, beanInstance);
        }

        if (container instanceof ExtendedBlueprintContainer) {
            ComponentMetadata cm = null;
            try {
                cm = container.getComponentMetadata(bn);
            } catch (NoSuchComponentException nsce) {
                cm = null;
            }
            if (cm instanceof BeanMetadata) {
                ((ExtendedBlueprintContainer)container).injectBeanInstance((BeanMetadata)cm, beanInstance);
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

}
