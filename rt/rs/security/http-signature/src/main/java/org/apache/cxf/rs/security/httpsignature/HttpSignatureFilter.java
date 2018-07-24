package org.apache.cxf.rs.security.httpsignature;

import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.container.*;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Key;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tomitribe.auth.signatures.Signature;
import org.tomitribe.auth.signatures.Signer;

@Provider
@PreMatching
public class HttpSignatureFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private Map<String, Key> keys = new HashMap<>();
    private Signature signature;

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        final Signer signer = new Signer(keys.get(signature.getKeyId()), signature);
        final Map<String, String> headers = mapHeaders(request.getHeaders());

        final Signature signedSignature = signer.sign(request.getMethod(), request.getUriInfo().getPath(), headers);
        request.getHeaders().putSingle("Authorization", signedSignature.toString());

    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException  {
        // Get first Authorization header if there are multiple?
        final Signature headerSignature = Signature.fromString((request.getHeaders().get("Authorization")).get(0));
        final Signer signer = new Signer(keys.get(headerSignature.getKeyId()), headerSignature);
        final Map<String, String> headers = mapHeaders(request.getHeaders());
        final String expectedSignature = signer.sign(request.getMethod(), request.getUriInfo().getPath(), headers)
                                              .getSignature();
        if (!expectedSignature.equals(headerSignature.getSignature())) {
            // Header has been modified, react with abort, throw exception or something
        }
    }

    private Map<String, String> mapHeaders(MultivaluedMap<String, String> multivaluedMap) {
        Map<String, String> mappedStrings = new HashMap<>();
        for (String key : multivaluedMap.keySet()) {
            mappedStrings.put(key, concatValues(multivaluedMap.get(key)));
        }
        return mappedStrings;
    }

    private String concatValues(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < values.size(); x++) {
            sb.append(values.get(x));
            if (x != values.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    public void setKeys(Map<String, Key> keys){
        this.keys = keys;
    }

    public Map<String, Key> getKeys(){
        return keys;
    }

    public void setSignature(String authorization) {
        signature = Signature.fromString(authorization);
    }

    public void setSignature(Signature signature) {
        this.signature = signature;
    }

    public Signature getSignature() {
        return signature;
    }
}