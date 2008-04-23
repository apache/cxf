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

import java.util.Iterator;
import java.util.Map;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.common.model.JavaPort;
import org.apache.cxf.tools.common.model.JavaServiceClass;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.util.NameUtil;

public class ClientGenerator extends AbstractJAXWSGenerator {

    private static final String CLT_TEMPLATE = TEMPLATE_BASE + "/client.vm";

    public ClientGenerator() {
        this.name = ToolConstants.CLT_GENERATOR;
    }

    public boolean passthrough() {
        if (env.optionSet(ToolConstants.CFG_GEN_CLIENT) || env.optionSet(ToolConstants.CFG_CLIENT)
            || env.optionSet(ToolConstants.CFG_ALL)) {
            return false;
        }
        if (env.optionSet(ToolConstants.CFG_GEN_ANT) || env.optionSet(ToolConstants.CFG_GEN_TYPES)
            || env.optionSet(ToolConstants.CFG_GEN_IMPL) || env.optionSet(ToolConstants.CFG_GEN_SEI)
            || env.optionSet(ToolConstants.CFG_GEN_SERVER) || env.optionSet(ToolConstants.CFG_GEN_SERVICE)
            || env.optionSet(ToolConstants.CFG_GEN_FAULT)) {
            return true;
        }

        return true;

    }

    public void generate(ToolContext penv) throws ToolException {
        this.env = penv;
        JavaModel javaModel = env.get(JavaModel.class);
        if (passthrough()) {
            return;
        }
        if (javaModel.getServiceClasses().size() == 0) {
            ServiceInfo serviceInfo = (ServiceInfo)env.get(ServiceInfo.class);
            String wsdl = serviceInfo.getDescription().getBaseURI();
            Message msg = new Message("CAN_NOT_GEN_CLIENT", LOG, wsdl);
            if (penv.isVerbose()) {
                System.out.println(msg.toString());
            }
            return;
        }

        Map<String, JavaInterface> interfaces = javaModel.getInterfaces();
        Iterator it = javaModel.getServiceClasses().values().iterator();
        while (it.hasNext()) {
            JavaServiceClass js = (JavaServiceClass)it.next();
            Iterator i = js.getPorts().iterator();
            while (i.hasNext()) {
                JavaPort jp = (JavaPort)i.next();
                String interfaceName = jp.getInterfaceClass();
                JavaInterface intf = interfaces.get(interfaceName);
                if (intf == null) {
                    interfaceName = jp.getPortType();
                    intf = interfaces.get(interfaceName);
                }
                
                String clientClassName = interfaceName + "_"
                                         + NameUtil.mangleNameToClassName(jp.getPortName()) + "_Client";

                clearAttributes();
                setAttributes("clientClassName", clientClassName);
                setAttributes("intf", intf);
                setAttributes("service", js);
                setAttributes("port", jp);

                setCommonAttributes();

                doWrite(CLT_TEMPLATE, parseOutputName(intf.getPackageName(), clientClassName));
            }
        }
    }

    public void register(final ClassCollector collector, String packageName, String fileName) {
        collector.addClientClassName(packageName , fileName , packageName + "." + fileName);
    }
}
