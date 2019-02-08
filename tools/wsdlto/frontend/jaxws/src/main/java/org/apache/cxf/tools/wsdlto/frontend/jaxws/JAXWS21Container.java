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

package org.apache.cxf.tools.wsdlto.frontend.jaxws;

import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolSpec;

/**
 *
 */
public class JAXWS21Container extends JAXWSContainer {
    public JAXWS21Container(ToolSpec toolspec) throws Exception {
        super(toolspec);
    }
    public String getServiceTarget() {
        return "jaxws21";
    }
    public void validate(ToolContext env) throws ToolException {
        super.validate(env);
        Object o = env.get(ToolConstants.CFG_XJC_ARGS);
        if (o instanceof String) {
            o = new String[] {(String)o, "-target", "2.1"};
            env.put(ToolConstants.CFG_XJC_ARGS, o);
        } else if (o == null) {
            o = new String[] {"-target", "2.1"};
            env.put(ToolConstants.CFG_XJC_ARGS, o);
        } else {
            String[] xjcArgs = (String[])o;
            String[] tmp = new String[xjcArgs.length + 2];
            System.arraycopy(xjcArgs, 0, tmp, 0, xjcArgs.length);
            tmp[xjcArgs.length] = "-target";
            tmp[xjcArgs.length + 1] = "2.1";
            env.put(ToolConstants.CFG_XJC_ARGS, tmp);
        }
    }
}
