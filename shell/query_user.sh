#!/bin/sh
# 查询所有用户

cmd="db.admin.find({},{'userName':1,'userKey':1,'_id':0}).pretty();"
# data : 需要访问的数据库；admin : 管理员保存的数据库
echo $cmd | mongo data -u songchaochao -p songchaochao --authenticationDatabase admin --shell
