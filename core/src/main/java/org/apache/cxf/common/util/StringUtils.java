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

package org.apache.cxf.common.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtils {
    public static final Map<String, Pattern> PATTERN_MAP = new HashMap<String, Pattern>();
    static {
        String patterns[] = {"/", " ", ":", "," , ";", "=", "\\.", "\\+"}; 
        for (String p : patterns) {
            PATTERN_MAP.put(p, Pattern.compile(p));
        }
    }
        
    private StringUtils() {
    }
    
    public static String[] split(String s, String regex) {
        Pattern p = PATTERN_MAP.get(regex);
        if (p != null) {
            return p.split(s);
        }
        return s.split(regex);
    }
    public static String[] split(String s, String regex, int limit) {
        Pattern p = PATTERN_MAP.get(regex);
        if (p != null) {
            return p.split(s, limit);
        }
        return s.split(regex, limit);
    }

    public static boolean isFileExist(String file) {
        return new File(file).exists() && new File(file).isFile();
    }

    public static boolean isEmpty(String str) {
        if (str != null) {
            int len = str.length();
            for (int x = 0; x < len; ++x) {
                if (str.charAt(x) > ' ') {
                    return false;
                }
            }
        }
        return true;
    }
    
    public static boolean isEmpty(List<String> list) {
        if (list == null || list.size() == 0) {
            return true;
        }
        if (list.size() == 1 && isEmpty(list.get(0))) {
            return true;
        }
        return false;
    }
   
    public static String diff(String str1, String str2) {
        int index = str1.lastIndexOf(str2);
        if (index > -1) {
            return str1.substring(str2.length());
        }
        return str1;
    }
    
    public static List<String> getParts(String str, String separator) {
        String[] parts = split(str, separator);
        List<String> ret = new ArrayList<String>(parts.length);
        for (String part : parts) {
            if (!isEmpty(part)) {
                ret.add(part);
            }
        }
        return ret;
    }
    
    public static String getFirstNotEmpty(String str, String separator) {
        List<String> parts = Arrays.asList(split(str, separator));
        for (String part : parts) {
            if (!isEmpty(part)) {
                return part;
            }
        }
        return str;
    }
    
    public static String getFirstNotEmpty(List<String> list) {
        if (isEmpty(list)) {
            return null;
        }
        for (String item : list) {
            if (!isEmpty(item)) {
                return item;
            }       
        }
        return null;
    }
    
    public static List<String> getFound(String contents, String regex) {
        if (isEmpty(regex) || isEmpty(contents)) {
            return null;
        }
        List<String> results = new ArrayList<String>();
        Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(contents);
        
        while (matcher.find()) {
            if (matcher.groupCount() > 0) {
                results.add(matcher.group(1));
            } else {
                results.add(matcher.group());
            }
        }
        return results;
    } 
    
    public static String getFirstFound(String contents, String regex) {
        List<String> founds = getFound(contents, regex);
        if (isEmpty(founds)) {
            return null;
        }
        return founds.get(0);
    }
    
    public static String addDefaultPortIfMissing(String urlString) {
        return addDefaultPortIfMissing(urlString, "80");
    }
    
    public static String addDefaultPortIfMissing(String urlString, String defaultPort) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            return urlString;
        }
        if (url.getPort() != -1) {
            return urlString;
        }
        String regex = "http://([^/]+)";        
        String found = StringUtils.getFirstFound(urlString, regex);
        String replacer = "http://" + found + ":" + defaultPort;
        
        if (!StringUtils.isEmpty(found)) {
            urlString = urlString.replaceFirst(regex, replacer);
        }                
        return urlString;
    }
 
    /**
     * Return input string with first character in upper case.
     * @param name input string.
     * @return capitalized form.
     */
    public static String capitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }
    
    public static String uncapitalize(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        return new StringBuilder(str.length())
            .append(Character.toLowerCase(str.charAt(0)))
            .append(str.substring(1))
            .toString();
    }    
    
    public static byte[] toBytesUTF8(String str) {
        return toBytes(str, "UTF-8");
    }
    public static byte[] toBytesASCII(String str) {
        return toBytes(str, "US-ASCII");
    }
    public static byte[] toBytes(String str, String enc) {
        try {
            return str.getBytes(enc);
        } catch (UnsupportedEncodingException ex) { 
            throw new RuntimeException(ex);
        }
    }

    public static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            hexString.append(Integer.toHexString(0xFF & bytes[i]));
        }
        return hexString.toString();
    }
}
