#!/bin/bash

if [ -z $1 ];then
 path="." 
else
 path=$1
fi

cmd=`df -h $path | awk 'NR==2{print $4}'`

echo $cmd > ./space
