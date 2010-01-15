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
package org.apache.cxf.xjc.bg;

import java.util.Collection;
import java.util.logging.Logger;

import org.xml.sax.ErrorHandler;

import com.sun.codemodel.JMethod;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;


/**
 * Generate getters named getXXX() for Booleans instead of isXXX(). Useful to use generated beans with tools
 * that needs introspections, like dozer.
 */
public class BooleanGetterPlugin {

    private static final Logger LOG = Logger.getLogger(BooleanGetterPlugin.class.getName()); //NOPMD

    public BooleanGetterPlugin() {
    }

    public String getOptionName() {
        return "Xbg";
    }

    public String getUsage() {
        return "  -Xbg                 : Generate getters methods for Booleans";
    }

    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) {
        LOG.info("Running boolean getter plugin.");
        for (ClassOutline classOutline : outline.getClasses()) {
            Collection<JMethod> methods = classOutline.implClass.methods();
            for (JMethod method : methods) {
                if (method.name().startsWith("is")) {
                    String newName = "get" + method.name().substring(2);
                    LOG.info("Changing method name from " + method.name() + " to " + newName);
                    method.javadoc().add(
                                         "\nThis getter has been renamed from " + method.name() + "() to "
                                             + newName + "() by cxf-xjc-boolean plugin.");
                    method.name(newName);
                }
            }
        }
        return true;
    }

}
