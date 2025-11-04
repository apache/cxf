How to create / update certs and truststores 
###

1. `openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 3650 -nodes`
2. `openssl pkcs12 -export -out keyStore.p12 -inkey key.pem -in cert.pem`
3. `cat cert.pem key.pem > combined.pem`
4. `keytool -import -trustcacerts -alias <alias> -file combined.pem -keystore <truststore>.jks`
    Optinally, delete existing alias: `keytool -delete  -alias <alias> -keystore <truststore>.jks`
5. `keytool -importkeystore -srckeystore keyStore.p12 -srcstoretype pkcs12 -destalias <alias> -srcalias 1 -destkeystore <keystore.jks>`




How to create / update stsstore.jks, clientstore.jks and servicestore.jks
###

1. `openssl req -x509 -newkey rsa:4096 -keyout myclientkey.pem -out myclientkey.cert -days 3650 -nodes`
2. `keytool -import -alias myclientkey -file myclientkey.cert -keystore stsstore.jks -trustcacerts`
3. `openssl req -x509 -newkey rsa:4096 -keyout myservicekey.pem -out myservicekey.cert -days 3650 -nodes`
4. `openssl pkcs12 -export -out mystskey.p12 -inkey myservicekey.pem -in myservicekey.cert -name mystskey`
5. `keytool -importkeystore -deststorepass stsspass -destkeystore clientstore.jks -srckeystore mystskey.p12 -srcstoretype PKCS12 -alias mystskey -destkeypass stspass`
6. `keytool -import -alias myservicekey -file myservicekey.cert -keystore stsstore.jks -trustcacerts`