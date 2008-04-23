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

package org.apache.cxf.tools.java2wsdl.generator.wsdl11;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cxf.common.util.Compiler;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.VelocityGenerator;
import org.apache.cxf.tools.common.model.JavaClass;
import org.apache.cxf.tools.java2wsdl.generator.AbstractGenerator;
import org.apache.cxf.tools.util.FileWriterUtil;

public class BeanGenerator extends AbstractGenerator<File> {
    private static final String TEMPLATE = "org/apache/cxf/tools/java2wsdl/generator/wsdl11/wrapperbean.vm";
    private File compileToDir;

    public void setCompileToDir(File f) {
        compileToDir = f;
    }
    
    protected Collection<JavaClass> generateBeanClasses(final ServiceInfo service) {
        return null;
    }
    
    public File generate(final File sourcedir) {
        File dir = getOutputBase();
        if (dir == null) {
            dir = sourcedir;
        }
        if (dir == null) {
            dir = new File("./");
        }
        Collection<JavaClass> wrapperClasses = generateBeanClasses(getServiceModel());

        if (!wrapperClasses.isEmpty()) {
            generateAndCompile(wrapperClasses, dir);
        }

        return dir;
    }

    public void generateAndCompile(Collection<JavaClass> wrapperClasses, File dir) {
        VelocityGenerator generator = new VelocityGenerator(false);
        generator.setBaseDir(dir.toString());

        List<File> generatedFiles = new ArrayList<File>();
        try {
            for (JavaClass wrapperClass : wrapperClasses) {
                generator.setCommonAttributes();
                generator.setAttributes("bean", wrapperClass);
            
                File file = generator.parseOutputName(wrapperClass.getPackageName(),
                                                      wrapperClass.getName());
                generatedFiles.add(file);
            
                generator.doWrite(TEMPLATE, FileWriterUtil.getWriter(file));
            
                generator.clearAttributes();
            }
        
                //compile the classes
            Compiler compiler = new Compiler();

            List<String> files = new ArrayList<String>(generatedFiles.size());
            for (File file : generatedFiles) {
                files.add(file.getAbsolutePath());
            }
            if (!compiler.compileFiles(files.toArray(new String[files.size()]), compileToDir)) {
                // TODO - compile issue
            }

            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
