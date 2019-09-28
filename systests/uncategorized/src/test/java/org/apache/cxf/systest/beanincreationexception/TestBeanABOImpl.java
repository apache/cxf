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
package org.apache.cxf.systest.beanincreationexception;

public class TestBeanABOImpl implements TestBeanABO {
    private TestBeanABO bean;
    private AddNumbersPortType client;

    public void setBean(TestBeanABO bean) {
        this.bean = bean;
    }

    public void setClient(AddNumbersPortType client) {
        this.client = client;
    }

    public String nothing() {
        return bean.toString() + client.toString();
    }

}
