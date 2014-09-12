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

package org.apache.cxf.sts.claims.mapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.cxf.sts.claims.Claim;
import org.apache.cxf.sts.claims.ClaimCollection;

public class ClaimUtils {

    /**
     * @param targetCollection Collection that should be used to add further claims to
     * @param claims Claims to be added to the provided collection
     * @return Returns provided collection including provided claims
     */
    public ClaimCollection add(ClaimCollection targetCollection, Claim... claims) {
        for (Claim c : claims) {
            if (c != null) {
                targetCollection.add(c);
            }
        }
        return targetCollection;
    }

    /**
     * @param targetCollection Collection that should be used to add claims from the other provided claim
     *            collections
     * @param claimCollections All claims contained within the provided collections will be added to the
     *            targetCollection
     * @return Returns claim collection containing all claims that have been provided
     */
    public ClaimCollection add(ClaimCollection targetCollection,
        ClaimCollection... claimCollections) {
        for (ClaimCollection cc : claimCollections) {
            targetCollection.addAll(cc);
        }
        return targetCollection;
    }

    /**
     * @param claimTypeURI claim type URI
     * @param values values of created claim. Can be null if no values shall be added to claim.
     * @return Returns new claim with provided claim type and values
     */
    public Claim create(String claimTypeURI, String... values) {
        Claim claim = new Claim();
        if (claimTypeURI != null) {
            claim.setClaimType(URI.create(claimTypeURI));
        }
        if (values != null) {
            claim.getValues().addAll(Arrays.asList(values));
        }
        return claim;
    }

    /**
     * @param claims Collection of multiple claims with different claim types
     * @param claimType URI of claim type to be selected from claim collection
     * @return Returns first claim from claims collection matching the provided claimType
     */
    public Claim get(ClaimCollection claims, String claimType) {
        if (claimType == null || claims == null) {
            return null;
        }
        for (Claim c : claims) {
            if (c.getClaimType() != null && claimType.equals(c.getClaimType().toString())) {
                return c;
            }
        }
        return null;
    }

    /**
     * @param claims Collection of claims to be mapped to a differnt claim type
     * @param map Map of old:new claim types
     * @param keepUnmapped if set to false only claims with a claim type contained in the map will be
     *            returned. If set to false claims with an unmapped claim type will also be returned.
     * @return Returns claim collection with mapped claim types
     */
    public ClaimCollection mapType(ClaimCollection claims, Map<String, String> map,
        boolean keepUnmapped) {
        ClaimCollection mappedClaims = new ClaimCollection();
        if (claims != null && map != null) {
            for (Claim c : claims) {
                String claimType = (c.getClaimType() != null)
                    ? c.getClaimType().toString()
                    : "";
                String mappedClaimType = map.get(claimType);
                if (mappedClaimType != null) {
                    Claim claim = c.clone();
                    claim.setClaimType(URI.create(mappedClaimType));
                    mappedClaims.add(claim);
                } else if (keepUnmapped) {
                    mappedClaims.add(c.clone());
                }
            }
        }
        return mappedClaims;
    }

    /**
     * @param claim Claim providing values to be mapped
     * @param map Map of old:new mapping values
     * @param keepUnmapped if set to false only values contained in the map will be returned. If set to true,
     *            values not contained in the map will also remain in the returned claim.
     * @return Returns the provided claim with mapped values
     */
    public Claim mapValues(Claim claim, Map<String, String> mapping, boolean keepUnmapped) {

        if (claim != null) {
            List<String> values = claim.getValues();
            List<String> mappedValues = new ArrayList<String>();

            if (values == null || mapping == null || mapping.size() == 0) {
                claim.setValues(mappedValues);
                return claim;
            }

            for (String value : values) {
                String newValue = mapping.get(value);
                if (newValue != null) {
                    mappedValues.add(newValue);
                } else if (keepUnmapped) {
                    mappedValues.add(value);
                }
            }
            claim.setValues(mappedValues);
        }
        return claim;
    }

    /**
     * @param claims Collection of claims containing claims with claim types of listed <code>claimType</code>
     *            array
     * @param targetClaimType claim type URI of merged result claim
     * @param delimiter Delimiter added between multiple claim types. Value can be <code>null</code>.
     * @param claimType URIs of claim types to be merged. Merging will be in the same order as the provided
     *            claim type URIs. If a claim type is not found in the collection this claim type will be
     *            omitted.
     * @return Returns merged claim of all found claim types
     */
    public Claim merge(ClaimCollection claims, String targetClaimType, String delimiter,
        String... claimType) {
        Claim mergedClaim = null;
        StringBuilder sbClaimValue = new StringBuilder();
        for (String sc : claimType) {
            Claim c = get(claims, sc);
            if (c != null) {
                List<String> values = c.getValues();
                if (values != null && values.size() > 0) {
                    if (mergedClaim == null) {
                        // First match TODO refactor for better method override
                        mergedClaim = c.clone();
                        sbClaimValue.append(values.get(0));
                        mergedClaim.getValues().clear();
                    } else {
                        sbClaimValue.append(delimiter).append(values.get(0));
                    }
                }
            }
        }
        if (mergedClaim != null) {
            mergedClaim.setClaimType(URI.create(targetClaimType));
            mergedClaim.addValue(sbClaimValue.toString());
        }

        return mergedClaim;
    }

