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

package org.apache.cxf.tools.corba.idlpreprocessor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;

/**
 * Provides information about a file that
 */
class IncludeStackEntry {
    private final URL url;

    private LineNumberReader reader;

    private final String location;

    IncludeStackEntry(URL link, String loc) throws IOException {
        this.url = link;
        this.location = loc;
        this.reader = new LineNumberReader(new InputStreamReader(url.openStream(), "ISO-8859-1"));
        this.reader.setLineNumber(1);
    }

    public String getLocation() {
        return location;
    }

    public URL getURL() {
        return url;
    }

    public LineNumberReader getReader() {
        return reader;
    }

    public String toString() {
        return "IncludeStackEntry[url=" + url + ", location=" + location
            + ", line=" + reader.getLineNumber() + "]";
    }
}
