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

package org.apache.cxf.tools.common.toolspec.parser;

import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.tools.common.toolspec.Tool;

public class Option implements TokenConsumer {

    private static final Logger LOG = LogUtils.getL7dLogger(Option.class);
    private static final String VALUE_ENUM_SEPARATOR = "|";
    protected Element argument;
    protected Element annotation;
    private final Element element;
    private Element valueType;

    private int numMatches;

    public Option(Element el) {
        this.element = el;
        
        
        List<Element> elemList = DOMUtils.findAllElementsByTagNameNS(element, 
                                                                     Tool.TOOL_SPEC_PUBLIC_ID, 
                                                                     "associatedArgument");
        if (elemList != null && elemList.size() > 0) {            
            argument = (Element)elemList.get(0);
        }
        
        elemList = DOMUtils.findAllElementsByTagNameNS(element, 
                                                       Tool.TOOL_SPEC_PUBLIC_ID, 
                                                       "annotation");
        if (elemList != null && elemList.size() > 0) {            
            annotation = (Element)elemList.get(0);
        }

        if (annotation == null && argument != null) {
            elemList =  DOMUtils.findAllElementsByTagNameNS(argument, Tool.TOOL_SPEC_PUBLIC_ID, "annotation");

            if (elemList != null && elemList.size() > 0) {
                annotation = (Element)elemList.get(0);
            }
        }
    }

    public boolean hasArgument() {
        return argument != null;
    }

    public boolean hasImmediateArgument() {
        return hasArgument() && "immediate".equals(argument.getAttribute("placement"));
    }

