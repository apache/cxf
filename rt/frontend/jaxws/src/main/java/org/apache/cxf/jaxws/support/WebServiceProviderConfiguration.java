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

package org.apache.cxf.jaxws.support;

import java.lang.reflect.Method;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import jakarta.activation.DataSource;
import jakarta.xml.soap.SOAPMessage;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;

public class WebServiceProviderConfiguration extends JaxWsServiceConfiguration {

    private JaxWsImplementorInfo implInfo;

    @Override
    public Boolean isOperation(Method method) {
        return "invoke".equals(method.getName())
            && method.getParameterTypes().length == 1
            && (Source.class.isAssignableFrom(method.getParameterTypes()[0])
                || SOAPMessage.class.isAssignableFrom(method.getParameterTypes()[0])
                || DataSource.class.isAssignableFrom(method.getParameterTypes()[0]));
    }


    @Override
    public void setServiceFactory(ReflectionServiceFactoryBean serviceFactory) {
        super.setServiceFactory(serviceFactory);
        implInfo = ((JaxWsServiceFactoryBean) serviceFactory).getJaxWsImplementorInfo();
    }


    @Override
    public String getServiceName() {
        QName service = implInfo.getServiceName();
        return service.getLocalPart();
    }

    @Override
    public String getServiceNamespace() {
        QName service = implInfo.getServiceName();
        return service.getNamespaceURI();
    }

    @Override
    public QName getEndpointName() {
        return implInfo.getEndpointName();
    }

    @Override
    public String getWsdlURL() {
        String wsdlLocation = implInfo.getWsdlLocation();
        if (wsdlLocation != null && wsdlLocation.length() > 0) {
            return wsdlLocation;
        }
        return null;
    }

    @Override
    public Boolean isWrapped(Method m) {
        return Boolean.FALSE;
    }
}



