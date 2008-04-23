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

package org.apache.cxf.phase;

public class Phase implements Comparable {
    
    // can be removed from once defined as default value in configuration metadata for bus
  
    public static final String SETUP = "setup";
    public static final String SETUP_ENDING = "setup-ending";
    public static final String PRE_LOGICAL = "pre-logical";
    public static final String PRE_LOGICAL_ENDING = "pre-logical-ending";
    public static final String USER_LOGICAL = "user-logical";
    public static final String USER_LOGICAL_ENDING = "user-logical-ending";
    public static final String POST_LOGICAL = "post-logical";
    public static final String POST_LOGICAL_ENDING = "post-logical-ending";
    public static final String PRE_MARSHAL = "pre-marshal";
    public static final String MARSHAL = "marshal";
    public static final String POST_MARSHAL = "post-marshal";
    public static final String MARSHAL_ENDING = "marshal-ending";
    public static final String PRE_PROTOCOL = "pre-protocol";
    public static final String PRE_PROTOCOL_ENDING = "pre-protocol-ending";
    public static final String USER_PROTOCOL = "user-protocol";
    public static final String USER_PROTOCOL_ENDING = "user-protocol-ending";
    public static final String POST_PROTOCOL = "post-protocol";
    public static final String POST_PROTOCOL_ENDING = "post-protocol-ending";
    public static final String PREPARE_SEND = "prepare-send";
    public static final String PREPARE_SEND_ENDING = "prepare-send-ending";
    public static final String PRE_STREAM = "pre-stream";
    public static final String PRE_STREAM_ENDING = "pre-stream-ending";
    public static final String USER_STREAM = "user-stream";
    public static final String USER_STREAM_ENDING = "user-stream-ending";
    public static final String POST_STREAM = "post-stream";
    public static final String POST_STREAM_ENDING = "post-stream-ending";
    public static final String WRITE = "write";
    public static final String WRITE_ENDING = "write-ending";
    public static final String SEND = "send";
    public static final String SEND_ENDING = "send-ending";
   
    public static final String RECEIVE = "receive";
    public static final String READ = "read";
    public static final String PROTOCOL = "protocol";
    public static final String UNMARSHAL = "unmarshal";
    public static final String PRE_INVOKE = "pre-invoke";
    public static final String INVOKE = "invoke";
    public static final String POST_INVOKE = "post-invoke";
    
    
    private String name;
    private int priority;
    
    public Phase() {
    }
    
    public Phase(String n, int p) {
        this.name = n;
        this.priority = p;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String n) {
        this.name = n;
    }
    public int getPriority() {
        return priority;
    }
    public void setPriority(int p) {
        this.priority = p;
    }
    
    public int hashCode() {
        return priority;
    }
    public boolean equals(Object o) {
        Phase p = (Phase)o;
        
        return p.priority == priority
            && p.name.equals(name);
    }

    public int compareTo(Object o) {
        Phase p = (Phase)o;
        
        if (priority == p.priority) {
            return name.compareTo(p.name); 
        }
        return priority - p.priority;
    }
    
    public String toString() {
        return "Phase(" + getName() + ")";
    }
}
