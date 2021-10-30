# The below scripts show the commands used to generate the self-signed keys for this sample.
# If you use the below script to create your own keys be sure to change the passwords used here
# DO NOT USE THE SUPPLIED KEYS IN PRODUCTION--everyone has them!!
# For production recommended to use keys signed by a third-party certificate authority (CA)

# Create the combination keystore/truststore for the server.
keytool -genkeypair -keyalg RSA -keysize 4096 -validity 730 -alias myservicekey -keystore serviceKeystore.jks -dname "cn=localhost" -keypass skpass -storepass sspass

