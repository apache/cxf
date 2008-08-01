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
package org.apache.cxf.aegis.databinding;

import java.lang.reflect.Method;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.type.TypeCreator;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.service.factory.AbstractServiceConfiguration;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.model.OperationInfo;

/**
 * Configure the services based on information in a .aegis.xml file
 * for the SEI or SEB.
 */
public class AegisXmlServiceConfiguration extends AbstractServiceConfiguration {
    
    private AegisContext context;
    private TypeCreator creator;
    @SuppressWarnings("unused")
    private Class<?> serviceClass;
    
    private void getTypeCreator() {
        if (creator != null) {
            return;
        }
        DataBinding db = getServiceFactory().getDataBinding();
        if (db instanceof AegisDatabinding) {
            context = ((AegisDatabinding) db).getAegisContext();
            serviceClass = getServiceFactory().getServiceClass();
            creator = context.getTypeMapping().getTypeCreator();
        }
    }

    @Override
    public void setServiceFactory(ReflectionServiceFactoryBean serviceFactory) {
        super.setServiceFactory(serviceFactory);
    }

    @Override
    public QName getInParameterName(OperationInfo op, Method method, int paramNumber) {
        getTypeCreator();
        if (creator != null) {
            QName paramName = creator.getElementName(method, paramNumber);
            if (paramName != null) {
                return paramName;
            }
        }

        return super.getInParameterName(op, method, paramNumber);
    }


}