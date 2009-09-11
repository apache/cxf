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
package org.apache.cxf.systest.jaxws;

import org.apache.cxf.tests.inherit.Inherit;
import org.apache.cxf.tests.inherit.objects.BaseType;
import org.apache.cxf.tests.inherit.objects.SubTypeA;
import org.apache.cxf.tests.inherit.objects.SubTypeB;
import org.apache.cxf.tests.inherit.types.ObjectInfo;

public class InheritImpl implements Inherit {

    public ObjectInfo getObject(int type) {
        ObjectInfo info = new ObjectInfo();
        info.setType("Type: " + type);
        BaseType ba = null;
        switch (type) {
        case 0:
            ba = new SubTypeA();
            ba.setName("A");
            ((SubTypeA)ba).setAvalue("A");
            break;
        case 1:
            ba = new SubTypeB();
            ba.setName("B");
            ((SubTypeB)ba).setBvalue("B");
            break;
        default:
        }
        info.setBaseObject(ba);
        return info;
    }

}
