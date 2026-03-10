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
  Use followig data:
  ```
    E=client@client.com
    CN=www.client.com
    OU=IT Department
    O=Sample Client -- NOT FOR PRODUCTION
    L=Niagara Falls
    S=New York
    C=US
  ```
2. `openssl req -x509 -newkey rsa:4096 -keyout myservicekey.pem -out myservicekey.cert -days 3650 -nodes`
  Use followig data:
  ```
    E=service@service.com
    CN=www.service.com
    OU=IT Department
    O=Sample Web Service Provider -- NOT FOR PRODUCTION
    L=Buffalo
    S=New York
    C=US
  ```
3. `openssl req -x509 -newkey rsa:4096 -keyout mystskey.pem -out myservicekey.cert -days 3650 -nodes`
  Use followig data:
  ```
    E=sts@sts.com
    CN=www.sts.com
    OU=IT Department
    O=Sample STS -- NOT FOR PRODUCTION
    L=Baltimore
    S=Maryland
    C=US
  ```

Update stsstore.jks
####

1. `keytool -import -alias myclientkey -file myclientkey.cert -keystore stsstore.jks -trustcacerts`
2. `keytool -import -alias myservicekey -file myservicekey.cert -keystore stsstore.jks -trustcacerts`
3. `openssl pkcs12 -export -out mystskey.p12 -inkey mystskey.pem -in mystskey.cert -name mystskey`
4. `keytool -importkeystore -deststorepass stsspass -destkeystore clientstore.jks -srckeystore mystskey.p12 -srcstoretype PKCS12 -alias mystskey -destkeypass stsspass`

Update clientstore.jks
####

1. `keytool -import -alias mystskey -file mystskey.cert -keystore clientstore.jks -trustcacerts`
2. `keytool -import -alias myservicekey -file myservicekey.cert -keystore clientstore.jks -trustcacerts`
3. `openssl pkcs12 -export -out myclientkey.p12 -inkey myclientkey.pem -in myclientkey.cert -name myclientkey`
4. `keytool -importkeystore -deststorepass cspass -destkeystore clientstore.jks -srckeystore myclientkey.p12 -srcstoretype PKCS12 -alias myclientkey -destkeypass cspass`

Update servicestore.jks
####

1. `keytool -import -alias mystskey -file mystskey.cert -keystore servicestore.jks -trustcacerts`
2. `keytool -import -alias myclientkey -file myclientkey.cert -keystore stsstore.jks -trustcacerts`
3. `openssl pkcs12 -export -out myservicekey.p12 -inkey myservicekey.pem -in myservicekey.cert -name myservicekey`
4. `keytool -importkeystore -deststorepass sspass -destkeystore servicestore.jks -srckeystore myservicekey.p12 -srcstoretype PKCS12 -alias myservicekey -destkeypass sspass`


How to create / update cxfca.jks, alice.jks, bob.jks and sts.jks
####

cxfca is a self-signed certificate, where the corresponding private key is used to sign the alice and bob keys and sts keys. To generate them follow the process listed in https://github.com/apache/ws-wss4j/blob/8b4799f16582cb335a8d8a3f0ea7d41027231cd8/ws-security-dom/src/test/java/org/apache/wss4j/dom/message/SignatureCertTest.java#L60
