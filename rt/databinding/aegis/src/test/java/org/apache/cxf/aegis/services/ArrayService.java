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
package org.apache.cxf.aegis.services;

/**
 * An array service for testing.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class ArrayService {
    
    private org.jdom.Element[] jdomArray;
    private org.w3c.dom.Document[] w3cArray;
    private String beforeValue;
    private String afterValue;
    
    public SimpleBean[] getBeanArray() {
        SimpleBean bean = new SimpleBean();
        bean.setBleh("bleh");
        bean.setHowdy("howdy");

        return new SimpleBean[] {bean};
    }
    
    public void resetValues() {
        beforeValue = null;
        afterValue = null;
        jdomArray = null;
        w3cArray = null;
    }

    public String[] getStringArray() {
        return new String[] {"bleh", "bleh"};
    }

    public boolean submitStringArray(String[] array) {
        return true;
    }

    public boolean submitBeanArray(SimpleBean[] array) {
        return true;
    }
    
    public void submitJDOMArray(String before, org.jdom.Element[] anything, String after) {
        beforeValue = before;
        jdomArray = anything;
        afterValue = after;
    }

    public void submitW3CArray(String before, org.w3c.dom.Document[] anything, String after) {
        beforeValue = before;
        w3cArray = anything;
        afterValue = after;
    }

    public org.jdom.Element[] getJdomArray() {
        return jdomArray;
    }

    public org.w3c.dom.Document[] getW3cArray() {
        return w3cArray;
    }

    public String getBeforeValue() {
        return beforeValue;
    }

    public String getAfterValue() {
        return afterValue;
    }
}
