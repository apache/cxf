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
package org.apache.cxf.common.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexLoggingFilter {

    public static final String DEFAULT_REPLACEMENT = "*****";

    private static class ReplaceRegEx {
        private final Pattern pattern;
        private final int group;
        private final String replacement;

        ReplaceRegEx(String pattern, int group, String replacement) {
            this.pattern = Pattern.compile(pattern);
            this.group = group;
            this.replacement = replacement;
        }

        public CharSequence filter(CharSequence command) {
            Matcher m = pattern.matcher(command);
            int offset = 0;
            while (m.find()) {
                int origLen = command.length();
                command = new StringBuilder(command)
                    .replace(m.start(group) + offset, m.end(group) + offset, replacement).toString();
                offset += command.length() - origLen;
            }
            return command;
        }
    }

    private String regPattern;
    private int regGroup = 1;
    private String regReplacement = DEFAULT_REPLACEMENT;

    private List<ReplaceRegEx> regexs = new ArrayList<>();

    public CharSequence filter(CharSequence command) {
        if (regPattern != null) {
            command = new ReplaceRegEx(regPattern, regGroup, regReplacement).filter(command);
        }
        for (ReplaceRegEx regex : regexs) {
            command = regex.filter(command);
        }
        return command;
    }

    public void addRegEx(String pattern) {
        addRegEx(pattern, 1);
    }

    public void addRegEx(String pattern, int group) {
        addRegEx(pattern, group, DEFAULT_REPLACEMENT);
    }

    public void addRegEx(String pattern, int group, String replacement) {
        regexs.add(new ReplaceRegEx(pattern, group, replacement));
    }

    public void addCommandOption(String option, String... commands) {
        StringBuilder pattern = new StringBuilder("(");
        for (String command : commands) {
            if (pattern.length() > 1) {
                pattern.append('|');
            }
            pattern.append(Pattern.quote(command));
        }
        pattern.append(") +.*?").append(Pattern.quote(option)).append(" +([^ ]+)");
        regexs.add(new ReplaceRegEx(pattern.toString(), 2, DEFAULT_REPLACEMENT));
    }

    public String getPattern() {
        return regPattern;
    }

    public void setPattern(String pattern) {
        this.regPattern = pattern;
    }

    public String getReplacement() {
        return regReplacement;
    }

    public void setReplacement(String replacement) {
        this.regReplacement = replacement;
    }

    public int getGroup() {
        return regGroup;
    }

    public void setGroup(int group) {
        this.regGroup = group;
    }
}
