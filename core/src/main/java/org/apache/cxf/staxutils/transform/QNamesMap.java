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

package org.apache.cxf.staxutils.transform;

import javax.xml.namespace.QName;

class QNamesMap {
    private QName[] keys;
    private QName[] values;
    private int index;

    QNamesMap(int size) {
        keys = new QName[size];
        values = new QName[size];
    }

    public void put(QName key, QName value) {
        keys[index] = key;
        values[index] = value;
        index++;
    }

    public QName get(QName key) {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].getNamespaceURI().equals(key.getNamespaceURI())) {
                if (keys[i].getLocalPart().equals(key.getLocalPart())) {
                    return values[i];
                } else if ("*".equals(keys[i].getLocalPart())) {
                    // assume it is something like {somens}* => * or {somens}* => {anotherns}*
                    // and return QName(nsuri, lcname) which covers both cases.
                    return new QName(values[i].getNamespaceURI(), key.getLocalPart());
                }
            }
        }
        return null;
    }

    public int size() {
        return index;
    }
}
