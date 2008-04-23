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

package org.apache.cxf.tools.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.helpers.JavaUtils;

public final class NameUtil {

    static final int UPPER_LETTER = 0;
    static final int LOWER_LETTER = 1;
    static final int OTHER_LETTER = 2;
    static final int DIGIT = 3;
    static final int OTHER = 4;

    private static final byte ACTION_CHECK_PUNCT = 0;
    private static final byte ACTION_CHECK_C2 = 1;
    private static final byte ACTION_BREAK = 2;
    private static final byte ACTION_NOBREAK = 3;

    private static final byte[] ACTION_TABLE = new byte[5 * 5];

    private NameUtil() {
    }
    
    static {
        for (int t0 = 0; t0 < 5; t0++) {
            for (int t1 = 0; t1 < 5; t1++) {
                ACTION_TABLE[t0 * 5 + t1] = decideAction(t0, t1);
            }
        }
    }

    protected static boolean isPunct(char c) {
        boolean isPunct = c == '-' || c == '.' || c == ':' || c == '_';
        boolean isUnicodePunct = c == '\u00b7' || c == '\u0387' || c == '\u06dd' || c == '\u06de';
        return isPunct || isUnicodePunct;
    }

    protected static boolean isLower(char c) {
        return c >= 'a' && c <= 'z' || Character.isLowerCase(c);
    }

    public static String capitalize(String s) {
        if (!isLower(s.charAt(0))) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        sb.append(Character.toUpperCase(s.charAt(0)));
        sb.append(s.substring(1).toLowerCase());
        return sb.toString();
    }

    private static int nextBreak(String s, int start) {
        int n = s.length();

        char c1 = s.charAt(start);
        int t1 = classify(c1);

        for (int i = start + 1; i < n; i++) {
            int t0 = t1;

            c1 = s.charAt(i);
            t1 = classify(c1);

            switch (ACTION_TABLE[t0 * 5 + t1]) {
            case ACTION_CHECK_PUNCT:
                if (isPunct(c1)) {
                    return i;
                }
                break;
            case ACTION_CHECK_C2:
                if (i < n - 1) {
                    char c2 = s.charAt(i + 1);
                    if (isLower(c2)) {
                        return i;
                    }
                }
                break;
            case ACTION_BREAK:
                return i;
            default:
            }
        }
        return -1;
    }

    private static byte decideAction(int t0, int t1) {
        if (t0 == OTHER && t1 == OTHER)  {
            return ACTION_CHECK_PUNCT;
        }
        if (!xor(t0 == DIGIT, t1 == DIGIT)
            || (t0 == LOWER_LETTER && t1 != LOWER_LETTER)) {
            return ACTION_BREAK;
        }
        if (!xor(t0 <= OTHER_LETTER, t1 <= OTHER_LETTER)) {
            return ACTION_BREAK;
        }
        if (!xor(t0 == OTHER_LETTER, t1 == OTHER_LETTER)) {
            return ACTION_BREAK;
        }
        if (t0 == UPPER_LETTER && t1 == UPPER_LETTER) {
            return ACTION_CHECK_C2;
        }

        return ACTION_NOBREAK;
    }

    private static boolean xor(boolean x, boolean y) {
        return (x && y) || (!x && !y);
    }

    protected static int classify(char c0) {
        switch (Character.getType(c0)) {
        case Character.UPPERCASE_LETTER:
            return UPPER_LETTER;
        case Character.LOWERCASE_LETTER:
            return LOWER_LETTER;
        case Character.TITLECASE_LETTER:
        case Character.MODIFIER_LETTER:
        case Character.OTHER_LETTER:
            return OTHER_LETTER;
        case Character.DECIMAL_DIGIT_NUMBER:
            return DIGIT;
        default:
            return OTHER;
        }
    }


    public static List<String> toWordList(String s) {
        List<String> ss = new ArrayList<String>();
        int n = s.length();
        for (int i = 0; i < n;) {
            while (i < n) {
                if (!isPunct(s.charAt(i))) {
                    break;
                }
                i++;
            }
            if (i >= n) {
                break;
            }

            int b = nextBreak(s, i);
            String w = (b == -1) ? s.substring(i) : s.substring(i, b);
            ss.add(escape(capitalize(w)));
            if (b == -1) {
                break;
            }
            i = b;
        }

        return ss;
    }

    protected static String toMixedCaseName(List<String> ss, boolean startUpper) {
        StringBuilder sb = new StringBuilder();
        if (!ss.isEmpty()) {
            sb.append(startUpper ? ss.get(0) : ss.get(0).toLowerCase());
            for (int i = 1; i < ss.size(); i++) {
                sb.append(ss.get(i));
            }
        }
        return sb.toString();
    }

    protected static String toMixedCaseVariableName(String[] ss,
                                             boolean startUpper,
                                             boolean cdrUpper) {
        if (cdrUpper) {
            for (int i = 1; i < ss.length; i++) {
                ss[i] = capitalize(ss[i]);
            }
        }
        StringBuilder sb = new StringBuilder();
        if (ss.length > 0) {
            sb.append(startUpper ? ss[0] : ss[0].toLowerCase());
            for (int i = 1; i < ss.length; i++) {
                sb.append(ss[i]);
            }
        }
        return sb.toString();
    }

    public static void escape(StringBuilder sb, String s, int start) {
        int n = s.length();
        for (int i = start; i < n; i++) {
            char c = s.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                sb.append(c);
            } else {
                sb.append('_');
                if (c <= '\u000f') {
                    sb.append("000");
                } else if (c <= '\u00ff') {
                    sb.append("00");
                } else if (c <= '\u0fff') {
                    sb.append('0');
                }
                sb.append(Integer.toString(c, 16));
            }
        }
    }

    private static String escape(String s) {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                StringBuilder sb = new StringBuilder(s.substring(0, i));
                escape(sb, s, i);
                return sb.toString();
            }
        }
        return s;
    }

    public static boolean isJavaIdentifier(String s) {
        if (s.length() == 0) {
            return false;
        }
        if (JavaUtils.isJavaKeyword(s)) {
            return false;
        }

        if (!Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }

        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static String mangleNameToClassName(String name) {
        return toMixedCaseName(toWordList(name), true);
    }

    public static String mangleNameToVariableName(String name) {
        return toMixedCaseName(toWordList(name), false);
    }    
}
