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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class VelocityWriter extends BufferedWriter {
    
    private final String newLine = System.getProperty("line.separator");

    public VelocityWriter(Writer out) {
        super(out);
    }

    public VelocityWriter(Writer out, int size) {
        super(out, size);
    }

    public void write(char[] chars) throws IOException {
        String str = new String(chars);
        if (str.indexOf("\r\n") >= 0 && newLine != null) {
            super.write(str.replaceAll("\r\n", newLine));
            return;
        } else if (str.indexOf("\n") >= 0 && newLine != null) {
            super.write(str.replaceAll("\n", newLine));
            return;
        } else {
            super.write(str);
        }
       
    }
   
    
    
    
    public void write(String str) throws IOException {
        if (str.indexOf("\r\n") >= 0  && newLine != null) {
            super.write(str.replaceAll("\r\n", newLine));
            return;
        } else if (str.indexOf("\n") >= 0  && newLine != null) {
            super.write(str.replaceAll("\n", newLine));
            return;
        } else {
            super.write(str);
        }
    }

}
