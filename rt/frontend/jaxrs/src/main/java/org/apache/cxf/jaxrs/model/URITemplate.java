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

package org.apache.cxf.jaxrs.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

public final class URITemplate {

    public static final String TEMPLATE_PARAMETERS = "jaxrs.template.parameters";
    public static final String LIMITED_REGEX_SUFFIX = "(/.*)?";
    public static final String UNLIMITED_REGEX_SUFFIX = "(/)?";
    public static final String FINAL_MATCH_GROUP = "FINAL_MATCH_GROUP";

    /**
     * The regular expression for matching URI templates and names.
     */
    private static final Pattern TEMPLATE_NAMES_PATTERN = Pattern.compile("\\{(\\w[-\\w\\.]*)(\\:(.+?))?\\}");

    private static final String DEFAULT_PATH_VARIABLE_REGEX = "([^/]+?)";
    private static final String PATH_UNLIMITED_VARIABLE_REGEX = "(.*?)";
        
    private static final String CHARACTERS_TO_ESCAPE = ".";

    private final String template;
    private final List<String> templateVariables = new ArrayList<String>();
    private final List<String> customTemplateVariables = new ArrayList<String>();
    private final Pattern templateRegexPattern;
    private final String literals;

    public URITemplate(String theTemplate) {
        this(theTemplate, true);
    }

    public URITemplate(String theTemplate, boolean limited) {
        
        this.template = theTemplate;

        StringBuilder literalChars = new StringBuilder();
        StringBuilder patternBuilder = new StringBuilder();

        // compute a regular expression from URI template
        Matcher matcher = TEMPLATE_NAMES_PATTERN.matcher(template);
        int i = 0;
        while (matcher.find()) {
            templateVariables.add(matcher.group(1).trim());

            String substr = escapeCharacters(template.substring(i, matcher.start()));
            literalChars.append(substr);
            patternBuilder.append(substr);
            i = matcher.end();
            if (matcher.group(2) != null && matcher.group(3) != null) {
                patternBuilder.append('(');
                patternBuilder.append(matcher.group(3).trim());
                patternBuilder.append(')');
                customTemplateVariables.add(matcher.group(1).trim());
            } else {
                if (!limited && i == template.length()) {
                    patternBuilder.append(PATH_UNLIMITED_VARIABLE_REGEX);
                } else {
                    patternBuilder.append(DEFAULT_PATH_VARIABLE_REGEX);
                }
            }
        }
        String substr = escapeCharacters(template.substring(i, template.length()));
        literalChars.append(substr);
        patternBuilder.append(substr);

        literals = literalChars.toString();

        int endPos = patternBuilder.length() - 1;
        boolean endsWithSlash = (endPos >= 0) ? patternBuilder.charAt(endPos) == '/' : false;
        if (endsWithSlash) {
            patternBuilder.deleteCharAt(endPos);
        }
        patternBuilder.append(limited ? LIMITED_REGEX_SUFFIX : UNLIMITED_REGEX_SUFFIX);

        templateRegexPattern = Pattern.compile(patternBuilder.toString());
    }

    public String getLiteralChars() {
        return literals;
    }

    public String getValue() {
        return template;
    }

    public List<String> getVariables() {
        return Collections.unmodifiableList(templateVariables);
    }

    public List<String> getCustomVariables() {
        return Collections.unmodifiableList(customTemplateVariables);
    }

