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

package org.apache.cxf.maven_plugin;

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class CodegenUtils {
    
    static long timestamp;
    
    private CodegenUtils() {
        //not consructed
    }
    
    public static long getCodegenTimestamp() {
        if (timestamp != 0) {
            return timestamp;
        }
        
        getClassTime(CodegenUtils.class);
        
        return timestamp;
    }

    private static void getClassTime(Class class1) {
        String str = "/" + class1.getName().replace('.', '/') + ".class";
        URL url = class1.getResource(str);
        if (url != null) {
            while ("jar".equals(url.getProtocol())) {
                str = url.getPath();
                if (str.lastIndexOf("!") != -1) {
                    str = str.substring(0, str.lastIndexOf("!"));
                }
                try {
                    url = new URL(str);
                } catch (MalformedURLException e) {
                    return;
                }
            }
            
            try {
                if (url.getPath().endsWith(".class")) {
                    timestamp = new File(url.toURI()).lastModified();
                } else {
                    JarFile jar = new JarFile(url.getPath());
                    Enumeration entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = (JarEntry)entries.nextElement();
                        if (!entry.isDirectory()
                            && !entry.getName().startsWith("META")
                            && entry.getTime() > timestamp) {
                            
                            timestamp = entry.getTime();
                        }                    
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
