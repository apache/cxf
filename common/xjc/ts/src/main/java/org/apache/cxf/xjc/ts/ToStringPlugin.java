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

package org.apache.cxf.xjc.ts;

import java.io.IOException;
import java.util.logging.Logger;

import org.xml.sax.ErrorHandler;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxb.JAXBToStringBuilder;
import org.apache.cxf.jaxb.JAXBToStringStyle;


/**
 * Modifies the JAXB code model to override the Object.toString() method with an 
 * implementation that provides a String representation of the xml content.
 */
public class ToStringPlugin {
    
    private static final Logger LOG = LogUtils.getL7dLogger(ToStringPlugin.class);

    private String styleFieldName = "DEFAULT_STYLE";
    public String getOptionName() {
        return "Xts";
    }

    public String getUsage() {
        return "  -Xts                 : Activate plugin to add a toString() method to generated classes\n"
            +  "  -Xts:style:multiline : Have toString produce multi line output\n"
            +  "  -Xts:style:simple    : Have toString produce single line terse output\n";
    }

    public int parseArgument(Options opt, String[] args, int index) 
        throws BadCommandLineException, IOException {
        int ret = 0;
        if (args[index].equals("-Xts:style:multiline")) {
            styleFieldName = "MULTI_LINE_STYLE";
            ret = 1;
        } else if (args[index].equals("-Xts:style:simple")) {
            styleFieldName = "SIMPLE_STYLE";
            ret = 1;
        }
        return ret;
    }
    
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) {
        LOG.fine("Running toString() plugin.");
        
        final JClass toStringDelegateImpl = outline.getCodeModel().ref(JAXBToStringBuilder.class);
        final JClass styleClass = outline.getCodeModel().ref(JAXBToStringStyle.class);
        final JFieldRef toStringDelegateStyleParam = styleClass.staticRef(styleFieldName);
        
        for (ClassOutline co : outline.getClasses()) {
            addToStringMethod(co, toStringDelegateImpl, toStringDelegateStyleParam);
        }
        
        return true;
    }

    private void addToStringMethod(ClassOutline co, 
                                   JClass delegateImpl, 
                                   JFieldRef toStringDelegateStyleParam) {
        final JDefinedClass implementation = co.implClass;
        final JMethod toStringMethod = implementation.method(JMod.PUBLIC, String.class, "toString");
        final JInvocation invoke = delegateImpl.staticInvoke("valueOf");
        invoke.arg(JExpr._this());
        invoke.arg(toStringDelegateStyleParam);
        toStringMethod.body()._return(invoke);
        
        JDocComment doc = toStringMethod.javadoc();
        doc.add("Generates a String representation of the contents of this type.");
        doc.add("\nThis is an extension method, produced by the 'ts' xjc plugin");
        toStringMethod.annotate(Override.class);
    }
}
