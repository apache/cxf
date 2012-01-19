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
public class WS1Exception extends Exception {
    private int errorCode;
    private Object simpleBean;

    public WS1Exception() {
        simpleBean = null;
        errorCode = 0;
    }

    public WS1Exception(String message) {
        super(message);
        simpleBean = null;
        errorCode = 0;
    }

    public WS1Exception(String message, int errorCode1) {
        super(message);
        errorCode = errorCode1;
        simpleBean = null;
    }
    public WS1Exception(String message, int errorCode1, Object bean) {
        super(message);
        errorCode = errorCode1;
        simpleBean = bean;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public void setSimpleBean(Object simpleBean) {
        this.simpleBean = simpleBean;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public Object getSimpleBean() {
        return simpleBean;
    }

    public String toString() {
        return "[" + getClass().getName() + "] msg=" + getMessage() + "; errorCode=" + errorCode;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final WS1Exception that = (WS1Exception)o;

        if (getMessage() != null ? !getMessage().equals(that.getMessage()) : that.getMessage() != null) {
            return false;
        }

        if (errorCode != that.errorCode) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return errorCode;
    }
}
