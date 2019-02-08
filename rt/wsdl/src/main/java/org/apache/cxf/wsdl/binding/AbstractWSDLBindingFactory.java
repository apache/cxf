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

package org.apache.cxf.wsdl.binding;

import java.util.Collection;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.extensions.ElementExtensible;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.AbstractBindingFactory;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.model.AbstractPropertiesHolder;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.ServiceInfo;

import static org.apache.cxf.helpers.CastUtils.cast;

public abstract class AbstractWSDLBindingFactory extends AbstractBindingFactory
                      implements WSDLBindingFactory {

    public AbstractWSDLBindingFactory() {
    }

    public AbstractWSDLBindingFactory(Collection<String> ns) {
        super(ns);
    }

    public AbstractWSDLBindingFactory(Bus b) {
        super(b);
    }

    public AbstractWSDLBindingFactory(Bus b, Collection<String> ns) {
        super(b, ns);
    }


    /**
     * Copies extensors from the Binding to BindingInfo.
     * @param service
     * @param binding
     * @param ns
     */
    public BindingInfo createBindingInfo(ServiceInfo service, Binding binding, String ns) {

        BindingInfo bi = createBindingInfo(service, ns, null);
        return initializeBindingInfo(service, binding, bi);
    }

    protected BindingInfo initializeBindingInfo(ServiceInfo service, Binding binding, BindingInfo bi) {
        bi.setName(binding.getQName());
        copyExtensors(bi, binding, null);

        for (BindingOperation bop : cast(binding.getBindingOperations(), BindingOperation.class)) {
            String inName = null;
            String outName = null;
            if (bop.getBindingInput() != null) {
                inName = bop.getBindingInput().getName();
            }
            if (bop.getBindingOutput() != null) {
                outName = bop.getBindingOutput().getName();
            }
            String portTypeNs = binding.getPortType().getQName().getNamespaceURI();
            QName opName = new QName(portTypeNs,
                                     bop.getName());
            BindingOperationInfo bop2 = bi.getOperation(opName);
            if (bop2 == null) {
                bop2 = bi.buildOperation(opName, inName, outName);
                if (bop2 != null) {
                    bi.addOperation(bop2);
                }
            }
            if (bop2 != null) {
                copyExtensors(bop2, bop, bop2);
                if (bop.getBindingInput() != null) {
                    copyExtensors(bop2.getInput(), bop.getBindingInput(), bop2);
                }
                if (bop.getBindingOutput() != null) {
                    copyExtensors(bop2.getOutput(), bop.getBindingOutput(), bop2);
                }
                for (BindingFault f : cast(bop.getBindingFaults().values(), BindingFault.class)) {
                    if (StringUtils.isEmpty(f.getName())) {
                        throw new IllegalArgumentException("wsdl:fault and soap:fault elements"
                                                           + " must have a name attribute.");
                    }
                    copyExtensors(bop2.getFault(new QName(service.getTargetNamespace(), f.getName())),
                                  bop.getBindingFault(f.getName()), bop2);
                }
            }
        }
        return bi;
    }

    private void copyExtensors(AbstractPropertiesHolder info, ElementExtensible extElement,
                               BindingOperationInfo bop) {
        if (info != null) {
            for (ExtensibilityElement ext : cast(extElement.getExtensibilityElements(),
                                                 ExtensibilityElement.class)) {
                info.addExtensor(ext);
                if (bop != null && extElement instanceof BindingInput) {
                    addMessageFromBinding(ext, bop, true);
                }
                if (bop != null && extElement instanceof BindingOutput) {
                    addMessageFromBinding(ext, bop, false);
                }
            }
        }
    }

    protected void addMessageFromBinding(ExtensibilityElement ext, BindingOperationInfo bop,
                                         boolean isInput) {
    }

}
