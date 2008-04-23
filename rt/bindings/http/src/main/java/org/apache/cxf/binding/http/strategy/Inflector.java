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
package org.apache.cxf.binding.http.strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Inflector {
    
    private List<String> uncountable = new ArrayList<String>();
    private List<Rule> singular = new ArrayList<Rule>();
    private List<Rule> plural = new ArrayList<Rule>();
    private Map<String, String> irregular = new HashMap<String, String>();

    public String singularlize(String orig) {
        if (uncountable.contains(orig)) {
            return orig;
        } 
        
        for (Map.Entry<String, String> entry : irregular.entrySet()) {
            if (entry.getValue().equals(orig)) {
                return entry.getKey();
            }
        }
        
        for (Rule r : singular) {
            Matcher m = r.getRegex().matcher(orig);
            if (m.find()) {
                return m.replaceAll(r.getReplacement());
            }
        }
       
        return orig;
    }
    
    public String pluralize(String orig) {
        if (uncountable.contains(orig)) {
            return orig;
        } 
        
        String irr = irregular.get(orig);
        if (irr != null) {
            return irr;
        }
        
        for (Rule r : plural) {
            Matcher m = r.getRegex().matcher(orig);
            //System.out.println(m.pattern().pattern());
            if (m.find()) {
                //System.out.println("!!!found match!!!");
                return m.replaceAll(r.getReplacement());
            }
        }
       
        return orig;
    }
    
    public void addPlural(String regex, String replacement) {
        plural.add(0, new Rule(regex, replacement));
    }
    
    public void addSingular(String regex, String replacement) {
        singular.add(0, new Rule(regex, replacement));
    }
    
    public void addIrregular(String orig, String replacement) {
        irregular.put(orig, replacement);
    }
    
    public void addUncountable(String[] words) {
        uncountable.addAll(Arrays.asList(words));
    }
    
    public void addUncountable(String word) {
        uncountable.add(word);
    }
    
    static class Rule {
        private Pattern regex;
        private String replacement;
        
        public Rule(String regex, String replacement) {
            this.regex = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            this.replacement = replacement;
        }
        
        public Pattern getRegex() {
            return regex;
        }
        public String getReplacement() {
            return replacement;
        }
    }
}
