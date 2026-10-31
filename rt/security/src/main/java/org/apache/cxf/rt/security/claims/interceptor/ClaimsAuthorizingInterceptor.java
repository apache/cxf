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
package org.apache.cxf.rt.security.claims.interceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.rt.security.claims.ClaimBean;
import org.apache.cxf.rt.security.claims.ClaimsSecurityContext;
import org.apache.cxf.rt.security.claims.SAMLClaim;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.security.claims.authorization.Claim;
import org.apache.cxf.security.claims.authorization.ClaimMode;
import org.apache.cxf.security.claims.authorization.Claims;


public class ClaimsAuthorizingInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getL7dLogger(ClaimsAuthorizingInterceptor.class);

    private static final Set<String> SKIP_METHODS = new HashSet<>(
            Arrays.asList("wait", "notify", "notifyAll", "equals", "toString", "hashCode"));

    // Claims configured explicitly via setClaims, keyed by method name
    private Map<String, List<ClaimBean>> claims = new HashMap<>();
    // The method-level and class-level @Claim/@Claims declarations which apply to each method
    // signature in the secured object's hierarchy. Keying by signature means that overloaded
    // methods do not share a rule
    private final Map<String, List<ClaimsDeclaration>> declarationsBySignature = new HashMap<>();
    // The erased signatures a method overrides with a different signature, e.g. update(Object)
    // for update(String) implementing update(T) of Repository<String>
    private final Map<String, Set<String>> overriddenSignatures = new HashMap<>();
    private final Map<String, Set<String>> signaturesByName = new HashMap<>();
    private final Set<Class<?>> securedClasses = new HashSet<>();
    // The classes passed to setSecuredObject, so their claims can be resolved again
    // if the aliases change afterwards
    private final List<Class<?>> securedObjectClasses = new ArrayList<>();
    private boolean annotationClaimsFound;
    private Map<String, String> nameAliases = Collections.emptyMap();
    private Map<String, String> formatAliases = Collections.emptyMap();

    public ClaimsAuthorizingInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    public void handleMessage(Message message) throws Fault {
        SecurityContext sc = message.get(SecurityContext.class);
        if (!(sc instanceof ClaimsSecurityContext)) {
            throw new AccessDeniedException("Security Context is unavailable or unrecognized");
        }

        Method method = MessageUtils.getTargetMethod(message).orElseThrow(() ->
            new AccessDeniedException("Method is not available : Unauthorized"));

        if (authorize((ClaimsSecurityContext)sc, method)) {
            return;
        }

        throw new AccessDeniedException("Unauthorized");
    }

    public void setClaims(Map<String, List<ClaimBean>> claimsMap) {
        claims.putAll(claimsMap);
    }

    protected boolean authorize(ClaimsSecurityContext sc, Method method) {
        String signature = ReflectionUtil.getMethodSignature(method);
        boolean securedMethod = declarationsBySignature.containsKey(signature);
        Set<String> signatures = new HashSet<>();
        if (securedMethod) {
            signatures.add(signature);
            signatures.addAll(overriddenSignatures.getOrDefault(signature, Collections.emptySet()));
        } else {
            // The method is not one of the secured object's methods, so every rule
            // recorded for its name applies
            signatures.addAll(signaturesByName.getOrDefault(method.getName(), Collections.emptySet()));
        }
        List<ClaimBean> requiredClaims = new ArrayList<>();
        for (String s : signatures) {
            requiredClaims.addAll(getAnnotationClaims(s));
        }
        List<ClaimBean> configuredClaims = claims.get(method.getName());
        if (configuredClaims != null) {
            requiredClaims.addAll(configuredClaims);
        } else if (!securedMethod && requiredClaims.isEmpty()) {
            // A method without claims requirements is only permitted if it is one of
            // the secured object's methods
            return false;
        }
        // Every claim which applies to the method has to be matched
        return isAuthorized(sc, requiredClaims);
    }

    private List<ClaimBean> getAnnotationClaims(String signature) {
        // Every @Claim which applies to the method has to be met, from any class or interface
        // in the hierarchy. A class-level @Claim is replaced by a method-level @Claim of the same
        // name and format declared on the same class or interface, or on a subclass of it
        List<ClaimsDeclaration> declarations =
            declarationsBySignature.getOrDefault(signature, Collections.emptyList());
        List<ClaimBean> requiredClaims = new ArrayList<>();
        for (ClaimsDeclaration declaration : declarations) {
            for (ClaimBean bean : declaration.claims) {
                if (declaration.methodLevel
                    || !isClaimOverridden(bean, declaration.declaringClass, declarations)) {
                    requiredClaims.add(bean);
                }
            }
        }
        return requiredClaims;
    }

    private static boolean isClaimOverridden(ClaimBean bean, Class<?> cls,
                                             List<ClaimsDeclaration> declarations) {
        for (ClaimsDeclaration declaration : declarations) {
            if (declaration.methodLevel && cls.isAssignableFrom(declaration.declaringClass)
                && isClaimOverridden(bean, declaration.claims)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAuthorized(ClaimsSecurityContext sc, List<ClaimBean> list) {
        org.apache.cxf.rt.security.claims.ClaimCollection actualClaims = sc.getClaims();

        for (ClaimBean claimBean : list) {
            org.apache.cxf.rt.security.claims.Claim matchingClaim = null;
            for (org.apache.cxf.rt.security.claims.Claim cl : actualClaims) {
                if (cl instanceof SAMLClaim) {
                    // If it's a SAMLClaim the name + nameformat must match what's configured
                    if (((SAMLClaim)cl).getName().equals(claimBean.getClaim().getClaimType())
                        && ((SAMLClaim)cl).getNameFormat().equals(claimBean.getClaimFormat())) {
                        matchingClaim = cl;
                        break;
                    }
                } else if (cl.getClaimType().equals(claimBean.getClaim().getClaimType())) {
                    matchingClaim = cl;
                    break;
                }
            }
            if (matchingClaim == null) {
                if (claimBean.getClaimMode() == ClaimMode.STRICT) {
                    return false;
                }
                continue;
            }
            List<Object> claimValues = claimBean.getClaim().getValues();
            List<Object> matchingClaimValues = matchingClaim.getValues();
            if (claimBean.isMatchAll()
                && !matchingClaimValues.containsAll(claimValues)) {
                return false;
            }
            boolean matched = false;
            for (Object value : matchingClaimValues) {
                if (claimValues.contains(value)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    public void setSecuredObject(Object object) {
        Class<?> cls = ClassHelper.getRealClass(object);
        securedObjectClasses.add(cls);
        findClaims(cls);
        if (!annotationClaimsFound) {
            LOG.warning("The claims list is empty, the service object is not protected");
        }
    }

    protected void findClaims(Class<?> cls) {
        if (cls == null || cls == Object.class || !securedClasses.add(cls)) {
            return;
        }
        List<ClaimBean> clsClaims =
            getClaims(cls.getAnnotation(Claims.class), cls.getAnnotation(Claim.class));
        for (Method m : cls.getMethods()) {
            if (SKIP_METHODS.contains(m.getName())) {
                continue;
            }
            List<ClaimBean> methodClaims =
                getClaims(m.getAnnotation(Claims.class), m.getAnnotation(Claim.class));

            String signature = ReflectionUtil.getMethodSignature(m);
            List<ClaimsDeclaration> declarations =
                declarationsBySignature.computeIfAbsent(signature, k -> new ArrayList<>());
            if (!methodClaims.isEmpty()) {
                declarations.add(new ClaimsDeclaration(m.getDeclaringClass(), methodClaims, true));
            }
            if (!clsClaims.isEmpty()) {
                declarations.add(new ClaimsDeclaration(cls, clsClaims, false));
            }
            signaturesByName.computeIfAbsent(m.getName(), k -> new HashSet<>()).add(signature);
            annotationClaimsFound |= !methodClaims.isEmpty() || !clsClaims.isEmpty();
            if (!m.isBridge()) {
                for (Method overridden : ReflectionUtil.getGenericallyOverriddenMethods(cls, m)) {
                    overriddenSignatures.computeIfAbsent(signature, k -> new HashSet<>())
                        .add(ReflectionUtil.getMethodSignature(overridden));
                }
            }
        }

        findClaims(cls.getSuperclass());
        for (Class<?> interfaceCls : cls.getInterfaces()) {
            findClaims(interfaceCls);
        }
    }

    private static boolean isClaimOverridden(ClaimBean bean, List<ClaimBean> mClaims) {
        for (ClaimBean methodBean : mClaims) {
            if (bean.getClaim().getClaimType().equals(methodBean.getClaim().getClaimType())
                && bean.getClaimFormat().equals(methodBean.getClaimFormat())) {
                return true;
            }
        }
        return false;
    }

    private List<ClaimBean> getClaims(
            Claims claimsAnn, Claim claimAnn) {
        List<ClaimBean> claimsList = new ArrayList<>();

        final List<Claim> annClaims;
        if (claimsAnn != null) {
            annClaims = Arrays.asList(claimsAnn.value());
        } else if (claimAnn != null) {
            annClaims = Collections.singletonList(claimAnn);
        } else {
            annClaims = Collections.emptyList();
        }
        for (Claim ann : annClaims) {
            org.apache.cxf.rt.security.claims.Claim claim = new org.apache.cxf.rt.security.claims.Claim();

            String claimName = ann.name();
            if (nameAliases.containsKey(claimName)) {
                claimName = nameAliases.get(claimName);
            }
            String claimFormat = ann.format();
            if (formatAliases.containsKey(claimFormat)) {
                claimFormat = formatAliases.get(claimFormat);
            }

            claim.setClaimType(claimName);
            for (String value : ann.value()) {
                claim.addValue(value);
            }

            claimsList.add(new ClaimBean(claim, claimFormat, ann.mode(), ann.matchAll()));
        }
        return claimsList;
    }

    public void setNameAliases(Map<String, String> nameAliases) {
        this.nameAliases = nameAliases == null ? Collections.emptyMap() : nameAliases;
        resolveSecuredObjectClaims();
    }

    public void setFormatAliases(Map<String, String> formatAliases) {
        this.formatAliases = formatAliases == null ? Collections.emptyMap() : formatAliases;
        resolveSecuredObjectClaims();
    }

    /**
     * The aliases are applied when the annotation claims are read, so read them again for any
     * secured object which was set before the aliases. Otherwise the claims would keep the
     * unresolved names, which never match, and a ClaimMode.LAX claim would always pass.
     */
    private void resolveSecuredObjectClaims() {
        if (securedObjectClasses.isEmpty()) {
            return;
        }
        declarationsBySignature.clear();
        overriddenSignatures.clear();
        signaturesByName.clear();
        securedClasses.clear();
        annotationClaimsFound = false;
        for (Class<?> cls : securedObjectClasses) {
            findClaims(cls);
        }
    }

    private static final class ClaimsDeclaration {
        private final Class<?> declaringClass;
        private final List<ClaimBean> claims;
        private final boolean methodLevel;

        ClaimsDeclaration(Class<?> declaringClass, List<ClaimBean> claims, boolean methodLevel) {
            this.declaringClass = declaringClass;
            this.claims = claims;
            this.methodLevel = methodLevel;
        }
    }
}
