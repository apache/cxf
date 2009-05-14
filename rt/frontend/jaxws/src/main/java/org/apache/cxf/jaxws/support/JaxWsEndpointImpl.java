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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.xml.ws.Binding;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.binding.xml.XMLBinding;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.binding.DefaultBindingImpl;
import org.apache.cxf.jaxws.binding.http.HTTPBindingImpl;
import org.apache.cxf.jaxws.binding.soap.SOAPBindingImpl;
import org.apache.cxf.jaxws.handler.logical.DispatchLogicalHandlerInterceptor;
import org.apache.cxf.jaxws.handler.logical.LogicalHandlerFaultInInterceptor;
import org.apache.cxf.jaxws.handler.logical.LogicalHandlerFaultOutInterceptor;
import org.apache.cxf.jaxws.handler.logical.LogicalHandlerInInterceptor;
import org.apache.cxf.jaxws.handler.logical.LogicalHandlerOutInterceptor;
import org.apache.cxf.jaxws.handler.soap.DispatchSOAPHandlerInterceptor;
import org.apache.cxf.jaxws.handler.soap.SOAPHandlerFaultInInterceptor;
import org.apache.cxf.jaxws.handler.soap.SOAPHandlerFaultOutInterceptor;
import org.apache.cxf.jaxws.handler.soap.SOAPHandlerInterceptor;
import org.apache.cxf.jaxws.interceptors.HolderInInterceptor;
import org.apache.cxf.jaxws.interceptors.HolderOutInterceptor;
import org.apache.cxf.jaxws.interceptors.SwAInInterceptor;
import org.apache.cxf.jaxws.interceptors.SwAOutInterceptor;
import org.apache.cxf.jaxws.interceptors.WrapperClassInInterceptor;
import org.apache.cxf.jaxws.interceptors.WrapperClassOutInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.wsdl.WSDLManager;

/**
 * A JAX-WS specific implementation of the CXF {@link org.apache.cxf.endpoint.Endpoint} interface.
 * Extends the interceptor provider functionality of its base class by adding 
 * interceptors in which to execute the JAX-WS handlers.
 * Creates and owns an implementation of {@link Binding} in addition to the
 * CXF {@link org.apache.cxf.binding.Binding}. 
 *
 */
public class JaxWsEndpointImpl extends EndpointImpl {

    private Binding jaxwsBinding;
    private JaxWsImplementorInfo implInfo; 
    private List<WebServiceFeature> wsFeatures;
    private List<AbstractFeature> features;
    
    public JaxWsEndpointImpl(Bus bus, Service s, EndpointInfo ei) throws EndpointException {
        this(bus, s, ei, null, null, null, true);
    }
    
    public JaxWsEndpointImpl(Bus bus, Service s, EndpointInfo ei, 
                             List<WebServiceFeature> wf) throws EndpointException {
        this(bus, s, ei, null, wf, new ArrayList<AbstractFeature>(), true);
    }    

    public JaxWsEndpointImpl(Bus bus, Service s, EndpointInfo ei, JaxWsImplementorInfo implementorInfo, 
                             List<WebServiceFeature> wf, List<AbstractFeature> af, boolean isFromWsdl)
        throws EndpointException {
        super(bus, s, ei);
        this.implInfo = implementorInfo;
        this.wsFeatures = wf;
        this.features = af;

        if (features == null) {
            features = new ArrayList<AbstractFeature>();
        }
        createJaxwsBinding();
        
        List<Interceptor> in = super.getInInterceptors();       
        List<Interceptor> out = super.getOutInterceptors();
        
        if (implInfo != null && implInfo.isWebServiceProvider()) {
            DispatchLogicalHandlerInterceptor slhi = new DispatchLogicalHandlerInterceptor(jaxwsBinding,
                Phase.USER_LOGICAL);
            in.add(slhi);
            out.add(new DispatchLogicalHandlerInterceptor(jaxwsBinding));

            if (getBinding() instanceof SoapBinding) {
                in.add(new DispatchSOAPHandlerInterceptor(jaxwsBinding));
                out.add(new DispatchSOAPHandlerInterceptor(jaxwsBinding));
            } 
        } else {
            // Inbound chain
            in.add(new LogicalHandlerInInterceptor(jaxwsBinding));
            in.add(new WrapperClassInInterceptor());
            in.add(new HolderInInterceptor());
            if (getBinding() instanceof SoapBinding) {
                in.add(new SOAPHandlerInterceptor(jaxwsBinding));
                in.add(new SwAInInterceptor());
                getOutInterceptors().add(new SwAOutInterceptor());
            } else {
                // TODO: what for non soap bindings?
            }

            // Outbound chain
            out.add(new LogicalHandlerOutInterceptor(jaxwsBinding));
            out.add(new WrapperClassOutInterceptor());
            out.add(new HolderOutInterceptor());
            if (getBinding() instanceof SoapBinding) {
                out.add(new SOAPHandlerInterceptor(jaxwsBinding));
            }
        }
      
        
        //Outbound fault chain
        List<Interceptor> outFault = super.getOutFaultInterceptors();    
        outFault.add(new LogicalHandlerFaultOutInterceptor(jaxwsBinding));
        if (getBinding() instanceof SoapBinding) {
            outFault.add(new SOAPHandlerFaultOutInterceptor(jaxwsBinding));
        }

        //Inbound fault chain
        List<Interceptor> inFault = super.getInFaultInterceptors(); 
        inFault.add(new LogicalHandlerFaultInInterceptor(jaxwsBinding));
        if (getBinding() instanceof SoapBinding) {
            inFault.add(new SOAPHandlerFaultInInterceptor(jaxwsBinding));
        }
        
        if (ei != null) {
            if (!isFromWsdl) {
                buildWsdlExtensibilities(ei.getBinding());
            }
            extractWsdlExtensibilities(ei);
        }
        resolveFeatures();
    }
    
