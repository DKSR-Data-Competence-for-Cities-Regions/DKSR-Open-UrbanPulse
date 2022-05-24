#!/bin/sh

echo "Checking if GLASSFISH_RESOURCE_PATH is set....."
java -version
if [ -z "${GLASSFISH_RESOURCE_PATH}" ]; then
  echo "GLASSFISH_RESOURCE_PATH is empty, skiping Payara configuration..."
else
   #echo "GLASSFISH_RESOURCE_PATH is set starting Payara configurations...."
   ${PAYARA_DIR}/bin/asadmin start-domain ${DOMAIN_NAME}
   ${PAYARA_DIR}/bin/asadmin --user ${ADMIN_USER} --passwordfile=${PASSWORD_FILE} add-resources ${GLASSFISH_RESOURCE_PATH}
   #${PAYARA_DIR}/bin/asadmin --user ${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-jvm-options -Dfish.payara.classloading.delegate=false
   echo "create sql folder for payara...."
   mkdir ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/sql

   echo "moving sql scripts....."
   cp /etc/config/persistence.sql ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/sql/
   cp /etc/config/inbound.sql ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/sql/

   echo "Restarting Payara....."
   ${PAYARA_DIR}/bin/asadmin stop-domain ${DOMAIN_NAME}

   ${SCRIPT_DIR}/entrypoint.sh
fi
