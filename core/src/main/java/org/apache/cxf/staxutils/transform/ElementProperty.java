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

/**
 *
 */
class ElementProperty {
    private QName name;
    private String text;
    private boolean child;

    ElementProperty(QName name, String text, boolean child) {
        this.name = name;
        this.text = text;
        this.child = child;
    }

    /**
     * @return Returns the name.
     */
    public QName getName() {
        return name;
    }

    /**
     * @return Returns the text.
     */
    public String getText() {
        return text;
    }

    /**
     * @return Returns the child.
     */
    public boolean isChild() {
        return child;
    }
}
