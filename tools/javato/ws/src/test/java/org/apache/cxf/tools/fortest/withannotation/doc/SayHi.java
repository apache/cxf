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

package org.apache.cxf.tools.fortest.withannotation.doc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "sayHi", namespace = "http://doc.withannotation.fortest.tools.cxf.apache.org/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sayHi", namespace = "http://doc.withannotation.fortest.tools.cxf.apache.org/")
public class SayHi {

    @XmlElement(name = "arg0", namespace = "")
    private long arg0;

    /**
     *
     * @return
     * returns long
     */
    public long getArg0() {
        return this.arg0;
    }

    /**
     *
     * @param arg0
     * the value for the arg0 property
     */
    public void setArg0(long arg0) {
        this.arg0 = arg0;
    }
}
