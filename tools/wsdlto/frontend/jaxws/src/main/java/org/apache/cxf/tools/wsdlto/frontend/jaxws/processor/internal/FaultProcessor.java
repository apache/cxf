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

import java.util.Collection;
import java.util.List;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaException;
import org.apache.cxf.tools.common.model.JavaExceptionClass;
import org.apache.cxf.tools.common.model.JavaField;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.util.NameUtil;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customization.JAXWSBinding;

public class FaultProcessor extends AbstractProcessor {
    private ClassCollector  collector;

    public FaultProcessor(ToolContext penv) {
        super(penv);
        collector = penv.get(ClassCollector.class);
    }

    public void process(JavaMethod method, Collection<FaultInfo> faults) throws ToolException {
        if (faults == null) {
            return;
        }

        for (FaultInfo fault : faults) {
            processFault(method, fault);
        }
    }

    private boolean isNameCollision(String packageName, String className) {
        if (context.optionSet(ToolConstants.CFG_GEN_OVERWRITE)) {
            return false;
        }
        boolean collision = collector.containTypesClass(packageName, className)
            || collector.containSeiClass(packageName, className);
        return collision;
    }

    private void processFault(JavaMethod method, FaultInfo faultMessage) throws ToolException {
        JavaModel model = method.getInterface().getJavaModel();

        String name = NameUtil.mangleNameToClassName(faultMessage.getName().getLocalPart());
        String namespace = faultMessage.getName().getNamespaceURI();
        String packageName = ProcessorUtil.parsePackageName(namespace, context.mapPackageName(namespace));
        if (namespace.equals(method.getInterface().getNamespace())) {
            packageName = method.getInterface().getPackageName();
        }


        JAXWSBinding jaxwsBinding = faultMessage.getExtensor(JAXWSBinding.class);
        if (jaxwsBinding != null) {
            if (jaxwsBinding.getPackage() != null) {
                packageName = jaxwsBinding.getPackage();
            }
            if (jaxwsBinding.getJaxwsClass() != null
                && jaxwsBinding.getJaxwsClass().getClassName() != null) {
                name = jaxwsBinding.getJaxwsClass().getClassName();
                if (name.contains(".")) {
                    packageName = name.substring(0, name.lastIndexOf('.'));
                    name = name.substring(name.lastIndexOf('.') + 1);
                }
            }
        }

        while (isNameCollision(packageName, name)) {
            name = name + "_Exception";
        }

        String fullClassName = packageName + "." + name;
        collector.addExceptionClassName(packageName, name, fullClassName);

        boolean samePackage = method.getInterface().getPackageName().equals(packageName);
        method.addException(new JavaException(faultMessage.getName().getLocalPart(), 
                                              samePackage ? name : fullClassName, namespace));

        List<MessagePartInfo> faultParts = faultMessage.getMessageParts();

        JavaExceptionClass expClass = new JavaExceptionClass(model);
        expClass.setName(name);
        expClass.setNamespace(namespace);
        expClass.setPackageName(packageName);

        for (MessagePartInfo part : faultParts) {
            String fName = null;
            String fNamespace = null;

            if (part.getElementQName() != null) {
                fNamespace = part.getElementQName().getNamespaceURI();
                //fNamespace = part.getConcreteName().getNamespaceURI();
                fName = part.getConcreteName().getLocalPart();
            } else {
                fNamespace = part.getTypeQName().getNamespaceURI();
                fName = part.getConcreteName().getLocalPart();
            }

            if (StringUtils.isEmpty(fNamespace)) {
                fNamespace = namespace;
            }

            String fType = ProcessorUtil.getType(part, context, false);

            //REVISIT - custom JAXB package names
            String fPackageName = method.getInterface().getPackageName();


            JavaField fField = new JavaField(fName, fType, fNamespace);
            fField.setQName(ProcessorUtil.getElementName(part));

            if (!method.getInterface().getPackageName().equals(fPackageName)) {
                fField.setClassName(ProcessorUtil.getFullClzName(part, context, false));
            }
            if (!fType.equals(ProcessorUtil.resolvePartType(part))) {
                fField.setClassName(ProcessorUtil.getType(part, context, true));
            }

            expClass.addField(fField);
        }
        model.addExceptionClass(packageName + "." + name, expClass);
    }
}
