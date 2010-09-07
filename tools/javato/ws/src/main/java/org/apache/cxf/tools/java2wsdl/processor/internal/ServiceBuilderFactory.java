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

import java.util.List;

import org.apache.cxf.service.ServiceBuilder;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.java2wsdl.processor.FrontendFactory;

/**
 * This class constructs ServiceBuilder objects. These objects are used to access the services and the data
 * bindings to generate the wsdl.
 */
public abstract class ServiceBuilderFactory {
    protected FrontendFactory frontend;
    protected String databindingName;
    protected Class<?> serviceClass;

    protected ServiceBuilderFactory() {
        frontend = FrontendFactory.getInstance();
        databindingName = ToolConstants.DEFAULT_DATA_BINDING_NAME;
    }

    public static ServiceBuilderFactory getInstance(List<String> beanDefinitions,
                                                    String db) {
        ServiceBuilderFactory factory;
        if (beanDefinitions == null || beanDefinitions.isEmpty()) {
            if (ToolConstants.JAXB_DATABINDING.equals(db)
                || ToolConstants.AEGIS_DATABINDING.equals(db)) {
                factory = new DefaultServiceBuilderFactory();
            } else {
                factory = new SpringServiceBuilderFactory(beanDefinitions);
            }
        } else {
            factory = new SpringServiceBuilderFactory(beanDefinitions);
        }
        return factory;
    }

    public ServiceBuilder newBuilder() {
        return newBuilder(getStyle());
    }

    public abstract ServiceBuilder newBuilder(FrontendFactory.Style s);

    public FrontendFactory.Style getStyle() {
        frontend.setServiceClass(this.serviceClass);
        return frontend.discoverStyle();
    }

    public void setServiceClass(Class<?> c) {
        this.serviceClass = c;
    }

    /**
     * Return the databinding name.
     * 
     * @return
     */
    public String getDatabindingName() {
        return databindingName;
    }

    /**
     * Set the databinding name
     * 
     * @param databindingName
     */
    public void setDatabindingName(String arg) {
        databindingName = arg;
    }
}
