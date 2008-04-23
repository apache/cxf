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

import javax.xml.namespace.QName;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamReader;

public class StaxStreamFilter implements StreamFilter {
    private QName[] tags;

    public StaxStreamFilter(QName... eventsToReject) {
        tags = eventsToReject;
    }

    public boolean accept(XMLStreamReader reader) {
        if (reader.isStartElement()) {
            QName elName = reader.getName();
            for (QName tag : tags) {
                if (elName.equals(tag)) {
                    return false;
                }
            }
        }

        if (reader.isEndElement()) {
            QName elName = reader.getName();
            for (QName tag : tags) {
                if (elName.equals(tag)) {
                    return false;
                }
            }
        }
        return true;
    }
}
