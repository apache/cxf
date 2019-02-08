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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.generators;

import java.util.Map;

import javax.jws.HandlerChain;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.annotations.DataBinding;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JAnnotationElement;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.WSDLToJavaProcessor;

public class SEIGenerator extends AbstractJAXWSGenerator {

    private static final String SEI_TEMPLATE = TEMPLATE_BASE + "/sei.vm";

    public SEIGenerator() {
        this.name = ToolConstants.SEI_GENERATOR;
    }

    public boolean passthrough() {
        if (env.optionSet(ToolConstants.CFG_GEN_SEI) || env.optionSet(ToolConstants.CFG_ALL)) {
            return false;
        }
        return env.optionSet(ToolConstants.CFG_GEN_ANT) || env.optionSet(ToolConstants.CFG_GEN_TYPES)
            || env.optionSet(ToolConstants.CFG_GEN_CLIENT) || env.optionSet(ToolConstants.CFG_GEN_IMPL)
            || env.optionSet(ToolConstants.CFG_GEN_SERVER) || env.optionSet(ToolConstants.CFG_GEN_SERVICE)
            || env.optionSet(ToolConstants.CFG_GEN_FAULT);
    }

    private boolean hasHandlerConfig(JavaInterface intf) {
        return intf.getHandlerChains() != null;

    }

    public void generate(ToolContext penv) throws ToolException {
        this.env = penv;
        if (passthrough()) {
            return;
        }

        Map<QName, JavaModel> map = CastUtils.cast((Map<?, ?>)penv.get(WSDLToJavaProcessor.MODEL_MAP));
        for (JavaModel javaModel : map.values()) {

            Map<String, JavaInterface> interfaces = javaModel.getInterfaces();

            if (interfaces.isEmpty()) {
                ServiceInfo serviceInfo = env.get(ServiceInfo.class);
                String wsdl = serviceInfo.getDescription().getBaseURI();
                Message msg = new Message("CAN_NOT_GEN_SEI", LOG, wsdl);
                if (penv.isVerbose()) {
                    System.out.println(msg.toString());
                }
                continue;
            }
            for (JavaInterface intf : interfaces.values()) {

                if (hasHandlerConfig(intf)) {
                    HandlerConfigGenerator handlerGen = new HandlerConfigGenerator();
                    // REVISIT: find a better way to handle Handler gen, should not
                    // pass JavaInterface around.
                    handlerGen.setJavaInterface(intf);
                    handlerGen.generate(getEnvironment());

                    JAnnotation annot = handlerGen.getHandlerAnnotation();
                    if (handlerGen.getHandlerAnnotation() != null) {
                        boolean existHandlerAnno = false;
                        for (JAnnotation jann : intf.getAnnotations()) {
                            if (jann.getType() == HandlerChain.class) {
                                existHandlerAnno = true;
                            }
                        }
                        if (!existHandlerAnno) {
                            intf.addAnnotation(annot);
                            intf.addImport("javax.jws.HandlerChain");
                        }
                    }
                }
                if (penv.containsKey(ToolConstants.RUNTIME_DATABINDING_CLASS)) {
                    JAnnotation ann = new JAnnotation(DataBinding.class);
                    JAnnotationElement el
                        = new JAnnotationElement(null,
                                                 penv.get(ToolConstants.RUNTIME_DATABINDING_CLASS),
                                                 true);
                    ann.addElement(el);
                    intf.addAnnotation(ann);
                }
                clearAttributes();
                setAttributes("intf", intf);
                String seiSc = "";
                for (String s : intf.getSuperInterfaces()) {
                    if (!seiSc.isEmpty()) {
                        seiSc += ", ";
                    } else {
                        seiSc = "extends ";
                    }
                    seiSc += s;
                }
                if (!StringUtils.isEmpty(seiSc)) {
                    seiSc += " ";
                }
                setAttributes("seiSuperinterfaceString", seiSc);
                setCommonAttributes();

                doWrite(SEI_TEMPLATE, parseOutputName(intf.getPackageName(), intf.getName()));

            }
        }
    }

    public void register(final ClassCollector collector, String packageName, String fileName) {
        collector.addSeiClassName(packageName, fileName, packageName + "." + fileName);
    }
}
