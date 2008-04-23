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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.toolspec.ToolSpec;

public class Argument implements TokenConsumer {

    private static final Logger LOG = LogUtils.getL7dLogger(Argument.class);

    protected ToolSpec toolspec;

    private final Element element;
    private int numMatches;

    public Argument(Element el) {
        this.element = el;
    }

    public boolean accept(TokenInputStream args, Element result, ErrorVisitor errors) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Accepting token stream for argument: " + this);
        }
        int minOccurs;
        if ("unbounded".equals(element.getAttribute("minOccurs"))) {
            minOccurs = 0;
        } else {
            minOccurs = Integer.parseInt(element.getAttribute("minOccurs"));
        }
        if (minOccurs == 0) {
            addElement(args, result);
            return true;
        }
        if (minOccurs > args.available()) {
            return false;
        }
        if (args.peekPre().endsWith(",") && args.peekPre().startsWith("-")) {
            if (args.hasNext()) {
                args.readNext();
            } else {
                return false;
            }
        }
        for (int i = 0; i < minOccurs; i++) {
            if (args.peek().startsWith("-")) {
                errors.add(new ErrorVisitor.UnexpectedOption(args.peek()));
                return false;
            }
            addElement(args, result);
        }
        return true;
    }

    private void addElement(TokenInputStream args, Element result) {
        Element argEl = result.getOwnerDocument().createElementNS("http://cxf.apache.org/Xutil/Command",
                                                                  "argument");
        argEl.setAttribute("name", getName());
        if (!args.isOutOfBound()) {
            argEl.appendChild(result.getOwnerDocument().createTextNode(args.read()));
        }
        result.appendChild(argEl);
        numMatches++;
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
        //  int maxOccurs = 1;
        if ("unbounded".equals(element.getAttribute("maxOccurs"))
            || "".equals(element.getAttribute("maxOccurs"))) {
            return true;
        }
        if (!"".equals(element.getAttribute("maxOccurs"))) {
            result = numMatches <= Integer.parseInt(element.getAttribute("maxOccurs"));
        }
        return result;
    }

    public boolean isSatisfied(ErrorVisitor errors) {
        if (errors.getErrors().size() > 0) {
            return false;
        }
        if (!isAtleastMinimum()) {
            errors.add(new ErrorVisitor.MissingArgument(getName()));
            return false;
        }
        if (!isNoGreaterThanMaximum()) {
            errors.add(new ErrorVisitor.DuplicateArgument(getName()));
            return false;
        }

        return true;
    }

    public void setToolSpec(ToolSpec toolSpec) {
        this.toolspec = toolSpec;
    }

    public String getName() {
        return element.getAttribute("id");
    }

    public String toString() {
        return getName();
    }
}