    /**
     * @return whether the first token was accepted
     */
    public boolean accept(TokenInputStream args, Element result, ErrorVisitor errors) {

        if (args.available() == 0) {
            return false;
        }
        String arg = args.peek();

        if (arg == null) {
            LOG.severe("ARGUMENT_IS_NULL_MSG");
        }

        // go through each switch to see if we can match one to the arg.
        List<Element> switches = 
            DOMUtils.findAllElementsByTagNameNS(element, Tool.TOOL_SPEC_PUBLIC_ID, "switch");

        boolean accepted = false;

        for (Element switchElem : switches) {

            String switchArg = "-" + switchElem.getFirstChild().getNodeValue();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("switchArg is " + switchArg);
            }
            if (hasImmediateArgument() ? arg.startsWith(switchArg) : arg.equals(switchArg)) {
                LOG.fine("Matches a switch!!!");
                // consume the token
                args.read();
                // Add ourselves to the result document
                Element optionEl = result.getOwnerDocument()
                    .createElementNS("http://cxf.apache.org/Xutil/Command", "option");
 
                optionEl.setAttribute("name", getName());

                // Add argument value to result
                if (hasArgument()) {
                    String argValue;
                    if (hasImmediateArgument()) {
                        argValue = arg.substring(switchArg.length());
                    } else {
                        argValue = readArgumentValue(args, switchArg, errors);
                    }
                    if (argValue != null) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Setting argument value of option to " + argValue);
                        }
                        optionEl.appendChild(result.getOwnerDocument().createTextNode(argValue));

                    } else {
                        break;
                    }
                }
                result.appendChild(optionEl);
                numMatches++;
                accepted = true;
            }
        }
        return accepted;
    }

    private String readArgumentValue(TokenInputStream args, String switchArg, ErrorVisitor errors) {
        String value = null;
        if (args.available() > 0) {
            value = args.read();
            if (value.startsWith("-")) {
                errors.add(new ErrorVisitor.InvalidOption(switchArg));
                value = null;
            } else if (hasInvalidCharacter(value)) {
                errors.add(new ErrorVisitor.UserError(switchArg + " has invalid character!"));
            }
            if (!isInEnumArgumentValue(value)) {
                errors.add(new ErrorVisitor.UserError(switchArg + " " 
                                                      + value + " not in the enumeration value list!"));
            }
        } else {
            errors.add(new ErrorVisitor.InvalidOption(switchArg));
        }
        return value;
    }

    private boolean hasInvalidCharacter(String argValue) {
        
        List<Element> list = 
            DOMUtils.findAllElementsByTagNameNS(argument, Tool.TOOL_SPEC_PUBLIC_ID, "valuetype");
        //NodeList list = argument.getElementsByTagNameNS(Tool.TOOL_SPEC_PUBLIC_ID, "valuetype");
        String valuetypeStr = null;

        if (list != null && list.size() > 0) {
            valueType = (Element)list.get(0);
            valuetypeStr = valueType.getFirstChild().getNodeValue();

            if ("IdentifyString".equals(valuetypeStr)) {
                return !isIdentifyString(argValue);
            } else if ("NamingSpacePackageString".equals(valuetypeStr)) {
                return !isNamingSpacePackageString(argValue);
            } else if ("Digital".equals(valuetypeStr)) {
                for (int i = 0; i < argValue.length(); i++) {
                    if (!Character.isDigit(argValue.charAt(i))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean isInEnumArgumentValue(String argValue) {
        boolean result = true;
        List<Element> list = 
            DOMUtils.findAllElementsByTagNameNS(argument, Tool.TOOL_SPEC_PUBLIC_ID, "valueenum");
        
        
        //NodeList list = argument.getElementsByTagNameNS(Tool.TOOL_SPEC_PUBLIC_ID, "valueenum");
        if (list != null && list.size() == 1) {
            result = false;
            String enumValue = list.get(0).getTextContent();
            StringTokenizer stk = new StringTokenizer(enumValue, VALUE_ENUM_SEPARATOR);
            if (stk.countTokens() <= 0) {
                return result;
            }
            while (stk.hasMoreTokens()) {
                if (argValue.equals(stk.nextToken())) {
                    result = true;
                }
            }
        }
        return result;
    }

    private boolean isIdentifyString(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '.') {
                continue;
            } else {
                if (!Character.isJavaIdentifierPart(value.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isNamingSpacePackageString(String value) {
        if (value.indexOf("=") < 0) {
            return isIdentifyString(value);
        } else {
            String packageName = value.substring(value.indexOf("=") + 1, value.length());
            return isIdentifyString(packageName);
        }
    }

    
    public boolean isSatisfied(ErrorVisitor errors) {
        if (errors.getErrors().size() > 0) {
            return false;
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("For this option, minOccurs=" + element.getAttribute("minOccurs") + " and maxOccurs="
                     + element.getAttribute("maxOccurs") + ", numMatches currently " + numMatches);
        }
        boolean result = true;

        if (!isAtleastMinimum()) {
            errors.add(new ErrorVisitor.MissingOption(this));
            result = false;
        }
        if (result && !isNoGreaterThanMaximum()) {
            errors.add(new ErrorVisitor.DuplicateOption(getName()));
            result = false;
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("isSatisfied() returning " + result);
        }
        return result;
    }

    private boolean isAtleastMinimum() {
        boolean result = true;
        int minOccurs = 0;

        if (!"".equals(element.getAttribute("minOccurs"))) {
            result = numMatches >= Integer.parseInt(element.getAttribute("minOccurs"));
        } else {
            result = numMatches >= minOccurs;
        }
        return result;
    }

    private boolean isNoGreaterThanMaximum() {
        boolean result = true;
        int maxOccurs = 1;

        if (!"".equals(element.getAttribute("maxOccurs"))) {
            result = "unbounded".equals(element.getAttribute("maxOccurs"))
                     || numMatches <= Integer.parseInt(element.getAttribute("maxOccurs"));
        } else {
            result = numMatches <= maxOccurs;
        }
        return result;
    }

    public String getName() {
        return element.getAttribute("id");
    }

    public String getAnnotation() {
        return annotation.getFirstChild().getNodeValue();
    }

    public String getPrimarySwitch() {
        //NodeList switches = element.getElementsByTagNameNS(Tool.TOOL_SPEC_PUBLIC_ID, "switch");
        
        List<Element> switches = 
            DOMUtils.findAllElementsByTagNameNS(element, Tool.TOOL_SPEC_PUBLIC_ID, "switch");

        // options must have atleast one switch, as enforced by schema, so no
        // need for defensive coding.
        return switches.get(0).getFirstChild().getNodeValue();
    }

    public String toString() {
        return getName();
    }

}
