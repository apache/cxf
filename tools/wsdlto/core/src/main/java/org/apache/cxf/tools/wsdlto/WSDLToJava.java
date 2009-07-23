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

package org.apache.cxf.tools.wsdlto;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.tools.common.CommandInterfaceUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolContainer;
import org.apache.cxf.tools.common.toolspec.ToolRunner;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.core.FrontEndProfile;
import org.apache.cxf.tools.wsdlto.core.PluginLoader;

public class WSDLToJava {


    public static final String DEFAULT_FRONTEND_NAME = "jaxws";
    public static final String DEFAULT_DATABINDING_NAME = "jaxb";

    private String[] args;
    private PrintStream out = System.out;

    private PluginLoader pluginLoader = PluginLoader.getInstance();

    public WSDLToJava() {
        args = new String[0];
    }
    public WSDLToJava(String pargs[]) {
        args = pargs;
    }

    private FrontEndProfile loadFrontEnd(String name) {
        if (StringUtils.isEmpty(name)) {
            name = DEFAULT_FRONTEND_NAME;
        }
        if (isVerbose()) {
            out.println("Loading FrontEnd " + name + " ...");
        }
        return pluginLoader.getFrontEndProfile(name);
    }

    private DataBindingProfile loadDataBinding(String name) {
        if (StringUtils.isEmpty(name)) {
            name = DEFAULT_DATABINDING_NAME;
        }
        if (isVerbose()) {
            out.println("Loading DataBinding " + name + " ...");
        }
        return pluginLoader.getDataBindingProfile(name);
    }

    private boolean isExitOnFinish() {
        String exit = System.getProperty("exitOnFinish");
        if (StringUtils.isEmpty(exit)) {
            return false;
        }
        return "YES".equalsIgnoreCase(exit) || "TRUE".equalsIgnoreCase(exit);
    }
    
    public void run(ToolContext context) throws Exception {
        run(context, null);
    }

    public void run(ToolContext context, OutputStream os) throws Exception {
        if (os != null) {
            this.out = (os instanceof PrintStream) ? (PrintStream)os : new PrintStream(os);
        }
        FrontEndProfile frontend = null;
        if (args != null) {
            context.put(ToolConstants.CFG_CMD_ARG, args);
            frontend = loadFrontEnd(getFrontEndName(args));
        } else {
            frontend = loadFrontEnd("");
        }


        context.put(FrontEndProfile.class, frontend);

        DataBindingProfile databinding = loadDataBinding(getDataBindingName(args));


        context.put(DataBindingProfile.class, databinding);

        Class<? extends ToolContainer> containerClass = frontend.getContainerClass();

        InputStream toolspecStream = getResourceAsStream(containerClass, frontend.getToolspec());

        ToolRunner.runTool(containerClass,
                           toolspecStream,
                           false,
                           args,
                           isExitOnFinish(),
                           context,
                           os);
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

    private String parseArguments(String[] pargs, String key) {
        if (pargs == null) {
            return null;
        }
        List<String> largs = Arrays.asList(pargs);

        int index = 0;
        if (largs.contains(key)) {
            index = largs.indexOf(key);
            if (index + 1 < largs.size()) {
                return largs.get(index + 1);
            }
        }
        return null;
    }

    private String getOptionValue(String[] pargs, String[] keys) {
        for (String key : keys) {
            String value = parseArguments(pargs, key);
            if (!StringUtils.isEmpty(value)) {
                return value.trim();
            }
        }
        return null;
    }

    protected String getFrontEndName(String[] pargs) {
        return getOptionValue(pargs, new String[]{"-frontend", "-fe"});
    }

    protected String getDataBindingName(String[] pargs) {
        return getOptionValue(pargs, new String[]{"-databinding", "-db"});
    }

    public void setArguments(String[] pargs) {
        args = pargs;
    }

    public static void main(String[] pargs) {

        CommandInterfaceUtils.commandCommonMain();
        WSDLToJava w2j = new WSDLToJava(pargs);
        try {

            w2j.run(new ToolContext());

        } catch (ToolException ex) {
            System.err.println();
            System.err.println("WSDLToJava Error: " + ex.getMessage());
            System.err.println();
            if (w2j.isVerbose()) {
                ex.printStackTrace();
            }
            if (w2j.isExitOnFinish()) {
                System.exit(1);
            }
        } catch (Exception ex) {
            System.err.println("WSDLToJava Error: " + ex.getMessage());
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
