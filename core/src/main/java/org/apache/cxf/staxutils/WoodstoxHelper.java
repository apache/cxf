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

package org.apache.cxf.staxutils;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import com.ctc.wstx.stax.WstxEventFactory;
import com.ctc.wstx.stax.WstxInputFactory;

import org.codehaus.stax2.XMLStreamReader2;

/**
 *
 */
final class WoodstoxHelper {

    private WoodstoxHelper() {
    }

    public static XMLInputFactory createInputFactory() {
        return new WstxInputFactory();
    }

    public static XMLEventFactory createEventFactory() {
        return new WstxEventFactory();
    }

    public static void setProperty(XMLStreamReader reader, String p, Object v) {
        ((XMLStreamReader2)reader).setProperty(p, v);
    }

}
