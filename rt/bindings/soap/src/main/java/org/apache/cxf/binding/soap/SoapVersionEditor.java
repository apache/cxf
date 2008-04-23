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
package org.apache.cxf.binding.soap;

import java.beans.PropertyEditorSupport;

public class SoapVersionEditor extends PropertyEditorSupport {

    private SoapVersion version;

    @Override
    public Object getValue() {
        return version;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if ("1.2".equals(text)) {
            this.version = Soap12.getInstance();
        } else if ("1.1".equals(text)) {
            this.version = Soap11.getInstance();
        } else {
            super.setAsText(text);
        }
    }

}
