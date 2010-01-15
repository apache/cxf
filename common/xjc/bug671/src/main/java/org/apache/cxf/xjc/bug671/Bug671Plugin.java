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

package org.apache.cxf.xjc.bug671;


import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.xml.bind.api.impl.NameConverter;

/**
 * Modifies the JAXB code model to handle package naming that run into:
 * https://jaxb.dev.java.net/issues/show_bug.cgi?id=671
 */
public class Bug671Plugin {
    private Plugin plugin;

    public Bug671Plugin(Plugin p) {
        plugin = p;
    }
    
    
    public String getOptionName() {
        return "Xbug671";
    }

    public String getUsage() {
        return "  -Xbug671             : Activate plugin to map package names that contain keywords";
    }

    public void onActivated(Options opt) throws BadCommandLineException {
        // kind of a bogus thing to have to do to workaround bug:
        // https://jaxb.dev.java.net/issues/show_bug.cgi?id=671
        opt.setNameConverter(new NameConverter.Standard() {
            @Override
            public String toPackageName(String nsUri) {
                String s = super.toPackageName(nsUri);
                int idx = s.indexOf('.');
                while (idx != -1) {
                    ++idx;
                    int idx2 = s.indexOf('.', idx);
                    if (idx2 == -1) {
                        idx2 = s.length();
                    }
                    if (!isJavaIdentifier(s.substring(idx, idx2))) {
                        s = s.substring(0, idx) + "_" + s.substring(idx);
                    }
                    idx = s.indexOf('.', idx);
                }
                return s;
            }
            
        }, plugin);
    }
}
