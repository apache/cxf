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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.tools.common.toolspec.Tool;

public class OptionGroup implements TokenConsumer {

    private static final Logger LOG = LogUtils.getL7dLogger(OptionGroup.class);
    private final Element element;

    private final List<Object> options = new ArrayList<Object>();

    public OptionGroup(Element el) {
        this.element = el;
        
        List<Element> optionEls = 
            DOMUtils.findAllElementsByTagNameNS(element, 
                                                Tool.TOOL_SPEC_PUBLIC_ID, 
                                                "option");        
        for (Element elem : optionEls) {
            options.add(new Option(elem));
        }
    }

    public boolean accept(TokenInputStream args, Element result, ErrorVisitor errors) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Accepting token stream for optionGroup: " + this + ", tokens are now " + args
                     + ", running through " + options.size() + " options");
        }
        // Give all the options the chance to exclusively consume the given
        // string:
        boolean accepted = false;

        for (Iterator it = options.iterator(); it.hasNext();) {
            Option option = (Option)it.next();

            if (option.accept(args, result, errors)) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Option " + option + " accepted the token");
                }
                accepted = true;
                break;
            }
        }
        if (!accepted) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("No option accepted the token, returning");
            }
            return false;
        }

        return true;
    }

    public boolean isSatisfied(ErrorVisitor errors) {
        // Return conjunction of all isSatisfied results from every option
        for (Iterator it = options.iterator(); it.hasNext();) {
            if (!((Option)it.next()).isSatisfied(errors)) {
                return false;
            }
        }
        return true;
    }

    public String getId() {
        return element.getAttribute("id");
    }

    public String toString() {
        if (element.hasAttribute("id")) {
            return getId();
        } else {
            return super.toString();
        }
    }
}
