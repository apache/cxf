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
package org.apache.cxf.frontend;

import java.io.File;

import org.apache.cxf.service.ServiceBuilder;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.model.ServiceInfo;

public abstract class AbstractServiceFactory extends AbstractWSDLBasedEndpointFactory implements
    ServiceBuilder {
    
    protected AbstractServiceFactory() {
        super();
    }
    protected AbstractServiceFactory(ReflectionServiceFactoryBean sbean) {
        super(sbean);
    }
    
    public ServiceInfo createService() {
        try {
            return createEndpoint().getEndpointInfo().getService();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public File getOutputFile() {
        return null;
    }

    public void setServiceClass(Class clz) {
        super.setServiceClass(clz);
        getServiceFactory().setServiceClass(clz);
    }

    public void validate() {
        // nothing to validate here
    }
}
