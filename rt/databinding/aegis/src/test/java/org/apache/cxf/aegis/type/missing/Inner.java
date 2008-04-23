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
package org.apache.cxf.aegis.type.missing;

import java.io.Serializable;

public class Inner implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -1009032817440459241L;

    private int script;
    private int scheme;
    private String transliteration;

    /**
     * @return the scheme
     */
    public int getScheme() {
        return scheme;
    }

    /**
     * @param scheme the scheme to set
     */
    public void setScheme(int scheme) {
        this.scheme = scheme;
    }

    /**
     * @return the script
     */
    public int getScript() {
        return script;
    }

    /**
     * @param script the script to set
     */
    public void setScript(int script) {
        this.script = script;
    }

    /**
     * @return the transliteration
     */
    public String getTransliteration() {
        return transliteration;
    }

    /**
     * @param transliteration the transliteration to set
     */
    public void setTransliteration(String transliteration) {
        this.transliteration = transliteration;
    }

}
