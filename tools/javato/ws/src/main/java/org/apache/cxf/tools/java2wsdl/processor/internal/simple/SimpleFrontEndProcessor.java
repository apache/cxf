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
package org.apache.cxf.tools.java2wsdl.processor.internal.simple;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.Processor;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.java2ws.util.JavaFirstUtil;
import org.apache.cxf.tools.java2wsdl.processor.internal.AntGenerator;
import org.apache.cxf.tools.java2wsdl.processor.internal.simple.generator.SimpleClientGenerator;
import org.apache.cxf.tools.java2wsdl.processor.internal.simple.generator.SimpleImplGenerator;
import org.apache.cxf.tools.java2wsdl.processor.internal.simple.generator.SimpleSEIGenerator;
import org.apache.cxf.tools.java2wsdl.processor.internal.simple.generator.SimpleServerGenerator;
import org.apache.cxf.tools.wsdlto.core.AbstractGenerator;

public class SimpleFrontEndProcessor implements Processor {
    private ToolContext context;
    private List<AbstractGenerator> generators = new ArrayList<AbstractGenerator>();
    @SuppressWarnings("unchecked")
    public void process() throws ToolException {       
        List<ServiceInfo> services = (List<ServiceInfo>)context.get(ToolConstants.SERVICE_LIST);
        ServiceInfo serviceInfo = services.get(0);
        
        JavaInterface jinf = JavaFirstUtil.serviceInfo2JavaInf(serviceInfo);
        JavaModel jm = new JavaModel();
        jm.addInterface("inf", jinf);
        jinf.setJavaModel(jm);
        context.put(JavaModel.class, jm);
        generators.add(new SimpleSEIGenerator());
        generators.add(new SimpleImplGenerator());
        generators.add(new SimpleServerGenerator());
        generators.add(new SimpleClientGenerator());
        generators.add(new AntGenerator());
        
        for (AbstractGenerator generator : generators) {
            generator.generate(context);
        }

    }
    
    public void setEnvironment(ToolContext env) {
        this.context = env;
    } 
}
