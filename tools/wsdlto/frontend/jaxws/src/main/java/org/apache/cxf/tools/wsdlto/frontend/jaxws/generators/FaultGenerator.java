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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaExceptionClass;
import org.apache.cxf.tools.common.model.JavaField;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.WSDLToJavaProcessor;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.ProcessorUtil;

public class FaultGenerator extends AbstractJAXWSGenerator {

    private static final String FAULT_TEMPLATE = TEMPLATE_BASE + "/fault.vm";

    public FaultGenerator() {
        this.name = ToolConstants.FAULT_GENERATOR;
    }


    public boolean passthrough() {
        if (env.optionSet(ToolConstants.CFG_GEN_FAULT)
            || env.optionSet(ToolConstants.CFG_ALL)) {
            return false;
        } 
        if (env.optionSet(ToolConstants.CFG_GEN_ANT)
            || env.optionSet(ToolConstants.CFG_GEN_TYPES)
            || env.optionSet(ToolConstants.CFG_GEN_CLIENT)
            || env.optionSet(ToolConstants.CFG_GEN_IMPL)
            || env.optionSet(ToolConstants.CFG_GEN_SEI)
            || env.optionSet(ToolConstants.CFG_GEN_SERVER)
            || env.optionSet(ToolConstants.CFG_GEN_SERVICE)) {
            return true;
        }
        
        return false;
    }

    public void generate(ToolContext penv) throws ToolException {
        this.env = penv;
        if (passthrough()) {
            return;
        }
        Map<QName, JavaModel> map = CastUtils.cast((Map)penv.get(WSDLToJavaProcessor.MODEL_MAP));
        for (JavaModel javaModel : map.values()) {

            Map<String, JavaExceptionClass> exceptionClasses = javaModel
                    .getExceptionClasses();
            for (Iterator iter = exceptionClasses.keySet().iterator(); iter
                    .hasNext();) {
                String expClassName = (String)iter.next();
                JavaExceptionClass expClz =
                    exceptionClasses.get(expClassName);
    
                clearAttributes();
                setAttributes("suid", getSUID());
                setAttributes("expClass", expClz);
                for (JavaField jf : expClz.getFields()) {
                    setAttributes("paraName", ProcessorUtil.mangleNameToVariableName(jf.getName()));
                }
                setCommonAttributes();
                doWrite(FAULT_TEMPLATE, parseOutputName(expClz.getPackageName(),
                        expClz.getName()));
            }
        }
    }

    private String getSUID() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    public void register(final ClassCollector collector, String packageName, String fileName) {
        collector.addExceptionClassName(packageName , fileName , packageName + "." + fileName);
    }
}
