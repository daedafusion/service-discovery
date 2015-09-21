#!/bin/bash

# Generate Root Private Key
# add -des3 or -aes256 to password protect
openssl genrsa -out root.key 2048

# Generate self-signed Root CA
openssl req -x509 -new -nodes -key root.key -days 1024 -out rootCA.crt -subj "/C=US/ST=California/L=San Carlos/O=DaedaFusion/CN=root.daedafusion.com"

# Generate RSA key for each host
openssl genrsa -out host.key 2048

# Generate CSR requests for both
echo "---- Host CSR"
openssl req -new -key host.key -out host.csr -subj "/C=US/ST=California/L=San Carlos/O=DaedaFusion/CN=localhost"

# Sign CSRs to generate service certs
openssl x509 -req -in host.csr -CA rootCA.crt -CAkey root.key -CAcreateserial -out host.crt -days 365

# Remove the unneeded csr files
rm host.csr

# Create pkcs12 stores for the service certs, so we can import both public and private keys into the keystore
# You must enter a password for the pkcs12 stores for the subsequent keytool import to work

openssl pkcs12 -export -in host.crt -inkey host.key -password pass:changeit -name servicecert > host.p12

echo "Creating Java Keystores"

# Create keystores
keytool -importkeystore -srckeystore host.p12 -destkeystore host.keystore -srcstoretype pkcs12 -deststoretype JCEKS -srcstorepass changeit -storepass changeit -keypass changeit -alias servicecert
keytool -importcert -noprompt -alias rootca -file rootCA.crt -keystore host.keystore -storepass changeit -storetype JCEKS