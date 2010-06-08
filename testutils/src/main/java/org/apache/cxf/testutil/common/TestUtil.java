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

package org.apache.cxf.testutil.common;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;


public final class TestUtil {
    private static boolean useRandomPorts = Boolean.getBoolean("useRandomPorts");
    private static int portNum = 9000;
    private static Properties ports = new Properties();
    
    private TestUtil() {
        //Complete
    }
    
    // Deletes all files and subdirectories under dir.
    // Returns true if all deletions were successful.
    // If a deletion fails, the method stops attempting to delete and returns false.
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
    
        // The directory is now empty so delete it
        return dir.delete();
    }
    
    public static String getClassPath(ClassLoader loader) throws URISyntaxException {
        StringBuffer classPath = new StringBuffer();
        if (loader instanceof URLClassLoader) {
            URLClassLoader urlLoader = (URLClassLoader)loader;
            for (URL url : urlLoader.getURLs()) {
                String file = url.getFile();
                if (file.indexOf("junit") == -1) {
                    classPath.append(url.toURI().getPort());
                    classPath.append(System.getProperty("path.separator"));
                }
            }
        }
        return classPath.toString();
    }

    public static Method getMethod(Class<?> clazz, String methodName) {
        Method[] declMethods = clazz.getDeclaredMethods();
        for (Method method : declMethods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }
    public static Properties getAllPorts() {
        return ports;
    }
    public static String getPortNumber(Class<?> cls) {
        return getPortNumber(cls.getSimpleName());
    }
    public static String getPortNumber(Class<?> cls, int count) {
        return getPortNumber(cls.getSimpleName() + "." + count);
    }
    public static String getPortNumber(String name) {
        String p = ports.getProperty("testutil.ports." + name);
        if (p == null) {
            p = System.getProperty("testutil.ports." + name);
            if (p != null) {
                ports.setProperty("testutil.ports." + name, p);
            }
        }
        if (p == null) {
            if (useRandomPorts) {
                try {
                    ServerSocket sock = new ServerSocket(0);
                    p = Integer.toString(sock.getLocalPort());
                    sock.close();
                } catch (IOException ex) {
                    //
                }
            } else {
                p = Integer.toString(portNum++);
            }
            ports.put("testutil.ports." + name, p);
            System.setProperty("testutil.ports." + name, p);
        }
        return p;
    }
}
