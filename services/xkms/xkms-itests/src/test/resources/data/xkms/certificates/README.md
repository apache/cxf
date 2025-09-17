## Regenerate CA w/o CRL:
 - Generate new key and CA: `openssl req -new -x509 -days 3650 -key trusted_cas/ca.key -out trusted_cas/ca.crt`
 - Convert to DER: `openssl crl -inform PEM -in trusted_cas/ca.crt -outform DER -out trusted_cas/wss40CA.cer`
 - Create `ca.conf`:
    ```
     [ ca ]
     default_ca = myca

     [ crl_ext ]
     # issuerAltName=issuer:copy 
     authorityKeyIdentifier=keyid:always,issuer:always
   
     [ myca ]
     dir = ./
     new_certs_dir = $dir
     unique_subject = no
     certificate = $dir/trusted_cas/wss40CA.cer
     database = $dir/certindex
     private_key = $dir/trusted_cas/ca.key
     serial = $dir/certserial
     default_days = 3650
     default_md = sha1
     policy = myca_policy
     crlnumber = $dir/crlnumber
     default_crl_days = 3650
     x509_extensions = myca_extensions
     default_bits  = 1024
    
     [ myca_policy ]
     commonName = supplied
     stateOrProvinceName = supplied
     countryName = optional
     emailAddress = optional
     organizationName = supplied
     organizationalUnitName = optional
     localityName = supplied
    
     [ myca_extensions ]
     basicConstraints = CA:false
     subjectKeyIdentifier = hash
     authorityKeyIdentifier = keyid:always,issuer:always
     nsComment = OpenSSL Generated Certificate
   ```
 - Run these commands:
    ```
    touch certindex
    echo 01 > certserial
    echo 01 > crlnumber
    ```
 - Create CSR: `openssl req -new -key cert.key -out cert.csr`
 - Create certificate: `openssl ca -batch -config ca.conf -notext -in cert.csr -out cert.crt`
 - Convert to DER: `openssl x509 -inform PEM -in cert.crt -outform DER -out wss40.cer`
 - Generate CRL `openssl ca -config ca.conf -gencrl -keyfile trusted_cas/ca.key -cert trusted_cas/wss40CA.cer -out rt.crl.pem`
 - Convert to DER: `openssl crl -inform PEM -in rt.crl.pem -outform DER -out crls/wss40CACRL.cer`
 