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
package org.apache.cxf.aegis.inheritance.ws1;

/**
 * <br/>
 * 
 * @author xfournet
 */
public class WS1ExtendedException extends WS1Exception {
    
    private int extendedCode;

    public WS1ExtendedException() {
        extendedCode = 0;
    }

    public WS1ExtendedException(String message) {
        super(message);
        extendedCode = 0;
    }

    public WS1ExtendedException(String message,
                                int errorCode1,
                                int extendedCode,
                                Object object) {
        super(message, errorCode1, object);
        this.extendedCode = extendedCode;
    }

    public void setExtendedCode(int extendedCode) {
        this.extendedCode = extendedCode;
    }

    public int getExtendedCode() {
        return extendedCode;
    }

    public String toString() {
        return super.toString() + "; extendedCode=" + extendedCode;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final WS1ExtendedException that = (WS1ExtendedException)o;

        if (extendedCode != that.extendedCode) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + extendedCode;
        return result;
    }
}
