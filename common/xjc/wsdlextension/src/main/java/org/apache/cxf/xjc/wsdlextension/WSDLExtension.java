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

package org.apache.cxf.xjc.wsdlextension;

import java.io.IOException;
import java.util.logging.Logger;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;

import org.xml.sax.ErrorHandler;

import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPrimitiveType;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

import org.apache.cxf.common.logging.LogUtils;

public class WSDLExtension {

    private static final Logger LOG = LogUtils.getL7dLogger(WSDLExtension.class);

    public String getOptionName() {
        return "Xwsdlextension";
    }

    public String getUsage() {
        return "  -Xwsdlextension               "
               + ":Activate plugin to add wsdl extension methods to generated root classes\n";
    }

    public int parseArgument(Options opt, String[] args, int index) throws BadCommandLineException,
        IOException {
        int ret = 0;
        if (args[index].equals("-Xwsdlextension")) {
            ret = 1;
        }
        return ret;
    }

    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) {
        LOG.fine("Running WSDLExtension plugin.");

        for (ClassOutline co : outline.getClasses()) {
            addWSDLExtension(co);
        }
        return true;
    }

    private void addWSDLExtension(ClassOutline co) {
        final JDefinedClass implementation = co.implClass;
        implementation._implements(ExtensibilityElement.class);

        JFieldVar elementTypeVar = implementation.field(JMod.PROTECTED, QName.class, "elementType");
        elementTypeVar.annotate(XmlTransient.class);

        JFieldVar requiredVar = implementation.field(JMod.PROTECTED, Boolean.class, "required");
        JAnnotationUse requiredAnnotation = requiredVar.annotate(XmlAttribute.class);
        requiredAnnotation.param("namespace", "http://schemas.xmlsoap.org/wsdl/");

        JMethod getElementTypeMethod = implementation.method(JMod.PUBLIC, QName.class,
                                                             "getElementType");
        getElementTypeMethod.body()._return(JExpr.direct("elementType"));

        JMethod setElementTypeMethod = implementation.method(JMod.PUBLIC, JPrimitiveType.parse(co
            .parent().getCodeModel(), "void"), "setElementType");
        setElementTypeMethod.param(QName.class, "type");
        setElementTypeMethod.body().directStatement("this.elementType = type;");

        JMethod getRequiredMethod = implementation.method(JMod.PUBLIC, Boolean.class,
                                                             "getRequired");
        getRequiredMethod.body()._return(JExpr.direct("required == null ? false : required"));

        JMethod setRequiredMethod = implementation.method(JMod.PUBLIC, JPrimitiveType.parse(co
            .parent().getCodeModel(), "void"), "setRequired");
        setRequiredMethod.param(Boolean.class, "required");
        setRequiredMethod.body().directStatement("this.required = required;");
    }
}
