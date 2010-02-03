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

/**
 * Modifies the JAXB code model to override the Object.toString() method with an 
 * implementation that provides a String representation of the xml content.
 */
public class ToStringPlugin {
    
    private static final Logger LOG = Logger.getLogger(ToStringPlugin.class.getName()); //NOPMD

    private String styleFieldName = "DEFAULT_STYLE";
    private String styleClassName = "org.apache.cxf.jaxb.JAXBToStringStyle";
    private boolean active;
    
    public String getOptionName() {
        return "Xts";
    }

    public String getUsage() {
        return "  -Xts                 : Activate plugin to add a toString() method to generated classes\n"
            +  "         equivalent to: -Xts:style:org.apache.cxf.jaxb.JAXBToStringStyle.DEFAULT_STYLE\n"
            +  "  -Xts:style:multiline : Have toString produce multi line output\n"
            +  "         equivalent to: -Xts:style:org.apache.cxf.jaxb.JAXBToStringStyle.MULTI_LINE_STYLE\n"
            +  "  -Xts:style:simple    : Have toString produce single line terse output\n"
            +  "         equivalent to: -Xts:style:org.apache.cxf.jaxb.JAXBToStringStyle.SIMPLE_STYLE\n"
            +  "  -Xts:style:org.apache.commons.lang.builder.ToStringStyle.FIELD : The full class+field\n"
            +  "         name of the ToStringStyle to use.";
    }

    public int parseArgument(Options opt, String[] args, int index, com.sun.tools.xjc.Plugin plugin) 
        throws BadCommandLineException, IOException {
        int ret = 0;
        
        if (args[index].startsWith("-Xts")) {
            ret = 1;                    
            if (args[index].startsWith("-Xts:style:")) {
                String v = args[index].substring("-Xts:style:".length());
                if ("multiline".equals(v)) {
                    styleFieldName = "MULTI_LINE_STYLE";
                } else if ("simple".equals(v)) {
                    styleFieldName = "SIMPLE_STYLE";                    
                } else {
                    int idx = v.lastIndexOf('.');
                    styleFieldName = v.substring(idx + 1);
                    styleClassName = v.substring(0, idx);
                }
            }
            if (!opt.activePlugins.contains(plugin)) {
                opt.activePlugins.add(plugin);
            }
            active = true;
        }
        return ret;
    }
    
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) {
        LOG.fine("Running toString() plugin.");
        if (!active) {
            return true;
        }

        final JClass toStringDelegateImpl = outline.getCodeModel()
            .ref("org.apache.commons.lang.builder.ToStringBuilder");
        final JClass styleClass = outline.getCodeModel().ref(styleClassName);
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
        final JInvocation invoke = delegateImpl.staticInvoke("reflectionToString");
        invoke.arg(JExpr._this());
        invoke.arg(toStringDelegateStyleParam);
        toStringMethod.body()._return(invoke);
        
        JDocComment doc = toStringMethod.javadoc();
        doc.add("Generates a String representation of the contents of this type.");
        doc.add("\nThis is an extension method, produced by the 'ts' xjc plugin");
        toStringMethod.annotate(Override.class);
    }

    public void onActivated(Options opts) {
        active = true;
    }
}
