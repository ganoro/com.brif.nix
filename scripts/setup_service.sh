#!/bin/sh
sed "s/%ID%/$1/g" service_template > /etc/init.d/$1
chmod 777 /etc/init.d/$1
service $1 start &
