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

package org.apache.cxf.tools.corba.utils;

import java.io.InputStream;

import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.apache.cxf.tools.common.toolspec.parser.CommandLineParser;

public class TestUtils {
    private String mUsage;
    private String mDetailedUsage;

    public TestUtils() {
        
    }
    
    public TestUtils(String toolName, InputStream in) throws Exception {
        ToolSpec spec = new ToolSpec(in, false);
        CommandLineParser parser = new CommandLineParser(spec);
        String usage = parser.getUsage();
        mUsage = "Usage : " + toolName + " " + usage;
        mDetailedUsage = toolName + " " + usage + System.getProperty("line.separator")
                          + System.getProperty("line.separator");
        mDetailedUsage += "Options : " + System.getProperty("line.separator") + parser.getDetailedUsage()
                           + System.getProperty("line.separator");
        mDetailedUsage += parser.getToolUsage() + System.getProperty("line.separator")
                           + System.getProperty("line.separator");
    }
    
    public void init() {
        
    }

    public String getUsage() throws Exception {
        return mUsage;
    }

    public String getToolHelp() throws Exception {
        return mDetailedUsage;
    }

}
