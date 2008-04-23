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

import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.frontend.AbstractServiceFactory;
import org.apache.cxf.service.ServiceBuilder;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.java2wsdl.processor.FrontendFactory;
import org.apache.cxf.tools.util.NameUtil;

import org.springframework.context.ApplicationContext;
/**
 * This class constructs ServiceBuilder objects. These objects are used to access the services
 * and the data bindings to generate the wsdl.
 */
public final class ServiceBuilderFactory {
    private static ServiceBuilderFactory instance;
    private static FrontendFactory frontend;
    private static String databindingName;
    private Class serviceClass;
    
    private ServiceBuilderFactory() {
        frontend = FrontendFactory.getInstance();
        databindingName = ToolConstants.DEFAULT_DATA_BINDING_NAME;
    }
    
    public static ServiceBuilderFactory getInstance() {
        if (instance == null) {
            instance = new ServiceBuilderFactory();
        }
        return instance;
    }

    public ServiceBuilder newBuilder(ApplicationContext applicationContext) {
        return newBuilder(applicationContext, getStyle());
    }
    
    /**
     * Convert a parameter value to the name of a bean we'd use for a data binding.
     * @param databindingName
     * @return
     */
    public static String databindingNameToBeanName(String dbName) {
        return NameUtil.capitalize(dbName.toLowerCase()) + ToolConstants.DATABIND_BEAN_NAME_SUFFIX;
    }

    public ServiceBuilder newBuilder(ApplicationContext applicationContext, FrontendFactory.Style s) {
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
            builder = (ServiceBuilder) applicationContext.getBean(beanName, ServiceBuilder.class);
            AbstractServiceFactory serviceFactory = (AbstractServiceFactory)builder;
            serviceFactory.setDataBinding(dataBinding);
        } catch (RuntimeException e) {
            throw new ToolException("Can not get ServiceBuilder bean " + beanName 
                                    + "to initialize the ServiceBuilder for style: " + s
                                    + " Reason: \n" + e.getMessage(),
                                    e);
        }
        builder.setServiceClass(serviceClass);
        return builder;
    }

    /**
     * Return the name of a prototype bean from Spring that can provide the service. The use of a bean
     * allows for the possibility of an override.
     * @param s Style of service
     * @return name of bean.
     */
    protected String getBuilderBeanName(FrontendFactory.Style s) {
        return s + "ServiceBuilderBean";
    }

    public FrontendFactory.Style getStyle() {
        frontend.setServiceClass(this.serviceClass);
        return frontend.discoverStyle();
    }

    public void setServiceClass(Class c) {
        this.serviceClass = c;
    }

    /**
     * Return the databinding name.
     * @return
     */
    public String getDatabindingName() {
        return databindingName;
    }

    /**
     * Set the databinding name
     * @param databindingName
     */
    public void setDatabindingName(String arg) {
        databindingName = arg;
    }
}