    private void extractWsdlExtensibilities(EndpointInfo endpoint) {
        List<ExtensibilityElement> bindingExtensors 
            = endpoint.getBinding().getExtensors(ExtensibilityElement.class);
        List<ExtensibilityElement> portExtensors 
            = endpoint.getExtensors(ExtensibilityElement.class);
        if (hasUsingAddressing(bindingExtensors) || hasUsingAddressing(portExtensors)) {
            WSAddressingFeature feature = new WSAddressingFeature();
            if (addressingRequired(bindingExtensors)
                || addressingRequired(portExtensors)) {
                feature.setAddressingRequired(true);
            }
            addAddressingFeature(feature);
        }
    }
    
    private boolean hasUsingAddressing(List<ExtensibilityElement> exts) {
        boolean found = false;
        if (exts != null) {
            Iterator<ExtensibilityElement> extensionElements = exts.iterator();
            while (extensionElements.hasNext() && !found) {
                ExtensibilityElement ext = 
                    (ExtensibilityElement)extensionElements.next();
                found = JAXWSAConstants.WSAW_USINGADDRESSING_QNAME.equals(ext.getElementType());
            }
        }
        return found;
    }    
    private boolean addressingRequired(List<ExtensibilityElement> exts) {
        boolean found = false;
        if (exts != null) {
            Iterator<ExtensibilityElement> extensionElements = exts.iterator();
            while (extensionElements.hasNext() && !found) {
                ExtensibilityElement ext = 
                    (ExtensibilityElement)extensionElements.next();
                if (JAXWSAConstants.WSAW_USINGADDRESSING_QNAME.equals(ext.getElementType())
                    && ext.getRequired() != null) {
                    return ext.getRequired();
                }
            }
        }
        return false;
    }    

    private void buildWsdlExtensibilities(BindingInfo bindingInfo) {
        Addressing addressing = getAddressing();
        if (addressing != null) {            
            ExtensionRegistry extensionRegistry = getBus().getExtension(WSDLManager.class)
            .getExtensionRegistry();            
            try {
                ExtensibilityElement el = extensionRegistry.createExtension(javax.wsdl.Binding.class, 
                                                                            JAXWSAConstants.
                                                                            WSAW_USINGADDRESSING_QNAME);
                el.setRequired(addressing.required());
                bindingInfo.addExtensor(el);
            } catch (WSDLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }        
    }

    private Addressing getAddressing() {
        Class<?> serviceClass = implInfo.getSEIClass();
        if (serviceClass == null) {
            serviceClass = implInfo.getImplementorClass();
        }
        if (serviceClass == null) {
            return null;
        }
        return serviceClass.getAnnotation(Addressing.class);
    }

    public Binding getJaxwsBinding() {
        return jaxwsBinding;
    }

    private AddressingFeature getAddressingFeature() { 
        if (wsFeatures == null) {
            return null;
        }
        for (WebServiceFeature feature : wsFeatures) {
            if (feature instanceof AddressingFeature) {
                return (AddressingFeature)feature;                
            }
        }
        return null;
    }
    
    public final void resolveFeatures() {
        AddressingFeature addressing = getAddressingFeature();
        if (addressing == null) {
            return;
        }
        if (addressing.isEnabled()) {
            WSAddressingFeature feature = getWSAddressingFeature();
            if (feature == null) {
                feature = new WSAddressingFeature();
                addAddressingFeature(feature);
            }
            feature.setAddressingRequired(addressing.isRequired());
        } else {
            removeAddressingFeature();
        }
    }
    
    public List<AbstractFeature> getFeatures() {
        return features;
    }

    private WSAddressingFeature getWSAddressingFeature() {
        if (features == null) {
            return null;
        }
        for (AbstractFeature f : features) {
            if (f instanceof WSAddressingFeature) {
                return (WSAddressingFeature)f;
            }
        }
        return null;
    }

    private void addAddressingFeature(AbstractFeature a) {
        AbstractFeature f = getWSAddressingFeature();
        if (f == null) {
            features.add(a);
        }
    }

    private void removeAddressingFeature() {
        AbstractFeature f = getWSAddressingFeature();
        if (f != null) {
            features.remove(f);
        }
    }    
    
    private MTOMFeature getMTOMFeature() {
        if (wsFeatures == null) {
            return null;
        }
        for (WebServiceFeature feature : wsFeatures) {
            if (feature instanceof MTOMFeature) {
                return (MTOMFeature)feature;                
            }
        }
        return null;
    }
    
    final void createJaxwsBinding() {
        if (getBinding() instanceof SoapBinding) {
            jaxwsBinding = new SOAPBindingImpl(getEndpointInfo().getBinding());
            MTOMFeature mtomFeature = getMTOMFeature();
            if (mtomFeature != null && mtomFeature.isEnabled()) {
                ((SOAPBinding)jaxwsBinding).setMTOMEnabled(true);
            }
        } else if (getBinding() instanceof XMLBinding) {
            jaxwsBinding = new HTTPBindingImpl(getEndpointInfo().getBinding());
        } else {
            //REVISIT: Should not get here, though some bindings like JBI
            //did not implement their own Binding type.
            jaxwsBinding = new DefaultBindingImpl();
           
        }
    }
}
