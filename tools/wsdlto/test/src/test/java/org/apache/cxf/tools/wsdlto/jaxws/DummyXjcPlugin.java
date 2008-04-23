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

package org.apache.cxf.tools.wsdlto.jaxws;

import org.xml.sax.ErrorHandler;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

public class DummyXjcPlugin extends Plugin {


    static final String XDUMMY_XJC_PLUGIN = "Xdummy-xjc-plugin";
    static final String DUMMY_ARG = "-" + XDUMMY_XJC_PLUGIN + ":" + "arg";
    
    @Override
    public String getOptionName() {
        return XDUMMY_XJC_PLUGIN;
    }

    @Override
    public String getUsage() {
        return DUMMY_ARG;
    }

    @Override
    public boolean run(Outline arg0, Options arg1, ErrorHandler arg2) {
        
        for (ClassOutline classOutline : arg0.getClasses()) {
            JDefinedClass implClass = classOutline.implClass;
            JCodeModel codeModel = implClass.owner();
            JMethod dummyMethod = 
                implClass.method(JMod.PUBLIC, codeModel.ref(String.class), "dummy");
            dummyMethod.body()._return(JExpr.lit("dummy"));
        }
        return true;
    }
    
    @Override
    public int parseArgument(Options opt, String[] args, int i)
        throws BadCommandLineException {
        int ret = 0;
        if (args[i].equals(DUMMY_ARG)) {
            ret = 1;
        }
        
        return ret;
    }
}
