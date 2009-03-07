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

package org.apache.cxf.js.rhino;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.ws.Service;

import org.apache.cxf.common.logging.LogUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

public class ProviderFactory {
    public static final String ILLEGAL_SVCMD_MODE = ": unknown ServiceMode: ";
    public static final String ILLEGAL_SVCMD_TYPE = ": ServiceMode value must be of type string";
    public static final String NO_SUCH_FILE = ": file does not exist";
    public static final String NO_PROVIDER = ": file contains no WebServiceProviders";

    private static final Logger LOG = LogUtils.getL7dLogger(ProviderFactory.class);

    private String epAddress;
    private boolean isBaseAddr;

    static {
        ContextFactory.initGlobal(new RhinoContextFactory());
    }

    public ProviderFactory(String baseAddr) {
        epAddress = baseAddr;
        isBaseAddr = true;
    }

    public ProviderFactory() {
        // complete
    }

    public void createAndPublish(File f, String epAddr, boolean isBase) throws Exception {
        publishImpl(f, epAddr, isBase);
    }

    public synchronized void createAndPublish(File f) throws Exception {
        publishImpl(f, epAddress, isBaseAddr);
    }

    private void publishImpl(File f, String epAddr, boolean isBase) throws Exception {
        if (!f.exists()) {
            throw new Exception(f.getPath() + NO_SUCH_FILE);
        }
        boolean isE4X = f.getName().endsWith(".jsx");
        BufferedReader bufrd = new BufferedReader(new FileReader(f));
        String line = null;
        StringBuffer sb = new StringBuffer();
        for (;;) {
            line = bufrd.readLine();
            if (line == null) {
                break;
            }
            sb.append(line).append("\n");
        }
        String scriptStr = sb.toString();

        Context cx = ContextFactory.getGlobal().enterContext();
        boolean providerFound = false;
        try {
            Scriptable scriptScope = cx.initStandardObjects(null, true);
            Object[] ids = compileScript(cx, scriptStr, scriptScope, f);
            if (ids.length > 0) {
                Service.Mode mode = Service.Mode.PAYLOAD;
                for (Object idObj : ids) {
                    if (!(idObj instanceof String)) {
                        continue;
                    }
                    String id = (String)idObj;
                    if (!id.startsWith("WebServiceProvider")) {
                        continue;
                    }
                    Object obj = scriptScope.get(id, scriptScope);
                    if (!(obj instanceof Scriptable)) {
                        continue;
                    }
                    Scriptable wspVar = (Scriptable)obj;
                    providerFound = true;
                    obj = wspVar.get("ServiceMode", wspVar);
                    if (obj != Scriptable.NOT_FOUND) {
                        if (obj instanceof String) {
                            String value = (String)obj;
                            if ("PAYLOAD".equalsIgnoreCase(value)) {
                                mode = Service.Mode.PAYLOAD;
                            } else if ("MESSAGE".equalsIgnoreCase(value)) {
                                mode = Service.Mode.MESSAGE;
                            } else {
                                throw new Exception(f.getPath() + ILLEGAL_SVCMD_MODE + value);
                            }
                        } else {
                            throw new Exception(f.getPath() + ILLEGAL_SVCMD_TYPE);
                        }
                    }
                    AbstractDOMProvider provider
                        = createProvider(mode, scriptScope, wspVar,
                                         epAddr, isBase, isE4X);
                    try {
                        provider.publish();
                    } catch (AbstractDOMProvider.JSDOMProviderException ex) {
                        StringBuffer msg = new StringBuffer(f.getPath());
                        msg.append(": ").append(ex.getMessage());
                        throw new Exception(msg.toString());
                    }
                }
            }
        } finally {
            Context.exit();
        }
        if (!providerFound) {
            throw new Exception(f.getPath() + NO_PROVIDER);
        }
    }

    protected AbstractDOMProvider createProvider(Service.Mode mode, Scriptable scope,
                                                 Scriptable wsp, String epAddr,
                                                 boolean isBase, boolean e4x) throws Exception {
        if (LOG.isLoggable(Level.FINE)) {
            String modestr = (mode == Service.Mode.PAYLOAD) ? "payload" : "message";
            String type = e4x ? "E4X" : "JavaScript";
            String base = isBase ? "base " : "";
            StringBuffer msg = new StringBuffer("creating a ");
            msg.append(modestr)
                .append(" ")
                .append(type)
                .append(" provider for ")
                .append(base)
                .append("address ")
                .append(epAddr);
            LOG.log(Level.FINE, msg.toString());
        }
        AbstractDOMProvider provider = null;
        if (mode == Service.Mode.PAYLOAD) {
            provider = new DOMPayloadProvider(scope, wsp, epAddr, isBase, e4x);
        } else if (mode == Service.Mode.MESSAGE) {
            provider = new DOMMessageProvider(scope, wsp, epAddr, isBase, e4x);
        }
        return provider;
    }

    private Object[] compileScript(Context cx, String scriptStr, Scriptable scriptScope, File f) {
        int opt = cx.getOptimizationLevel();
        cx.setOptimizationLevel(-1);
        Script script = cx.compileString(scriptStr, f.getName(), 1, null);
        script.exec(cx, scriptScope);
        Object[] ids = scriptScope.getIds();
        cx.setOptimizationLevel(opt);
        script = cx.compileString(scriptStr, f.getName(), 1, null);
        script.exec(cx, scriptScope);
        return ids;
    }

    static class RhinoContextFactory extends ContextFactory {
        public boolean hasFeature(Context cx, int feature) {
            if (feature == Context.FEATURE_DYNAMIC_SCOPE) {
                return true;
            }
            return super.hasFeature(cx, feature);
        }
    }
}
