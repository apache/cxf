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

package org.apache.cxf.javascript.fortest;

import javax.jws.WebService;

@WebService(targetNamespace = "uri:cxf.apache.org.javascript.rpc",
            endpointInterface = "org.apache.cxf.javascript.fortest.SimpleRPC")
//@org.apache.cxf.feature.Features(features = "org.apache.cxf.feature.LoggingFeature")              
public class SimpleRPCImpl implements SimpleRPC {
    
    private String lastString;
    private int lastInt;
    private TestBean1 lastBean;
    
    public void resetLastValues() {
        lastString = null;
        lastInt = -1;
        lastBean = null;
    }

    public void returnVoid(String p1, int p2) {
        lastString = p1;
        lastInt = p2;
    }

    public String simpleType(String p1, int p2) {
        lastString = p1;
        lastInt = p2;
        return lastString;
    }

    public void beanType(TestBean1 p1) {
        lastBean = p1;
    }

    public String getLastString() {
        return lastString;
    }

    public void setLastString(String lastString) {
        this.lastString = lastString;
    }

    public int getLastInt() {
        return lastInt;
    }

    public void setLastInt(int lastInt) {
        this.lastInt = lastInt;
    }

    public TestBean1 getLastBean() {
        return lastBean;
    }

    public void setLastBean(TestBean1 lastBean) {
        this.lastBean = lastBean;
    }

}