    /**
     * @param claim Claim to be updated
     * @param claimTypeURI URI as String to be set as claim type in provided claim
     * @return Returns updated claim
     */
    public Claim setType(Claim claim, String claimTypeURI) {
        claim.setClaimType(URI.create(claimTypeURI));
        return claim;
    }

    /**
     * All claims within the provided collection will be updated in the following manner: If no original
     * issuer is set, the issuer in the provided claims will be set as original issuer. If an original issuer
     * was already set before, the original issuer will not be updated. All claims will be updated to have the
     * provided issuer name be set as the claim issuer.
     * 
     * @param claims Collection of claims to be updated
     * @param issuerName Issuer to be set for all claims within the collection
     * @return Returns updated claim collection
     */
    public ClaimCollection updateIssuer(ClaimCollection claims, String newIssuer) {
        for (Claim c : claims) {
            if (c.getOriginalIssuer() == null) {
                c.setOriginalIssuer(c.getIssuer());
            }
            c.setIssuer(newIssuer);
        }
        return claims;
    }

    /**
     * @param claim values of this claim will be transformed to uppercase format
     * @return Returns claim with values all in uppercase format
     */
    public Claim upperCaseValues(Claim claim) {
        if (claim != null && claim.getValues() != null) {
            List<String> oldValues = claim.getValues();
            List<String> newValues = new ArrayList<String>();
            for (String value : oldValues) {
                newValues.add(value.toUpperCase());
            }
            claim.getValues().clear();
            claim.getValues().addAll(newValues);
        }
        return claim;
    }

    /**
     * @param claim values of this claim will be transformed to lowercase format
     * @return Returns claim with values all in lowercase format
     */
    public Claim lowerCaseValues(Claim claim) {
        if (claim != null && claim.getValues() != null) {
            List<String> oldValues = claim.getValues();
            List<String> newValues = new ArrayList<String>();
            for (String value : oldValues) {
                newValues.add(value.toLowerCase());
            }
            claim.getValues().clear();
            claim.getValues().addAll(newValues);
        }
        return claim;
    }

    /**
     * @param claim Claim providing values to be wrapped
     * @param prefix Prefix to be added to each claim value. Can be null.
     * @param suffix Suffix to be appended to each claim value. Can be null.
     * @return Returns the provided claim with wrapped values
     */
    public Claim wrapValues(Claim claim, String prefix, String suffix) {
        prefix = (prefix == null)
            ? ""
            : prefix;
        suffix = (suffix == null)
            ? ""
            : suffix;
        if (claim != null && claim.getValues() != null) {
            List<String> oldValues = claim.getValues();
            List<String> newValues = new ArrayList<String>();
            for (String value : oldValues) {
                newValues.add(prefix + value + suffix);
            }
            claim.getValues().clear();
            claim.getValues().addAll(newValues);
        }
        return claim;
    }

    /**
     * This function is especially useful if multi values from a claim are stored within a single value entry.
     * 
     * For example multi user roles could all be stored in a single value element separated by comma:
     * USER,MANAGER,ADMIN 
     * The result of this function will provide a claim with three distinct values: 
     * USER and MANAGER and ADMIN.
     * 
     * @param claim claim containing multi-values in a single value entry
     * @param delimiter Delimiter to split multi-values into single values
     * @return Returns a Processed Claim containing only single values per value entry
     */
    public Claim singleToMultiValue(Claim claim, String delimiter) {
        if (claim != null && claim.getValues() != null) {
            List<String> oldValues = claim.getValues();
            List<String> newValues = new ArrayList<String>();
            for (String multivalue : oldValues) {
                StringTokenizer st = new StringTokenizer(multivalue, delimiter);
                while (st.hasMoreTokens()) {
                    newValues.add(st.nextToken());
                }
            }
            claim.getValues().clear();
            claim.getValues().addAll(newValues);
        }
        return claim;
    }
    
    /**
     * This function is especially useful if values from multiple claim values need to be condensed into a
     * single value element. 
     * 
     * For example a user has three roles: USER and MANAGER and ADMIN. If ',' is used as a delimiter, 
     * then this method would provide the following claim with only a single value looking like this:
     * USER,MANAGER,ADMIN
     * 
     * @param claim claim containing multi-values
     * @param delimiter Delimiter to concatenate multi-values into a single value
     * @return Returns a Processed Claim containing only one single value
     */
    public Claim multiToSingleValue(Claim claim, String delimiter) {
        if (claim != null && claim.getValues() != null) {
            List<String> oldValues = claim.getValues();
            boolean first = true;
            StringBuilder sb = new StringBuilder();
            for (String value : oldValues) {
                if (first) {
                    sb.append(value);
                    first = false;
                } else {
                    sb.append(delimiter).append(value);
                }
            }
            claim.getValues().clear();
            claim.getValues().add(sb.toString());
        }
        return claim;
    }
}
