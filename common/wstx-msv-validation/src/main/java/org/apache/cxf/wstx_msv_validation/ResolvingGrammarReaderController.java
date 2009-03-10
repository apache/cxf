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

package org.apache.cxf.wstx_msv_validation;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import com.sun.msv.reader.GrammarReaderController;

import org.apache.cxf.common.logging.LogUtils;

/**
 * Catch error messages and resolve schema locations. 
 */
public class ResolvingGrammarReaderController implements GrammarReaderController {
    private static final Logger LOG = LogUtils.getL7dLogger(ResolvingGrammarReaderController.class);

    private Map<String, InputSource> sources;

    public ResolvingGrammarReaderController(Map<String, InputSource> sources) {
        this.sources = sources;
    }

    public void error(Locator[] locs, String msg, Exception nestedException) {
        /* perhaps throw ? */
        LOG.log(Level.SEVERE, msg, nestedException);
        for (Locator loc : locs) {
            LOG.severe("in " + loc.getSystemId() + " " + loc.getLineNumber() + ":" + loc.getColumnNumber());
        }
    }

    public void warning(Locator[] locs, String errorMessage) {
        LOG.log(Level.WARNING, errorMessage);
        for (Locator loc : locs) {
            LOG.warning("in " + loc.getSystemId() + " " + loc.getLineNumber() + ":" + loc.getColumnNumber());
        }

    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        // CXF never trucks with publicId's.
        return sources.get(systemId);
    }
}
