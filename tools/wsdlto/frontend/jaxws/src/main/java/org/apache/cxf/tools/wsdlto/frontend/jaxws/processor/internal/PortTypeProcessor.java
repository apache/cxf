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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customization.JAXWSBinding;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.mapper.InterfaceMapper;

public class PortTypeProcessor extends AbstractProcessor {
    private List<QName> operationMap = new ArrayList<QName>();

    public PortTypeProcessor(ToolContext c) {
        super(c);
    }

    public static JavaInterface getInterface(
                                       ToolContext context,
                                       ServiceInfo serviceInfo,
                                       InterfaceInfo interfaceInfo) throws ToolException {
        JavaInterface intf = interfaceInfo.getProperty("JavaInterface", JavaInterface.class);
        if (intf == null) {
            intf = new InterfaceMapper(context).map(interfaceInfo);

            JAXWSBinding jaxwsBinding = null;
            if (serviceInfo.getDescription() != null) {
                jaxwsBinding = serviceInfo.getDescription().getExtensor(JAXWSBinding.class);
            }
            JAXWSBinding infBinding = interfaceInfo.getExtensor(JAXWSBinding.class);
            if (infBinding != null && infBinding.getPackage() != null) {
                intf.setPackageName(infBinding.getPackage());
            } else if (jaxwsBinding != null && jaxwsBinding.getPackage() != null) {
                intf.setPackageName(jaxwsBinding.getPackage());
            }
            
            if (infBinding != null && !infBinding.getPackageJavaDoc().equals("")) {
                intf.setPackageJavaDoc(infBinding.getPackageJavaDoc());
            } else if (jaxwsBinding != null && !jaxwsBinding.getPackageJavaDoc().equals("")) {
                intf.setPackageJavaDoc(jaxwsBinding.getPackageJavaDoc());
            }

            String name = intf.getName();
            if (infBinding != null
                && infBinding.getJaxwsClass() != null
                && infBinding.getJaxwsClass().getClassName() != null) {
                name = infBinding.getJaxwsClass().getClassName();
                
                if (name.contains(".")) {
                    intf.setPackageName(name.substring(0, name.lastIndexOf('.')));
                    name = name.substring(name.lastIndexOf('.') + 1);
                }

                intf.setClassJavaDoc(infBinding.getJaxwsClass().getComments());
            }

            ClassCollector collector = context.get(ClassCollector.class);
            if (context.optionSet(ToolConstants.CFG_AUTORESOLVE)) {
                int count = 0;
                String checkName = name;
                while (collector.isReserved(intf.getPackageName(), checkName)) {
                    checkName = name + "_" + (++count);
                }
                name = checkName;
            } else if (collector.isReserved(intf.getPackageName(), name)) {
                throw new ToolException("RESERVED_SEI_NAME", LOG, name);
            }
            interfaceInfo.setProperty("InterfaceName", name);
            intf.setName(name);
            collector.addSeiClassName(intf.getPackageName(),
                                      intf.getName(),
                                      intf.getPackageName() + "." + intf.getName());
        
            interfaceInfo.setProperty("JavaInterface", intf);
        }
        return intf;
    }
    
    public void processClassNames(ServiceInfo serviceInfo) throws ToolException {
        InterfaceInfo interfaceInfo = serviceInfo.getInterface();
        if (interfaceInfo == null) {
            return;
        }
        getInterface(context, serviceInfo, interfaceInfo);
    }

    public void process(ServiceInfo serviceInfo) throws ToolException {
        operationMap.clear();
        JavaModel jmodel = context.get(JavaModel.class);


        InterfaceInfo interfaceInfo = serviceInfo.getInterface();

        if (interfaceInfo == null) {
            return;
        }

        JavaInterface intf = getInterface(context, serviceInfo, interfaceInfo);
        intf.setJavaModel(jmodel);

        Element handler = (Element)context.get(ToolConstants.HANDLER_CHAIN);
        intf.setHandlerChains(handler);


        Collection<OperationInfo> operations = interfaceInfo.getOperations();

        for (OperationInfo operation : operations) {
            if (isOverloading(operation.getName())) {
                LOG.log(Level.WARNING, "SKIP_OVERLOADED_OPERATION", operation.getName());
                continue;
            }
            OperationProcessor operationProcessor = new OperationProcessor(context);
            operationProcessor.process(intf, operation);
        }

        jmodel.setLocation(intf.getLocation());
        jmodel.addInterface(intf.getName(), intf);
    }

    private boolean isOverloading(QName operationName) {
        if (operationMap.contains(operationName)) {
            return true;
        } else {
            operationMap.add(operationName);
        }
        return false;
    }


}
