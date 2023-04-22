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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import jakarta.xml.ws.Service;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.apache.cxf.tools.wsdlto.WSDLToJavaContainer;

public class JAXWSContainer extends WSDLToJavaContainer {

    private static final String TOOL_NAME = "wsdl2java";

    public JAXWSContainer(ToolSpec toolspec) throws Exception {
        super(TOOL_NAME, toolspec);
    }

    public Set<String> getArrayKeys() {
        Set<String> set = super.getArrayKeys();
        set.add(ToolConstants.CFG_BINDING);
        set.add(ToolConstants.CFG_RESERVE_NAME);
        set.add(ToolConstants.CFG_ASYNCMETHODS);
        set.add(ToolConstants.CFG_BAREMETHODS);
        set.add(ToolConstants.CFG_MIMEMETHODS);
        set.add(ToolConstants.CFG_SEI_SUPER);
        return set;
    }

    public String getServiceSuperclass() {
        return Service.class.getName();
    }
    public String getServiceTarget() {
        return isJaxws22() ? "jaxws22" : "jaxws21";
    }
    public boolean isJaxws22() {
        return Service.class.getDeclaredConstructors().length == 2;
    }

    public void validate(ToolContext env) throws ToolException {
        env.put("service.target", getServiceTarget());
        env.put("service.superclass", getServiceSuperclass());
        super.validate(env);
        if (env.containsKey(ToolConstants.CFG_BINDING)) {
            String[] bindings = (String[])env.get(ToolConstants.CFG_BINDING);
            for (int i = 0; i < bindings.length; i++) {
                try (URIResolver resolver = new URIResolver(bindings[i])) {
                    if (!resolver.isResolved()) {
                        Message msg = new Message("FILE_NOT_EXIST", LOG, bindings[i]);
                        throw new ToolException(msg);
                    }
                } catch (IOException ioe) {
                    throw new ToolException(ioe);
                }
            }
            env.put(ToolConstants.CFG_BINDING, bindings);
        }
        cleanArrays(env, ToolConstants.CFG_ASYNCMETHODS);
        cleanArrays(env, ToolConstants.CFG_BAREMETHODS);
        cleanArrays(env, ToolConstants.CFG_MIMEMETHODS);
    }

    private void cleanArrays(ToolContext env, String key) {
        String[] s = env.getArray(key);
        if (s != null) {
            List<String> n = new ArrayList<>();
            for (String s2 : s) {
                StringTokenizer tokenizer = new StringTokenizer(s2, ",=", false);
                while (tokenizer.hasMoreTokens()) {
                    String arg = tokenizer.nextToken();
                    n.add(arg);
                }
            }
            env.put(key, n.toArray(new String[0]));
        }
    }
}
