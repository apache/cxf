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
package org.apache.cxf.maven_plugin.javatowadl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

public final class DumpJavaDoc {
    
    private DumpJavaDoc() {
        
    }
    
    public static boolean start(RootDoc root) throws IOException {
        String dumpFileName = readOptions(root.options());
        FileOutputStream fos = new FileOutputStream(dumpFileName);
        Properties javaDocMap = new Properties();
        for (ClassDoc classDoc : root.classes()) {
            javaDocMap.put(classDoc.toString(), classDoc.commentText());
            for (MethodDoc method : classDoc.methods()) {
                javaDocMap.put(method.qualifiedName(), method.commentText());
                for (ParamTag paramTag : method.paramTags()) {
                    Parameter[] parameters = method.parameters();
                    for (int i = 0; i < parameters.length; ++i) {
                        if (parameters[i].name().equals(paramTag.parameterName())) {
                            javaDocMap.put(method.qualifiedName() + ".paramCommentTag." + i, 
                                   paramTag.parameterComment());
                        }
                    }
                }
                Tag retTags[] = method.tags("return");
                if (retTags != null && retTags.length == 1) {
                    Tag retTag = method.tags("return")[0];
                    javaDocMap.put(method.qualifiedName() + "." + "returnCommentTag", 
                                   retTag.text());
                }
            }
                
        }
        javaDocMap.store(fos, "");
        fos.flush();
        fos.close();
        return true;
    }
    
    private static String readOptions(String[][] options) {
        String tagName = null;
        for (int i = 0; i < options.length; i++) {
            String[] opt = options[i];
            if (opt[0].equals("-dumpJavaDocFile")) {
                tagName = opt[1];
            }
        }
        return tagName;
    }

    public static int optionLength(String option) {
        if ("-dumpJavaDocFile".equals(option)) {
            return 2;
        }
        return 0;
    }

    public static boolean validOptions(String options[][], DocErrorReporter reporter) {
        boolean foundTagOption = false;
        for (int i = 0; i < options.length; i++) {
            String[] opt = options[i];
            if (opt[0].equals("-dumpJavaDocFile")) {
                if (foundTagOption) {
                    reporter.printError("Only one -dumpJavaDocFile option allowed.");
                    return false;
                } else {
                    foundTagOption = true;
                }
            }
        }
        if (!foundTagOption) {
            reporter.printError("Usage: -dumpJavaDocFile theFileToDumpJavaDocForLatarUse...");
        }
        return foundTagOption;
    }
}