    private static String escapeCharacters(String expression) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            sb.append(isReservedCharacter(ch) ? "\\" + ch : ch);
        }
        return sb.toString();
    }

    private static boolean isReservedCharacter(char ch) {
        return CHARACTERS_TO_ESCAPE.indexOf(ch) != -1;
    }

    public boolean match(String uri, MultivaluedMap<String, String> templateVariableToValue) {

        if (uri == null) {
            return (templateRegexPattern == null) ? true : false;
        }

        if (templateRegexPattern == null) {
            return false;
        }

        Matcher m = templateRegexPattern.matcher(uri);
        if (!m.matches()) {
            if (uri.contains(";")) {
                // we might be trying to match one or few path segments containing matrix
                // parameters against a clear path segment as in @Path("base").
                List<PathSegment> pList = JAXRSUtils.getPathSegments(template, false);
                List<PathSegment> uList = JAXRSUtils.getPathSegments(uri, false);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < uList.size(); i++) {
                    sb.append('/');
                    if (pList.size() > i && pList.get(i).getPath().indexOf('{') == -1) {
                        sb.append(uList.get(i).getPath());
                    } else {
                        sb.append(HttpUtils.fromPathSegment(uList.get(i)));
                    }
                }
                uri = sb.toString();
                m = templateRegexPattern.matcher(uri);
                if (!m.matches()) {
                    return false;
                }
            } else {
                return false;
            }
        }

        // Assign the matched template values to template variables
        int i = 1;
        for (String name : templateVariables) {
            String value = m.group(i++);
            templateVariableToValue.add(name, value);
        }

        // The right hand side value, might be used to further resolve sub-resources.

        String finalGroup = m.group(i);
        templateVariableToValue.putSingle(FINAL_MATCH_GROUP, finalGroup == null ? "/" : finalGroup);

        return true;
    }

    /**
     * Substitutes template variables with listed values. List of values is counterpart for
     * {@link #getVariables() list of variables}. When list of value is shorter than variables substitution
     * is partial. When variable has pattern, value must fit to pattern, otherwise
     * {@link IllegalArgumentException} is thrown.
     * <p>
     * Example1: for template "/{a}/{b}/{a}" {@link #getVariables()} returns "[a, b, a]"; providing here list
     * of value "[foo, bar, baz]" results with "/foo/bar/baz".
     * <p>
     * Example2: for template "/{a}/{b}/{a}" providing list of values "[foo]" results with "/foo/{b}/{a}".
     * 
     * @param values values for variables
     * @return template with bound variables.
     * @throws IllegalArgumentException when values is null, any value does not match pattern etc.
     */
    public String substitute(List<String> values) throws IllegalArgumentException {
        if (values == null) {
            throw new IllegalArgumentException("values is null");
        }
        Matcher m = TEMPLATE_NAMES_PATTERN.matcher(template);
        Iterator<String> valIter = values.iterator();
        StringBuffer sb = new StringBuffer();
        while (m.find() && valIter.hasNext()) {
            String value = valIter.next();
            String varPattern = m.group(2);
            if (varPattern != null) {
                // variable has pattern, matching formats e.g.
                // for "{a:\d\d}" variable value must have two digits etc.
                Pattern p = Pattern.compile(varPattern);
                if (!p.matcher(":" + value).matches()) {
                    throw new IllegalArgumentException("Value '" + value + "' does not match variable "
                                                       + m.group());
                }
            }
            m.appendReplacement(sb, value);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Substitutes template variables with mapped values. Variables are mapped to values; if not all variables
     * are bound result will still contain variables. Note that all variables with the same name are replaced
     * by one value.
     * <p>
     * Example: for template "/{a}/{b}/{a}" {@link #getVariables()} returns "[a, b, a]"; providing here
     * mapping "[a: foo, b: bar]" results with "/foo/bar/foo" (full substitution) and for mapping "[b: baz]"
     * result is "{a}/baz/{a}" (partial substitution).
     * 
     * @param valuesMap map variables to their values; on each value Object.toString() is called.
     * @return template with bound variables.
     * @throws IllegalArgumentException when size of list of values differs from list of variables or list
     *                 contains nulls.
     */
    public String substitute(Map<String, ? extends Object> valuesMap) throws IllegalArgumentException {
        if (valuesMap == null) {
            throw new IllegalArgumentException("valuesMap is null");
        }
        Matcher m = TEMPLATE_NAMES_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            Object value = valuesMap.get(m.group(1));
            if (value == null) {
                continue;
            }
            String sval = value.toString();
            String varPattern = m.group(2);
            if (varPattern != null) {
                Pattern p = Pattern.compile(varPattern);
                if (!p.matcher(":" + sval).matches()) {
                    throw new IllegalArgumentException("Value '" + sval + "' does not match variable "
                                                       + m.group());
                }
            }
            m.appendReplacement(sb, sval);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Encoded literal characters surrounding template variables, 
     * ex. "a {id} b" will be encoded to "a%20{id}%20b" 
     * @return encoded value
     */
    public String encodeLiteralCharacters() {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = TEMPLATE_NAMES_PATTERN.matcher(template);
        int i = 0;
        while (matcher.find()) {
            sb.append(HttpUtils.encodePartiallyEncoded(template.substring(i, matcher.start()), false));
            sb.append('{').append(matcher.group(1)).append('}');
            i = matcher.end();
        }
        sb.append(HttpUtils.encodePartiallyEncoded(template.substring(i, template.length()), false));
        return sb.toString();
    }
    
    public static URITemplate createTemplate(ClassResourceInfo cri, Path path) {

        if (path == null) {
            return new URITemplate("/");
        }

        String pathValue = path.value();
        if (!pathValue.startsWith("/")) {
            pathValue = "/" + pathValue;
        }

        return new URITemplate(pathValue, path.limited());
    }

    public static int compareTemplates(URITemplate t1, URITemplate t2) {
        String l1 = t1.getLiteralChars();
        String l2 = t2.getLiteralChars();
        if (!l1.equals(l2)) {
            // descending order
            return l1.length() < l2.length() ? 1 : -1;
        }

        int g1 = t1.templateVariables.size();
        int g2 = t2.templateVariables.size();
        // descending order
        int result = g1 < g2 ? 1 : g1 > g2 ? -1 : 0;
        if (result == 0) {
            int gCustom1 = t1.customTemplateVariables.size();
            int gCustom2 = t2.customTemplateVariables.size();
            if (gCustom1 != gCustom2) {
                // descending order
                return gCustom1 < gCustom2 ? 1 : -1;
            }
        }
        return result;
    }
}
