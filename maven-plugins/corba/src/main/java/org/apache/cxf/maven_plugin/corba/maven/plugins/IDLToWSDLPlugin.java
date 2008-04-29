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

package org.apache.cxf.maven_plugin.corba.maven.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.tools.corba.IDLToWSDL;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;


/**
 * @goal idl2wsdl
 * @description CXF IDL To WSDL Tool
 */
public class IDLToWSDLPlugin extends AbstractMojo {

    /**
     * @parameter  expression="${project.build.directory}/generated/src/main/java"
     * @required
     */
    String outputDir;
    
    /**
     * @parameter
     */
    IdltowsdlOption idltowsdlOptions[];


    public void execute() throws MojoExecutionException {
        File outputDirFile = new File(outputDir);
        outputDirFile.mkdirs();
        
        if (idltowsdlOptions == null) {
            throw new MojoExecutionException("Please specify the idltowsdl options");
        }

        for (int x = 0; x < idltowsdlOptions.length; x++) {
            File file = new File(idltowsdlOptions[x].getIDL());
            File doneFile = new File(outputDirFile, "." + file.getName() + ".DONE");

            boolean doWork = file.lastModified() > doneFile.lastModified();
            if (!doneFile.exists()) {
                doWork = true;
            } else if (file.lastModified() > doneFile.lastModified()) {
                doWork = true;
            }

            if (doWork) {
                List<Object> list = new ArrayList<Object>();
                list.add("-o");
                list.add(outputDir);
                list.addAll(idltowsdlOptions[x].getExtraargs());
                list.add(idltowsdlOptions[x].getIDL());            
                try {
                    IDLToWSDL.run((String[])list.toArray(new String[list.size()]));
                    doneFile.delete();
                    doneFile.createNewFile();
                } catch (Throwable e) {
                    e.printStackTrace();
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }
        }
    }

}
