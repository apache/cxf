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

package org.apache.cxf.tools.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.toolspec.AbstractToolContainer;
import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.apache.cxf.tools.common.toolspec.parser.BadUsageException;
import org.apache.cxf.tools.common.toolspec.parser.CommandDocument;
import org.apache.cxf.tools.common.toolspec.parser.CommandLineParser;
import org.apache.cxf.tools.common.toolspec.parser.ErrorVisitor;
import org.apache.cxf.version.Version;

/**
 * Common processing for the CXF tools. Processes common options.
 */
public abstract class AbstractCXFToolContainer extends AbstractToolContainer {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractCXFToolContainer.class);
    
    private final String name;
    private CommandDocument commandDocument;
    private boolean verbose;
    private String usage;
    private final ErrorVisitor errors = new ErrorVisitor();
    private String beanConfigResource;
    
    
    public AbstractCXFToolContainer(String nm, ToolSpec toolspec) throws Exception {
        super(toolspec);
        name = nm;
    }

    public boolean hasInfoOption() throws ToolException {
        commandDocument = getCommandDocument();
        if (commandDocument == null) {
            return false;
        }
        if ((commandDocument.hasParameter("help")) || (commandDocument.hasParameter("version"))) {
            return true;
        }
        return false;
    }

    public void execute(boolean exitOnFinish) throws ToolException {
        super.execute(exitOnFinish);
        if (hasInfoOption()) {
            outputInfo();
        } else {
            if (commandDocument.hasParameter(ToolConstants.CFG_VERBOSE)) {
                verbose = true;
                outputFullCommandLine();
                outputVersion();               
            }
            checkParams(errors);
        }             
    }
    
    private void outputInfo() {
        CommandLineParser parser = getCommandLineParser();

        if (commandDocument.hasParameter("help")) {
            try {
                out.println(name + " " + getUsage());
                out.println();
                out.println("Options: ");
                out.println();
                out.println(parser.getFormattedDetailedUsage());
                String toolUsage = parser.getToolUsage();
                if (toolUsage != null) {
                    out.println(toolUsage);
                }
            } catch (Exception ex) {
                err.println("Error: Could not output detailed usage");
                err.println();
            }
        }
        if (commandDocument.hasParameter("version")) {
            outputVersion();
        }
    }

    /**
     * Check command-line parameters for validity. Since subclasses delegate down to here,
     * this cannot complain about unwanted options.
     * @param err place to report errors.
     * @throws ToolException for impossible options.
     */
    public void checkParams(ErrorVisitor err) throws ToolException {
        CommandDocument doc = getCommandDocument();

        if (doc.hasParameter(ToolConstants.CFG_BEAN_CONFIG)) {
            String beanPath = doc.getParameter(ToolConstants.CFG_BEAN_CONFIG);           
            setBeanConfigResource(beanPath);
        }
    }

    public boolean isVerboseOn() {
        if (context != null && context.isVerbose()) {
            return true;
        }
        return verbose;
    }

    public String getToolName() {
        return name;
    }

    public String getUsage() {
        if (usage == null) {
            try {
                CommandLineParser parser = getCommandLineParser();

                if (parser != null) {
                    usage = parser.getUsage();
                }
            } catch (Exception ex) {
                usage = "Could not get usage for the tool";
            }
        }
        return usage;
    }

    public void outputVersion() {
        out.println(name + " - " + Version.getCompleteVersionString());
        out.println();
    }

    public void outputFullCommandLine() {
        out.print(name);
        for (int i = 0; i < getArgument().length; i++) {
            out.print(" " + getArgument()[i]);
        }
        out.println();
    }
    
    public String getFileBase(String wsdlUrl) {
        String fileBase = wsdlUrl;
        StringTokenizer tok = new StringTokenizer(wsdlUrl, "\\/");

        while (tok.hasMoreTokens()) {
            fileBase = tok.nextToken();
        }
        if (fileBase.endsWith(".wsdl")) {
            fileBase = new String(fileBase.substring(0, fileBase.length() - 5));
        }
        return fileBase;
    }

    public void printUsageException(String toolName, BadUsageException ex) {
        if (verbose) {
            outputFullCommandLine();
        }
        err.println(ex.getMessage());
        err.println("Usage : " + toolName + " " + ex.getUsage());
        if (verbose) {
            outputVersion();
        }
        err.println();
    }

    public String getFileName(String loc) {
        int idx = loc.lastIndexOf("/");

        if (idx != -1) {
            loc = loc.substring(idx + 1);
        }
        idx = loc.lastIndexOf("\\");
        if (idx != -1) {
            loc = loc.substring(idx + 1);
        }

        idx = loc.lastIndexOf(".");
        if (idx != -1) {
            loc = loc.substring(0, idx);
        }

        StringTokenizer strToken = new StringTokenizer(loc, "-.!~*'();?:@&=+$,");
        StringBuffer strBuf = new StringBuffer();

        if (!strToken.hasMoreTokens()) {
            strBuf.append(loc);
        }

        while (strToken.hasMoreTokens()) {
            strBuf.append(strToken.nextToken());
            if (strToken.countTokens() != 0) {
                strBuf.append("_");
            }
        }

        return strBuf.toString();
    }

    private InputStream getResourceAsStream(String resource) {
        ClassLoader cl = AbstractCXFToolContainer.class.getClassLoader();
        InputStream ins = cl.getResourceAsStream(resource);
        if (ins == null && resource.startsWith("/")) {
            ins = cl.getResourceAsStream(resource.substring(1));
        }
        return ins;
    }
   
    public Properties loadProperties(InputStream inputs) {
        Properties p = new Properties();
        try {
            p.load(inputs);
            inputs.close();
        } catch (IOException ex) {
            // ignore, use defaults
        }
        return p;
    }
    
    
    public Properties loadProperties(String propertyFile) {
        Properties p = new Properties();

        try {
            InputStream ins = getResourceAsStream(propertyFile);

            p.load(ins);
            ins.close();
        } catch (IOException ex) {
            // ignore, use defaults
        }
        return p;
    }

    protected String[] getDefaultExcludedNamespaces(String excludeProps) {
        List<String> result = new ArrayList<String>();
        Properties props = loadProperties(excludeProps);
        java.util.Enumeration nexcludes = props.propertyNames();

        while (nexcludes.hasMoreElements()) {
            result.add(props.getProperty((String)nexcludes.nextElement()));
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * get all parameters in a map
     * @param stringArrayKeys, contains keys, whose value should be string array
     */
    protected Map<String, Object> getParametersMap(Set stringArrayKeys) {
        Map<String, Object> map = new HashMap<String, Object>();
        CommandDocument doc = getCommandDocument();
        if (doc == null) {
            return map;
        }
        String[] keys = doc.getParameterNames();
        if (keys == null) {
            return map;
        }
        for (int i = 0; i < keys.length; i++) {
            if (stringArrayKeys.contains(keys[i])) {
                map.put(keys[i], doc.getParameters(keys[i]));
            } else {
                map.put(keys[i], doc.getParameter(keys[i]));
            }
        }
        return map;
    }

    /**
     * @return Returns the beanConfigResource.
     */
    public String getBeanConfigResource() {
        return beanConfigResource;
    }

    /**
     * @param beanConfigResource The beanConfigResource to set.
     */
    public void setBeanConfigResource(String beanConfigResource) {
        this.beanConfigResource = beanConfigResource;
    }
}
