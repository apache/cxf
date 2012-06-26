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

package org.apache.cxf.maven_plugin.wsdl2java;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.wsdlto.WSDLToJava;

/**
 * 
 */
public final class ForkOnceWSDL2Java {
    private ForkOnceWSDL2Java() {
        //utility
    }
    public static void main(String args[]) throws Exception {
        File file = new File(args[0]);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        while (line != null) {
            int i = Integer.parseInt(line);
            if (i == -1) {
                reader.close();
                return;
            }
            String wargs[] = new String[i];
            for (int x = 0; x < i; x++) {
                wargs[x] = reader.readLine();
            }
            
            new WSDLToJava(wargs).run(new ToolContext());
            
            line = reader.readLine();
        }
        reader.close();
    }
}
