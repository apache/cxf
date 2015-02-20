
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
# This file generates a number of keys/certificates and keystores for 
# names to be used with corresponding CXF configuration files (*.cxf).
#

#
# Start with a clean slate. Remove all keystores.
#
rm -f *.jks
rm -f Truststore.pem

#
# This function generates a key/self-signed certificate with the following DN.
#  "CN=$1, OU=$2, O=ApacheTest, L=Syracuse, C=US" and adds it to 
# the truststore.
#
function genkey {
    keytool -genkey -alias $2 -keystore $2.jks -dname "CN=$1, OU=$2, O=ApacheTest, L=Syracuse, C=US" -keyalg RSA -keypass password -storepass password -storetype jks -validity 10000
    keytool -export -file $2.cer -alias $2 -keystore $2.jks -storepass password
    keytool -import -file $2.cer -alias $2 -noprompt -keystore Truststore.jks -storepass password
}

#
# We generate keys/certificates with the following CN=<name> OU=<name>
# The CN used to be "localhost" to conform to the default HostnameVerifier of
# HttpsURLConnection so it would work for tests. However, we have enhanced
# the HTTP Conduit logic to accept anything in the CN in favor of the 
# MessageTrustDecider callback making the verification determination.
#
for name in Bethal Gordy Tarpin Poltim Morpit
do
   genkey $name $name
   keytool -export -keystore Truststore.jks -storepass password -alias $i -rfc >> Truststore.pem
done

