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
import java.net.URI;

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
    
    public File getOutputFile() {
        JaxWsImplementorInfo jaxwsImpl = serviceFactory.getJaxWsImplementorInfo();
        String wsdlLocation = jaxwsImpl.getWsdlLocation();
        if (!StringUtils.isEmpty(wsdlLocation)) {
            try {
                URI uri = new URI(wsdlLocation);
                if ("file".equals(uri.getScheme()) 
                    || StringUtils.isEmpty(uri.getScheme())) {
                    File f = new File(uri);
                    if (f.exists()) {
                        return f;
                    }
                }
            } catch (Exception e) {
                //ignore
            }
            
            File f = new File(wsdlLocation);
            if (f.exists()) {
                return f;
            }
        }
        return super.getOutputFile();
    }
    
   
    
}
