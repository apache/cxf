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

package org.apache.cxf.common.commands;

import java.io.File;

public final class JavaHelper {

    private JavaHelper() {
        //complete
    }

    /** Get the command to launch a JVM.  Find the java command
     * relative to the java.home property rather than what is on the
     * path.  It is possible that the java version being used it not
     * on the path
     *
     */
    public static String getJavaCommand() { 
        String javaHome = System.getProperty("java.home");
        if (null != javaHome) { 
            return javaHome + File.separator + "bin"  
                + File.separator  + "java" + ForkedCommand.EXE_SUFFIX; 
        } else { 
            return "java" + ForkedCommand.EXE_SUFFIX;
        } 
    } 
}
