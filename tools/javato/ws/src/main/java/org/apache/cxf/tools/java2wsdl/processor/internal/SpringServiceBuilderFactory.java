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

package org.apache.cxf.tools.java2wsdl.processor.internal;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.BusApplicationContext;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.frontend.AbstractServiceFactory;
import org.apache.cxf.service.ServiceBuilder;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.java2wsdl.processor.FrontendFactory;
import org.apache.cxf.tools.util.NameUtil;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;

/**
 * This class constructs ServiceBuilder objects using Spring. These objects are used to access the services
 * and the data bindings to generate the wsdl.
 */
public final class SpringServiceBuilderFactory extends ServiceBuilderFactory {

    private List<String> beanDefinitions;

    public SpringServiceBuilderFactory(List<String> beanDefinitions) {
        super();
        this.beanDefinitions = beanDefinitions;
    }

    public SpringServiceBuilderFactory() {
        super();
        this.beanDefinitions = new ArrayList<String>(0);
    }

    /**
     * Convert a parameter value to the name of a bean we'd use for a data binding.
     * 
     * @param databindingName
     * @return
     */
    public static String databindingNameToBeanName(String dbName) {
        return NameUtil.capitalize(dbName.toLowerCase()) + ToolConstants.DATABIND_BEAN_NAME_SUFFIX;
    }

    @Override
    public ServiceBuilder newBuilder(FrontendFactory.Style s) {
        ApplicationContext applicationContext = getApplicationContext(beanDefinitions);
        DataBinding dataBinding;
        String databindingBeanName = databindingNameToBeanName(databindingName);
        try {
            dataBinding = (DataBinding)applicationContext.getBean(databindingBeanName);
        } catch (RuntimeException e) {
            throw new ToolException("Cannot get databinding bean " + databindingBeanName
                                    + " for databinding " + databindingName);
        }

        String beanName = getBuilderBeanName(s);
        ServiceBuilder builder = null;

        try {
            builder = (ServiceBuilder)applicationContext.getBean(beanName, ServiceBuilder.class);
            AbstractServiceFactory serviceFactory = (AbstractServiceFactory)builder;
            serviceFactory.setDataBinding(dataBinding);
        } catch (RuntimeException e) {
            throw new ToolException("Can not get ServiceBuilder bean " + beanName
                                    + "to initialize the ServiceBuilder for style: " + s + " Reason: \n"
                                    + e.getMessage(), e);
        }
        builder.setServiceClass(serviceClass);
        return builder;
    }

    /**
     * Return the name of a prototype bean from Spring that can provide the service. The use of a bean allows
     * for the possibility of an override.
     * 
     * @param s Style of service
     * @return name of bean.
     */
    protected String getBuilderBeanName(FrontendFactory.Style s) {
        return s + "ServiceBuilderBean";
    }

    /**
     * This is factored out to permit use in a unit test.
     * 
     * @param bus
     * @return
     */
    public static ApplicationContext getApplicationContext(List<String> additionalFilePathnames) {
        BusApplicationContext busApplicationContext = BusFactory.getDefaultBus()
            .getExtension(BusApplicationContext.class);
        GenericApplicationContext appContext = new GenericApplicationContext(busApplicationContext);
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
        List<URL> urls = ClassLoaderUtils.getResources("META-INF/cxf/java2wsbeans.xml", 
                                                       SpringServiceBuilderFactory.class);
        for (URL url : urls) {
            reader.loadBeanDefinitions(new UrlResource(url));
        }
        
        for (String pathname : additionalFilePathnames) {
            try {
                reader.loadBeanDefinitions(new FileSystemResource(pathname));
            } catch (BeanDefinitionStoreException bdse) {
                throw new ToolException("Unable to open bean definition file " + pathname, bdse.getCause());
            }
        }

        return appContext;
    }

    public void setBeanDefinitions(List<String> beanDefinitions) {
        this.beanDefinitions = beanDefinitions;
    }

}
