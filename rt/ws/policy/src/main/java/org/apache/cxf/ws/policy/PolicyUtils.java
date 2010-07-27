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

package org.apache.cxf.ws.policy;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.ws.policy.builder.primitive.NestedPrimitiveAssertion;
import org.apache.neethi.Assertion;
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyOperator;

/**
 * 
 */
public final class PolicyUtils {

    private static final String INDENT = "  ";
    
    private PolicyUtils() {
    }  
    
    /**
     * Checks if a given policy contains no policy components 
     * or if it has only empty ExactlyOne or All components 
     * containing no assertions 
     * 
     * @param p the policy
     * @return true if the policy is empty
     */
    public static boolean isEmptyPolicy(Policy p) {
        
        return isEmptyPolicyOperator(p);
    }
    
    /**
     * Checks if a given policy operator has no policy components 
     * or if it has only empty ExactlyOne or All components 
     * containing no assertions 
     * 
     * @param p the policy operator
     * @return true if this policy operator is empty
     */
    public static boolean isEmptyPolicyOperator(PolicyOperator p) {
        
        if (p.isEmpty()) {
            return true;
        }
        
        List components = p.getPolicyComponents();
        
        for (Object component : components) {
            if (!(component instanceof PolicyOperator)
                || !isEmptyPolicyOperator((PolicyOperator)component)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Determine if a collection of assertions contains a given assertion, using
     * the equal method from the Assertion interface.
     * 
     * @param assertions a collection of assertions
     * @param candidate the assertion to test
     * @return true iff candidate is equal to one of the assertions in the collection
     */
    public static boolean contains(Collection<Assertion> assertions, Assertion candidate) {
        for (Assertion a : assertions) {
            if (a.equal(candidate)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Determine if one collection of assertions contains another collection of assertion, using
     * the equal method from the Assertion interface.
     * 
     * @param assertions a collection of assertions
     * @param candidates the collections of assertion to test
     * @return true iff each candidate is equal to one of the assertions in the collection
     */
    public static boolean contains(Collection<Assertion> assertions, 
                                   Collection<Assertion> candidates) {
        if (null == candidates || candidates.isEmpty()) {
            return true;
        }
        for (Assertion c : candidates) {
            if (!contains(assertions, c)) {
                return false;
            }
        }
        return true;
    }
    
    public static void logPolicy(Logger log, Level level, String msg, PolicyComponent pc) {
        if (!log.isLoggable(level)) {
            return;
        }
        if (null == pc) {
            log.log(level, msg);
            return;
        }
        StringBuilder buf = new StringBuilder();
        buf.append(msg);
        nl(buf);
        printPolicyComponent(pc, buf, 0);
        log.log(level, buf.toString());
    }
    
    public static void printPolicyComponent(PolicyComponent pc) {
        StringBuilder buf = new StringBuilder();
        printPolicyComponent(pc, buf, 0);
        System.out.println(buf.toString());
    }
    
    public static void printPolicyComponent(PolicyComponent pc, StringBuilder buf, int level) {
        indent(buf, level);
        buf.append("type: ");
        buf.append(typeToString(pc.getType()));
        if (Constants.TYPE_ASSERTION == pc.getType()) {
            buf.append(" ");
            buf.append(((Assertion)pc).getName());
            if (((Assertion)pc).isOptional()) {
                buf.append(" (optional)");
            }
            buf.append(" (");
            buf.append((Assertion)pc);
            buf.append(")");
            nl(buf);
            if (pc instanceof NestedPrimitiveAssertion) {
                PolicyComponent nested = ((NestedPrimitiveAssertion)pc).getPolicy();
                level++;
                printPolicyComponent(nested, buf, level);
                level--;                
            }
        } else {
            level++;
            List<PolicyComponent> children = CastUtils.cast(((PolicyOperator)pc).getPolicyComponents(),
                PolicyComponent.class);
            nl(buf);
            for (PolicyComponent child : children) {
                printPolicyComponent(child, buf, level);
            }
            level--;
        }
    }
    
    private static void indent(StringBuilder buf, int level) {
        for (int i = 0; i < level; i++) {
            buf.append(INDENT);
        }
    }
    
    private static void nl(StringBuilder buf) {
        buf.append(System.getProperty("line.separator"));
    }
    
    private static String typeToString(short type) {
        switch(type) {
        case Constants.TYPE_ASSERTION:
            return "Assertion";
        case Constants.TYPE_ALL:
            return "All";
        case Constants.TYPE_EXACTLYONE:
            return "ExactlyOne";
        case Constants.TYPE_POLICY:
            return "Policy";
        case Constants.TYPE_POLICY_REF:
            return "PolicyReference";
        default:
            break;
        }
        return "";
    }
    
}
