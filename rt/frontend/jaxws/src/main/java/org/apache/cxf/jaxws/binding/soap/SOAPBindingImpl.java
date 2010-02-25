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

package org.apache.cxf.jaxws.binding.soap;

import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxws.binding.AbstractBindingImpl;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingInfo;

public class SOAPBindingImpl extends AbstractBindingImpl implements SOAPBinding {
    
    private static final Logger LOG = LogUtils.getL7dLogger(SOAPBindingImpl.class);
    private static final ResourceBundle BUNDLE = LOG.getResourceBundle();
    
    private BindingInfo soapBinding;
    private Set<String> roles;

    public SOAPBindingImpl(BindingInfo sb, JaxWsEndpointImpl endpoint) {
        super(endpoint);
        soapBinding = sb;  
        addRequiredRoles();
    }
    
    private void addRequiredRoles() {
        if (this.roles == null) {
            this.roles = new HashSet<String>();
        }
        if (this.soapBinding instanceof SoapBindingInfo) {
            SoapBindingInfo bindingInfo = (SoapBindingInfo) this.soapBinding;
            if (bindingInfo.getSoapVersion() instanceof Soap11) {
                this.roles.add(bindingInfo.getSoapVersion().getNextRole());
            } else if (bindingInfo.getSoapVersion() instanceof Soap12) {
                this.roles.add(bindingInfo.getSoapVersion().getNextRole());
                this.roles.add(bindingInfo.getSoapVersion().getUltimateReceiverRole());
            }
        }
    }

    public Set<String> getRoles() {
        return this.roles;
    }

    public void setRoles(Set<String> set) {
        if (set != null 
            && (set.contains(Soap11.getInstance().getNoneRole()) 
                || set.contains(Soap12.getInstance().getNoneRole()))) {
            throw new WebServiceException(BUNDLE.getString("NONE_ROLE_ERR"));
        }
        this.roles = set;
        addRequiredRoles();
    }

    public boolean isMTOMEnabled() {
        return Boolean.TRUE.equals(soapBinding.getProperty(Message.MTOM_ENABLED));
    }

    public void setMTOMEnabled(boolean flag) {        
        soapBinding.setProperty(Message.MTOM_ENABLED, flag);
    }

    public MessageFactory getMessageFactory() {
        if (this.soapBinding instanceof SoapBindingInfo) {
            SoapBindingInfo bindingInfo = (SoapBindingInfo) this.soapBinding;
            try {
                if (bindingInfo.getSoapVersion() instanceof Soap11) {
                    return MessageFactory.newInstance();
                } else if (bindingInfo.getSoapVersion() instanceof Soap12) {
                    return MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
                }
            } catch (SOAPException e) {
                throw new WebServiceException(BUNDLE.getString("SAAJ_FACTORY_ERR"), e);
            }
        }
        return null;
    }

    public SOAPFactory getSOAPFactory() {
        if (this.soapBinding instanceof SoapBindingInfo) {
            SoapBindingInfo bindingInfo = (SoapBindingInfo) this.soapBinding;
            try {
                if (bindingInfo.getSoapVersion() instanceof Soap11) {
                    return SOAPFactory.newInstance();
                } else if (bindingInfo.getSoapVersion() instanceof Soap12) {
                    return SOAPFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
                }
            } catch (SOAPException e) {
                throw new WebServiceException(BUNDLE.getString("SAAJ_FACTORY_ERR"), e);
            }
        }
        return null;
    }

    public static boolean isSoapBinding(final String bindingID) {
        return bindingID.equals(SoapBindingConstants.SOAP11_BINDING_ID)
            || bindingID.equals(SoapBindingConstants.SOAP12_BINDING_ID)
            || bindingID.equals(SOAPBinding.SOAP11HTTP_BINDING)
            || bindingID.equals(SOAPBinding.SOAP11HTTP_MTOM_BINDING)
            || bindingID.equals(SOAPBinding.SOAP12HTTP_BINDING)
            || bindingID.equals(SOAPBinding.SOAP12HTTP_MTOM_BINDING);
    }

    public String getBindingID() {
        if (this.soapBinding instanceof SoapBindingInfo) {
            SoapBindingInfo bindingInfo = (SoapBindingInfo) this.soapBinding;
            if (bindingInfo.getSoapVersion() instanceof Soap12) {
                return SOAP12HTTP_BINDING;             
            }
        }
        return SOAP11HTTP_BINDING;
    }
}
