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

package org.apache.cxf.jaxws;

import java.io.File;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.frontend.AbstractServiceFactory;
import org.apache.cxf.jaxws.binding.soap.JaxWsSoapBindingConfiguration;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;

public class JaxwsServiceBuilder extends AbstractServiceFactory {

    final JaxWsServiceFactoryBean serviceFactory;
    
    public JaxwsServiceBuilder() {
        super();
        serviceFactory = new JaxWsServiceFactoryBean();
        //As this is a javatowsdl tool, explictly populate service model from class
        serviceFactory.setPopulateFromClass(true);
        
        setServiceFactory(serviceFactory);
        setBindingConfig(new JaxWsSoapBindingConfiguration(serviceFactory));
    }

    @Override
    public void validate() {
        Class clz = getServiceClass();
        if (java.rmi.Remote.class.isAssignableFrom(clz)) {
            throw new RuntimeException("JAXWS SEIs may not implement the java.rmi.Remote interface.");
        }
    }
    
    public File getOutputFile() {
        JaxWsImplementorInfo jaxwsImpl = serviceFactory.getJaxWsImplementorInfo();
        String wsdlLocation = jaxwsImpl.getWsdlLocation();
        if (!StringUtils.isEmpty(wsdlLocation)) {
            return new File(wsdlLocation);
        }
        return super.getOutputFile();
    }
    
   
    
}
