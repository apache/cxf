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
package org.apache.cxf.ant.extensions;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.tools.wsdlto.WSDLToJava;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
/**
 * Ant task for wsdl2java
 */
public class WSDL2JavaTask extends CxfAntTask {

    /* 
     * For reference, here's the usage message from the command-line.
     *  wsdl2java -fe <frontend name>* 
     *  -db <data binding name>* 
     *  -wv <[wsdl version]>* 
     *  -p <[wsdl namespace =]Package Name>* 
     *  -sn <service-name> -b <binding-name>* 
     *  -catalog <catalog-file-name> 
     *  -d <output-directory> 
     *  -compile -classdir <compile-classes-directory> 
     *  -impl -server -client -all 
     *  -defaultValues<=class name for DefaultValueProvider> 
     *  -ant 
     *  -nexclude <schema namespace [= java packagename]>* 
     *  -exsh <enable extended soap header message binding (true, false)> 
     *  -dns <Default value is true> 
     *  -dex <Default value is true> -validate -keep 
     *  -wsdlLocation <wsdlLocation attribute> -xjc<xjc arguments> 
     *  -noAddressBinding -h -v -verbose -quiet <wsdlurl> 
     */
    
    private String wsdlLocation;
    private String wsdl;
    private Set<File> bindingFiles = new HashSet<File>();

    public void setWsdlLocation(String w) {
        wsdlLocation = w;
    }
    public void setWsdl(String w) {
        wsdl = w;
    }
    public void addConfiguredBinding(FileSet fs) {
        DirectoryScanner ds = fs.getDirectoryScanner(getProject());
        String[] includedFiles = ds.getIncludedFiles();
        File baseDir = ds.getBasedir();
        for (int i = 0; i < includedFiles.length; ++i) {
            bindingFiles.add(new File(baseDir, includedFiles[i]));
        }
    }

    public void execute() throws BuildException {
        buildCommandLine();

        LogStreamHandler log = new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_WARN);
        Execute exe = new Execute(log);
        exe.setAntRun(getProject());
        exe.setCommandline(cmd.getCommandline());
        try {
            int rc = exe.execute();
            if (exe.killedProcess()
                || rc != 0) {
                throw new BuildException("wsdl2java failed", getLocation());
            }
        } catch (IOException e) {
            throw new BuildException(e, getLocation());
        }
    }

    private void buildCommandLine() {
        ClassLoader loader = this.getClass().getClassLoader();
        Path classpath = new Path(getProject());
        if (loader instanceof AntClassLoader) {
            classpath = new Path(getProject(), ((AntClassLoader)loader).getClasspath());
        }
        cmd.createClasspath(getProject()).append(classpath);
        cmd.createVmArgument().setLine("-Djava.util.logging.config.file=");

        cmd.setClassname(WSDLToJava.class.getName());


        if (null != classesDir
            && !StringUtils.isEmpty(classesDir.getName())) {
            cmd.createArgument().setValue("-classdir");
            cmd.createArgument().setFile(classesDir);
            cmd.createArgument().setValue("-compile");
        }
        if (null != sourcesDir
            && !StringUtils.isEmpty(sourcesDir.getName())) {
            cmd.createArgument().setValue("-d");
            cmd.createArgument().setFile(sourcesDir);
        }

        // verbose option
        if (verbose) {
            cmd.createArgument().setValue("-verbose");
        }

        if (!bindingFiles.isEmpty()) {
            for (File b : bindingFiles) {
                cmd.createArgument().setValue("-b");
                cmd.createArgument().setFile(b);
            }
        }

        if (!StringUtils.isEmpty(wsdlLocation)) {
            cmd.createArgument().setValue("-wsdlLocation");
            cmd.createArgument().setValue(wsdlLocation);
        }

        // wsdl
        if (!StringUtils.isEmpty(wsdl)) {
            cmd.createArgument().setValue(wsdl);
        }
    }
}