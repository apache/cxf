#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
#
# This file uses openssl and keytool to generate 2 chains of 3 certificates 
# CN=Wibble             CN=Cherry
#             CN=TheRA
#             CN=TheCA
# and generates a CRL to revoke the "CN=TheRA" certificate.
#
# This file also serves as a specification on what needs to be done to
# get the underlying CXF to work correctly.
# For the most part, you need to use only JKS (Java Key Store) formatted
# keystores and truststores.


# Initialize the default openssl DataBase.
# According to a default /usr/lib/ssl/openssl.cnf file it is ./demoCA
# Depending on the Openssl version, comment out "crlnumber" in config file.
# We echo 1345 to start the certificate serial number counter.

    rm -rf demoCA
    mkdir -p demoCA/newcerts
    cp /dev/null demoCA/index.txt
    echo "1345" > demoCA/serial

# This file makes sure that the certificate for CN=TheRA can be a Certificate
# Authority, i.e. can sign the user certificates, e.g. "CN=Wibble".

cat <<EOF > exts
[x509_extensions]
basicConstraints=CA:TRUE
EOF

# Create the CA's keypair and self-signed certificate
#   -x509 means create self-sign cert
#   -keyout means generate keypair
#   -nodes means do not encrypt private key.
#   -set_serial sets the serial number of the certificate

    openssl req -verbose -x509 -new -nodes -set_serial 1234 \
    -subj "/CN=TheCA/OU=NOT FOR PRODUCTION/O=Apache/ST=NY/C=US" \
    -days 7300 -out cacert.pem -keyout caprivkey.pem 

# Create the RA's keypair and Certificate Request
#    without -x509, we generate an x509 cert request.
#   -keyout means generate keypair
#   -nodes means do not encrypt private key.

    openssl req -verbose -new -nodes \
    -subj "/CN=TheRA/OU=NOT FOR PRODUCTION/O=Apache/ST=NY/C=US" \
    -days 7300 -out csrra.pem -keyout raprivkey.pem 

# Have the CN=TheCA issue a certificate for the CN=TheRA
# We need -extfile exts -extenstions x509_extensions to make sure 
# CN=TheRA can be a Certificate Authority.

    openssl ca -batch -days 7300 -cert cacert.pem -keyfile caprivkey.pem \
    -in csrra.pem -out ra-ca-cert.pem -extfile exts -extensions x509_extensions

# Create keypairs and Cert Request for a certificate for CN=Wibble and CN=Cherry
# This procedure must be done in JKS, because we need to use a JKS keystore.
# The current version of CXF using PCKS12 will not work for a number of 
# internal CXF reasons.

    rm -f wibble.jks

    keytool -genkey \
    -dname "CN=Wibble, OU=NOT FOR PRODUCTION, O=Apache, ST=NY, C=US" \
    -keystore wibble.jks -storetype jks -storepass password -keypass password

    keytool -certreq -keystore wibble.jks -storetype jks -storepass password \
    -keypass password -file csrwibble.pem


    rm -f cherry.jks

    keytool -genkey \
    -dname "CN=Cherry, OU=NOT FOR PRODUCTION, O=Apache, ST=NY, C=US" \
    -keystore cherry.jks -storetype jks -storepass password -keypass password

    keytool -certreq -keystore cherry.jks -storetype jks -storepass password \
    -keypass password -file csrcherry.pem


# Have the CN=TheRA issue a certificate for CN=Wibble and CN=Cherry via
# their Certificate Requests.

   openssl ca -batch -days 7300 -cert ra-ca-cert.pem -keyfile raprivkey.pem \
   -in csrwibble.pem -out wibble-ra-cert.pem 
   
   openssl ca -batch -days 7300 -cert ra-ca-cert.pem -keyfile raprivkey.pem \
   -in csrcherry.pem -out cherry-ra-cert.pem


# Rewrite the certificates in PEM only format. This allows us to concatenate
# them into chains.

    openssl x509 -in cacert.pem -out cacert.pem -outform PEM
    openssl x509 -in ra-ca-cert.pem -out ra-ca-cert.pem -outform PEM
    openssl x509 -in wibble-ra-cert.pem -out wibble-ra-cert.pem -outform PEM
    openssl x509 -in cherry-ra-cert.pem -out cherry-ra-cert.pem -outform PEM

# Create a chain readable by CertificateFactory.getCertificates.

    cat wibble-ra-cert.pem ra-ca-cert.pem cacert.pem > wibble.chain
    cat cherry-ra-cert.pem ra-ca-cert.pem cacert.pem > cherry.chain

# Replace the certificate in the Wibble keystore with their respective
# full chains.

    keytool -import -file wibble.chain -keystore wibble.jks -storetype jks \
    -storepass password -keypass password -noprompt

    keytool -import -file cherry.chain -keystore cherry.jks -storetype jks \
    -storepass password -keypass password -noprompt

# Revoke the CN=TheRA certificate (happens in the Openssl DB)

    openssl ca -verbose -cert cacert.pem -keyfile caprivkey.pem \
    -revoke ra-ca-cert.pem -crl_reason keyCompromise 

# Create the CRL from that revocation (from the Openssl DB)

    openssl ca -verbose -gencrl -out ca.crl -cert cacert.pem \
    -keyfile caprivkey.pem

# Create the Truststore file containing the CA cert.

    rm -f truststore.jks
    
    keytool -import -file cacert.pem -alias TheCA -keystore truststore.jks \
    -storepass password -noprompt

# Uncomment to see what's in the Keystores and CRL

    keytool -v -list -keystore wibble.jks -storepass password
    
    keytool -v -list -keystore cherry.jks -storepass password
    
    keytool -v -list -keystore truststore.jks -storepass password
    
    openssl crl -in ca.crl -text -noout

# Get rid of everything but wibble.chain and ra.crl
#rm -rf *.pem exts demoCA *pk12
