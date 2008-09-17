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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ClassNameProcessor;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.wsdlto.core.WSDLToProcessor;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.PortTypeProcessor;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.ServiceProcessor;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.BindingAnnotator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.WebServiceAnnotator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.XmlSeeAlsoAnnotator;

public class WSDLToJavaProcessor extends WSDLToProcessor implements ClassNameProcessor {
    public static final String MODEL_MAP = WSDLToProcessor.class.getName() 
        + ".MODEL_MAP";
    
    
    public void processClassNames() {
        ServiceInfo serviceInfo = context.get(ServiceInfo.class);

        PortTypeProcessor portTypeProcessor = new PortTypeProcessor(context);
        portTypeProcessor.processClassNames(serviceInfo);
    }

    
    public void process() throws ToolException {
        super.process();

        JavaModel jmodel = wsdlDefinitionToJavaModel(context.get(ServiceInfo.class));

        if (jmodel == null) {
            Message msg = new Message("FAIL_TO_CREATE_JAVA_MODEL", LOG);
            throw new ToolException(msg);
        }
        context.setJavaModel(jmodel);
    }

    private JavaModel wsdlDefinitionToJavaModel(ServiceInfo serviceInfo) throws ToolException {
        JavaModel javaModel = null;
        Map<QName, JavaModel> map = CastUtils.cast((Map)context.get(MODEL_MAP));
        if (map == null) {
            map = new LinkedHashMap<QName, JavaModel>();
            context.put(MODEL_MAP, map);
        }
        if (map.containsKey(serviceInfo.getName())) {
            javaModel = map.get(serviceInfo.getName());
        } else {
            javaModel = new JavaModel();
            map.put(serviceInfo.getName(), javaModel);
        }
        context.put(JavaModel.class, javaModel);
        
        List<JavaInterface> interfaces = new ArrayList<JavaInterface>();
        interfaces.addAll(javaModel.getInterfaces().values());
        
        PortTypeProcessor portTypeProcessor = new PortTypeProcessor(context);
        portTypeProcessor.process(serviceInfo);

        ServiceProcessor serviceProcessor = new ServiceProcessor(context);
        serviceProcessor.process(serviceInfo);
        
        for (JavaInterface intf : javaModel.getInterfaces().values()) {
            if (!interfaces.contains(intf)) {
                intf.annotate(new WebServiceAnnotator());
                intf.annotate(new XmlSeeAlsoAnnotator(context.get(ClassCollector.class)));
                intf.annotate(new BindingAnnotator());
            }
        }
        return javaModel;
    }

}
