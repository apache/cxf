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

package org.apache.cxf.sts.claims.mapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.jexl2.Script;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.claims.ClaimsMapper;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;

public class JexlClaimsMapper implements ClaimsMapper {

    private static final Logger LOG = LogUtils.getL7dLogger(JexlClaimsMapper.class);

    private JexlEngine jexlEngine = new JexlEngine();
    private Script script;

    public JexlClaimsMapper() {
        // jexl.setCache(512);
        // jexl.setLenient(false);
        jexlEngine.setSilent(false);

        Map<String, Object> functions = new HashMap<>();
        functions.put("claims", new ClaimUtils());
        functions.put("LOG", LOG);
        jexlEngine.setFunctions(functions);
    }

    public JexlClaimsMapper(String script) throws IOException {
        this();

        if (script != null) {
            setScript(script);
        }
    }

    public ProcessedClaimCollection mapClaims(String sourceRealm, ProcessedClaimCollection sourceClaims,
        String targetRealm, ClaimsParameters parameters) {
        JexlContext context = new MapContext();
        context.set("sourceClaims", sourceClaims);
        context.set("targetClaims", new ProcessedClaimCollection());
        context.set("sourceRealm", sourceRealm);
        context.set("targetRealm", targetRealm);
        context.set("claimsParameters", parameters);

        Script s = getScript();
        if (s == null) {
            LOG.warning("No claim mapping script defined");
            return new ProcessedClaimCollection(); // TODO Check if null or an exception would be more
                                                   // appropriate
        } else {
            return (ProcessedClaimCollection)s.execute(context);
        }
    }

    public Script getScript() {
        return script;
    }

    public void setScript(Script script) {
        this.script = script;
    }

    public void setScript(String scriptLocation) throws IOException {
        URL resource = ClassLoaderUtils.getResource(scriptLocation, this.getClass());
        if (resource != null) {
            scriptLocation = resource.getPath();
            LOG.fine("Script found within Classpath: " + scriptLocation);
        }
        File scriptFile = new File(scriptLocation);
        if (scriptFile.exists()) {
            this.script = jexlEngine.createScript(scriptFile);
        } else {
            throw new IllegalArgumentException("Script resource not found!");
        }
    }

    public JexlEngine getJexlEngine() {
        return jexlEngine;
    }

    public void setJexlEngine(JexlEngine jexl) {
        this.jexlEngine = jexl;
    }

}
