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

package org.apache.cxf.tools.wsdlto.javascript;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.tools.common.CommandInterfaceUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolRunner;

public class WSDLToJavaScript {


    private String[] args;

    public WSDLToJavaScript() {
        args = new String[0];
    }
    public WSDLToJavaScript(String pargs[]) {
        args = pargs;
    }

    private boolean isExitOnFinish() {
        String exit = System.getProperty("exitOnFinish");
        if (StringUtils.isEmpty(exit)) {
            return false;
        }
        return "YES".equalsIgnoreCase(exit) || "TRUE".equalsIgnoreCase(exit);
    }

    public void run(ToolContext context) throws Exception {
        if (args != null) {
            context.put(ToolConstants.CFG_CMD_ARG, args);
        }

        InputStream toolspecStream = getResourceAsStream(WSDLToJavaScript.class, "javascript-toolspec.xml");

        ToolRunner.runTool(JavaScriptContainer.class,
                           toolspecStream,
                           false,
                           args,
                           isExitOnFinish(),
                           context);
    }

    protected boolean isVerbose() {
        return isSet(new String[]{"-V", "-verbose"});
    }

    private boolean isSet(String[] keys) {
        if (args == null) {
            return false;
        }
        List<String> pargs = Arrays.asList(args);

        for (String key : keys) {
            if (pargs.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public void setArguments(String[] pargs) {
        args = pargs;
    }

    public static void main(String[] pargs) {
        
        CommandInterfaceUtils.commandCommonMain();

        WSDLToJavaScript w2j = new WSDLToJavaScript(pargs);
        try {

            w2j.run(new ToolContext());

        } catch (ToolException ex) {
            System.err.println();
            System.err.println("WSDLToJS Error : " + ex.getMessage());
            System.err.println();
            if (w2j.isVerbose()) {
                ex.printStackTrace();
            }
            if (w2j.isExitOnFinish()) {
                System.exit(1);
            }
        } catch (Exception ex) {
            System.err.println("WSDLToJS Error : " + ex.getMessage());
            System.err.println();
            if (w2j.isVerbose()) {
                ex.printStackTrace();
            }
            if (w2j.isExitOnFinish()) {
                System.exit(1);
            }
        }
        if (w2j.isExitOnFinish()) {
            System.exit(0);
        }
    }

    private static InputStream getResourceAsStream(Class clz, String file) {
        return clz.getResourceAsStream(file);
    }
}
