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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Compiler;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.tools.util.ClassCollector;

public class ClassUtils {
    
    protected static final Logger LOG = LogUtils.getL7dLogger(ClassUtils.class);
    
    public void compile(ToolContext context) throws ToolException {        
        List<String> argList = new ArrayList<String>();
        
        //fix for CXF-2081, set maximum heap of current VM to javac.
        argList.add("-J-Xmx" + Runtime.getRuntime().maxMemory());

        String javaClasspath = System.getProperty("java.class.path");
        // hard code cxf.jar
        boolean classpathSetted = javaClasspath != null ? true : false;
        // && (javaClasspath.indexOf("cxf.jar") >= 0);
        if (context.isVerbose()) {
            argList.add("-verbose");
        }

        if (context.get(ToolConstants.CFG_CLASSDIR) != null) {
            argList.add("-d");
            String classDir = (String)context.get(ToolConstants.CFG_CLASSDIR);
            argList.add(classDir.replace(File.pathSeparatorChar, '/'));
        }

        if (!classpathSetted) {
            argList.add("-extdirs");
            argList.add(getClass().getClassLoader().getResource(".").getFile() + "../lib/");
        } else {
            argList.add("-classpath");
            if (context.get(ToolConstants.CFG_OUTPUTDIR) != null) { 
                argList.add(javaClasspath + File.pathSeparatorChar 
                            + context.get(ToolConstants.CFG_OUTPUTDIR));
            } else {
                argList.add(javaClasspath);
            }
        }

        String outPutDir = (String)context.get(ToolConstants.CFG_OUTPUTDIR);
       
        Set<String> dirSet = new HashSet<String>();
        ClassCollector classCollector = context.get(ClassCollector.class);
        List<String> fileList = new ArrayList<String>();
        Iterator<String> ite = classCollector.getGeneratedFileInfo().iterator();
        while (ite.hasNext()) {
            String fileName = ite.next();
            fileName = fileName.replace('.', File.separatorChar);
            String dirName = fileName.substring(0, fileName.lastIndexOf(File.separator) + 1);
            
            String path = outPutDir + File.separator + dirName;
            if (!dirSet.contains(path)) {

                dirSet.add(path);
                File file = new File(path);
                if (file.isDirectory()) {
                    for (String str : file.list()) {
                        if (str.endsWith("java")) {
                            fileList.add(path + str);
                        } else {
                            // copy generated xml file or others to class directory
                            File otherFile = new File(path + File.separator + str);
                            if (otherFile.isFile() && str.toLowerCase().endsWith("xml")
                                && context.get(ToolConstants.CFG_CLASSDIR) != null) {
                                String targetDir = (String)context.get(ToolConstants.CFG_CLASSDIR);

                                File targetFile = new File(targetDir + File.separator + dirName
                                                           + File.separator + str);
                                copyXmlFile(otherFile, targetFile);
                            }
                        }
                    }
                    // JAXB plugins will generate extra files under the runtime directory
                    // Those files can not be allocated into the ClassCollector
                    File jaxbRuntime = new File(path, "runtime");
                    if (jaxbRuntime.isDirectory() && jaxbRuntime.exists()) {
                        List<File> files = FileUtils.getFiles(jaxbRuntime, ".+\\.java$");
                        for (File f : files) {
                            fileList.add(f.toString());
                        }
                    }
                }
            }

        }
        //Jaxb's bug . Jaxb ClassNameCollecotr may not be invoked when generated class is an enum.
        //So we need recheck whether we add all generated source files to  fileList
        
        String[] arguments = new String[argList.size() + fileList.size() + 1];
        arguments[0] = "javac";
        
        int i = 1;
        
        for (Object obj : argList.toArray()) {
            String arg = (String)obj;
            arguments[i] = arg;
            i++;
        }
        
        int srcFileIndex = i; 
        for (Object o : fileList.toArray()) {
            String file = (String)o;
            arguments[i] = file;
            i++;
        }

        Compiler compiler = new Compiler();

        if (!compiler.internalCompile(arguments, srcFileIndex)) {
            Message msg = new Message("FAIL_TO_COMPILE_GENERATE_CODES", LOG);
            throw new ToolException(msg);
        }        
    }
    
    private void copyXmlFile(File from, File to) throws ToolException {

        try {
            String dir = to.getCanonicalPath()
                .substring(0, to.getCanonicalPath().lastIndexOf(File.separator));
            File dirFile = new File(dir);
            dirFile.mkdirs();
            FileInputStream input = new FileInputStream(from);
            FileOutputStream output = new FileOutputStream(to);
            byte[] b = new byte[1024 * 3];
            int len = 0;
            while (len != -1) {
                len = input.read(b);
                if (len != -1) {
                    output.write(b, 0, len);
                }
            }
            output.flush();
            output.close();
            input.close();
        } catch (Exception e) {
            Message msg = new Message("FAIL_TO_COPY_GENERATED_RESOURCE_FILE", LOG);
            throw new ToolException(msg, e);
        }
    }    
}
