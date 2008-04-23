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

public class TestCommand {
    
    private int result;
    private int duration;
    private String err;
    private String out;
    
    public TestCommand(String[] args) {
        int i = 0;
        while (i < args.length) {
            if ("-duration".equals(args[i]) && i < (args.length - 1)) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (NumberFormatException ex) {
                    // leave at default
                }
            } else if ("-result".equals(args[i]) && i < (args.length - 1)) {
                i++;
                try {
                    result = Integer.parseInt(args[i]);
                } catch (NumberFormatException ex) {
                    // leave at default
                } 
            } else if ("-err".equals(args[i]) && i < (args.length - 1)) {
                i++;               
                err = args[i];             
            } else if ("-out".equals(args[i]) && i < (args.length - 1)) {
                i++;
                out = args[i];
            } else {
                result = -1;
                System.err.println("Usage: TestCommand [-duration <duration>] [-result <result>]" 
                                   + "                   [-out <out>] [-err <err>]");
                break;
            }
            i++;
        }
    }
    
    void execute() {
       
        if (null != out) {
            System.out.println(out);
        }
        if (null != err) {
            System.err.println(err);
        }
        try {
            Thread.sleep(duration * 1000);
        } catch (InterruptedException ex) {
            // ignore
        }
        System.exit(result); 
    }
    
    public static void main(String[] args) {
        TestCommand tc = new TestCommand(args);
        tc.execute();
    }
}
