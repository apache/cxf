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

package org.apache.cxf.tools.common.toolspec.parser;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.*;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.toolspec.ToolSpec;

public class CommandDocument {
    private static final Logger LOG = LogUtils.getL7dLogger(CommandDocument.class);

    private final Document doc;
    private final ToolSpec toolspec;
    private final List<Object> values;

    CommandDocument(ToolSpec ts, Document d) {

        if (ts == null) {
            throw new NullPointerException("CommandDocument cannot be created with a null toolspec");
        }
        this.toolspec = ts;

        if (d == null) {
            throw new NullPointerException("CommandDocument cannot be created with a null document");
        }
        values = new ArrayList<Object>();
        this.doc = d;
        NodeList nl = doc.getDocumentElement().getElementsByTagName("option");

        for (int i = 0; i < nl.getLength(); i++) {
            values.add(nl.item(i));
        }
        nl = doc.getDocumentElement().getElementsByTagName("argument");
        for (int i = 0; i < nl.getLength(); i++) {
            values.add(nl.item(i));
        }
    }

    public Document getDocument() {
        return doc;
    }

    public boolean hasParameter(String name) {
        return getParameters(name).length > 0;
    }

    public String getParameter(String name) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Getting parameter " + name);
        }
        String[] res = getParameters(name);

        if (res.length == 0) {
            return null;
        }
        return res[0];
    }

    public String[] getParameters(String name) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Getting parameters for " + name);
        }
        List<Object> result = new ArrayList<Object>();

        if (values != null) {
            for (Iterator it = values.iterator(); it.hasNext();) {
                Element el = (Element)it.next();

                if (el.getAttribute("name").equals(name)) {
                    if (el.hasChildNodes()) {
                        result.add(el.getFirstChild().getNodeValue());
                    } else {
                        result.add("true");
                    }
                }
            }
        }
        if (result.isEmpty()) {
            String def = toolspec.getParameterDefault(name);

            if (def != null) {
                result.add(def);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public String[] getParameterNames() {
        List<Object> result = new ArrayList<Object>();

        if (values != null) {
            for (Iterator it = values.iterator(); it.hasNext();) {
                Element el = (Element)it.next();

                result.add(el.getAttribute("name"));
            }
        }
        return result.toArray(new String[result.size()]);
    }

}
