#!/bin/sh
cd /root/brif-nix
nohup ant run -Darg0=$1 -Darg1=setup:true -Darg2=$2 -Darg3=$3  >/var/log/$1.boostrapper.log 2>&1 &
