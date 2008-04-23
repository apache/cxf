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

package org.apache.cxf.tools.corba.common;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;

import junit.framework.TestCase;

public abstract class ToolTestBase extends TestCase {

    protected PrintStream oldStdErr; 
    protected PrintStream oldStdOut; 
    protected URL wsdlLocation; 
    protected URL idlLocation;
    
    protected ByteArrayOutputStream errOut = new ByteArrayOutputStream(); 
    protected ByteArrayOutputStream stdOut = new ByteArrayOutputStream(); 

    public void setUp() { 
        
        oldStdErr = System.err; 
        oldStdOut = System.out;
        
        System.setErr(new PrintStream(errOut));
        System.setOut(new PrintStream(stdOut));
        
        wsdlLocation = ToolTestBase.class.getResource("/wsdl/hello_world.wsdl");
        idlLocation = ToolTestBase.class.getResource("/idl/HelloWorld.idl");
    }
    
    public void tearDown() { 
        
        System.setErr(oldStdErr);
        System.setOut(oldStdOut);
    }
    
    protected String getStdOut() {
        return new String(stdOut.toByteArray());
    }
    protected String getStdErr() {
        return new String(errOut.toByteArray());
    }

}

