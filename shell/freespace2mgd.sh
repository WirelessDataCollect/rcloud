#!/bin/bash

####### get available space ##########
if [ -z $1 ];then
 path="." 
else
 path=$1
fi

# df -h $path | awk 'NR==2{print $4}'
cmd=`df -h $path | awk 'NR==2{print $4}'`

path_avail=$cmd

########### update to mongodb ##################

cmd_left="db.space.update({},{\$set:{'free_space':'"
cmd_right="'}},{upsert:true});"
cmd="$cmd_left$path_avail$cmd_right"
# data : 需要访问的数据库；admin : 管理员保存的数据库
echo $cmd | mongo data -u songchaochao -p songchaochao --authenticationDatabase admin --shell

echo "run $cmd"
