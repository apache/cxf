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



import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.jaxws.binding.soap.JaxWsSoapBindingConfiguration;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;

/**
 * Bean to help easily create Client endpoints for JAX-WS.
 */
public class JaxWsClientFactoryBean extends ClientFactoryBean {


    public JaxWsClientFactoryBean() {
        super(new JaxWsServiceFactoryBean());
    }
    public void setServiceClass(Class<?> serviceClass) {
        super.setServiceClass(serviceClass);
        if (((JaxWsServiceFactoryBean)getServiceFactory()).getJaxWsImplementorInfo() == null) {
            JaxWsImplementorInfo implInfo = new JaxWsImplementorInfo(serviceClass);
            ((JaxWsServiceFactoryBean)getServiceFactory()).setJaxWsImplementorInfo(implInfo);
        }
    }

    protected SoapBindingConfiguration createSoapBindingConfig() {
        JaxWsSoapBindingConfiguration bc
            = new JaxWsSoapBindingConfiguration((JaxWsServiceFactoryBean)getServiceFactory());
        if (transportId != null) {
            bc.setTransportURI(transportId);
        }
        return bc;
    }

    @Override
    public void setBindingId(String bind) {
        if (SOAPBinding.SOAP11HTTP_BINDING.equals(bind)
            || SOAPBinding.SOAP11HTTP_MTOM_BINDING.equals(bind)) {
            super.setBindingId("http://schemas.xmlsoap.org/wsdl/soap/");
        } else if (SOAPBinding.SOAP12HTTP_BINDING.equals(bind)
            || SOAPBinding.SOAP12HTTP_MTOM_BINDING.equals(bind)) {
            super.setBindingId("http://schemas.xmlsoap.org/wsdl/soap12/");
        } else {
            super.setBindingId(bind);
        }

        if (SOAPBinding.SOAP11HTTP_BINDING.equals(bind)
            || SOAPBinding.SOAP12HTTP_BINDING.equals(bind)) {
            setBindingConfig(new JaxWsSoapBindingConfiguration((JaxWsServiceFactoryBean)getServiceFactory()));
        } else if (SOAPBinding.SOAP11HTTP_MTOM_BINDING.equals(bind)
            || SOAPBinding.SOAP12HTTP_MTOM_BINDING.equals(bind)) {
            setBindingConfig(new JaxWsSoapBindingConfiguration((JaxWsServiceFactoryBean)getServiceFactory()));
            ((JaxWsSoapBindingConfiguration)getBindingConfig()).setMtomEnabled(true);
        }
    }

}
