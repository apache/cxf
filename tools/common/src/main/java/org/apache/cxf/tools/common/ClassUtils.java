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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
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
        Compiler compiler = (Compiler)context.get(ToolConstants.COMPILER);
        if (compiler == null) {
            compiler = new Compiler();
        }

        if (context.isVerbose()) {
            compiler.setVerbose(true);
        }

        compiler.setEncoding((String)context.get(ToolConstants.CFG_ENCODING));

        if (context.get(ToolConstants.CFG_CLASSDIR) != null) {
            compiler.setOutputDir((String)context.get(ToolConstants.CFG_CLASSDIR));
        }

        String javaClasspath = System.getProperty("java.class.path");
        if (javaClasspath != null) {
            if (context.get(ToolConstants.CFG_OUTPUTDIR) != null) {
                compiler.setClassPath(javaClasspath + File.pathSeparatorChar
                            + context.get(ToolConstants.CFG_OUTPUTDIR));
            } else {
                compiler.setClassPath(javaClasspath);
            }
        }

        String outPutDir = (String)context.get(ToolConstants.CFG_OUTPUTDIR);

        Set<String> dirSet = new HashSet<>();
        ClassCollector classCollector = context.get(ClassCollector.class);
        List<String> fileList = new ArrayList<>();
        Iterator<String> ite = classCollector.getGeneratedFileInfo().iterator();
        while (ite.hasNext()) {
            String fileName = ite.next();
            fileName = fileName.replace('.', File.separatorChar);
            String dirName = fileName.substring(0, fileName.lastIndexOf(File.separator) + 1);

            String path = outPutDir + File.separator + dirName;
            if (!dirSet.contains(path)) {

                dirSet.add(path);
                File file = new File(path);
                if (file.isDirectory() && file.list() != null) {
                    for (String str : file.list()) {
                        if (str.endsWith("java")) {
                            fileList.add(path + str);
                        } else {
                            // copy generated xml file or others to class directory
                            File otherFile = new File(path + File.separator + str);
                            String suffix = "xml";
                            if (otherFile.isFile()
                                && str.regionMatches(true, str.length() - suffix.length(), suffix, 0, suffix.length())
                                && context.get(ToolConstants.CFG_CLASSDIR) != null) {
                                String targetDir = (String)context.get(ToolConstants.CFG_CLASSDIR);

                                File targetFile = new File(targetDir + File.separator + dirName
                                                           + File.separator + str);
                                try {
                                    copyXmlFile(otherFile, targetFile);

                                } catch (IOException e) {
                                    Message msg = new Message("FAIL_TO_COPY_GENERATED_RESOURCE_FILE", LOG);
                                    throw new ToolException(msg, e);
                                }
                            }
                        }
                    }
                    // JAXB plugins will generate extra files under the runtime directory
                    // Those files can not be allocated into the ClassCollector
                    File jaxbRuntime = new File(path, "runtime");
                    if (jaxbRuntime.isDirectory() && jaxbRuntime.exists()) {
                        List<File> files = FileUtils.getFilesUsingSuffix(jaxbRuntime, ".java");
                        files.forEach(f -> fileList.add(f.toString()));
                    }
                }
            }

        }

        if (!compiler.compileFiles(fileList.toArray(new String[0]))) {
            Message msg = new Message("FAIL_TO_COMPILE_GENERATE_CODES", LOG);
            throw new ToolException(msg);
        }
    }

    private void copyXmlFile(File from, File to) throws ToolException, IOException {

        String dir = to.getCanonicalPath()
                .substring(0, to.getCanonicalPath().lastIndexOf(File.separator));
        File dirFile = new File(dir);
        dirFile.mkdirs();
        try (InputStream input = Files.newInputStream(from.toPath());
            OutputStream output = Files.newOutputStream(to.toPath())) {
            byte[] b = new byte[1024 * 3];
            int len = 0;
            while (len != -1) {
                len = input.read(b);
                if (len != -1) {
                    output.write(b, 0, len);
                }
            }
            output.flush();
        } catch (Exception e) {
            Message msg = new Message("FAIL_TO_COPY_GENERATED_RESOURCE_FILE", LOG);
            throw new ToolException(msg, e);
        }
    }
}
