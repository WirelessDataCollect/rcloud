#!/bin/sh
# 查询所有用户

cmd="db.admin.find({},{'userName':1,'userKey':1,'_id':0}).pretty();"
echo $cmd | mongo data --shell
