WS-Security Demos 
=================

These demos shows how WS-Security support in Apache CXF may be enabled.

WS-Security can be enabled in CXF in two ways:

a) WS-Security can be configured to the Client and Server endpoints by adding
WSS4JInterceptors. Both Server and Client can be configured for outgoing and
incoming interceptors. Various Actions like, Timestamp, UsernameToken,
Signature, Encryption, etc., can be applied to the interceptors by passing
appropriate configuration properties.

b) WS-Security can be enabled automatically by using WS-SecurityPolicy, and
by defining some configuration information (keys, usernames, etc.).

In addition, CXF 3.0.0 supports both a DOM-based (in-memory) and StAX-based
(streaming) approach to WS-Security. Each demo gives options for running the
demo using either approach.

The demos show the following functionality:

 - sign_enc: How to configure the "Action" based approach to WS-Security in
             CXF. The demo shows how to configure Signature, Encryption,
             Timestamps and UsernameTokens.
 - sign_enc_policy: How to configure CXF using WS-SecurityPolicy to sign and
                    encrypt the SOAP Body, to encrypt a UsernameToken and to
                    include a signed Timestamp.
 - ut: How to simply include a UsernameToken using the "Action" based approach.
 - ut_policy: How to include a UsernameToken using WS-SecurityPolicy.

