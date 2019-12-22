# NettySpringWebServer
[Docker repo](https://hub.docker.com/repository/docker/neyzoter/rcloud)

## 预期目标
服务器主要包括四个方面的功能

1.从设备端接收原始数据并进行解析

2.将原数据直接（命令驱动）转发给PC上位机

3.将解析后的数据存储在服务器的MongoDB数据库中

4.上位机连接服务器，通过命令调取测试名称、测试数据等

## 运行流程
### 上位机和服务器的连接
1、上位机连接服务器

上位机加入服务器channel map，并设置为请求连接状态

2、服务器单独为每个通道（即上位机）生成一个盐值（salt）

发送给上位机，数据格式为RandStr:具体的盐值，即```RandStr:j7sjbwwDxzoCYybveyk6```

3、上位机加密管理员账户和密码

用户密码(UTF-8字符串)  \-\-\-MD5\-\-\-\>  密码密文结合salt  \-\-\-MD5\-\-\-\>  最终密文

以Login指令登录，具体见命令表。

4、服务器接收来自上位机的用户名和密码密文

5、服务器从数据库中获取相应用户的密码

*后期考虑是否使用密文存储密码*

具体密文存储方案：每个密码都结合一个固定字符串（服务器和上位机相同）后使用MD5计算出密文。

6、服务器提取数据库中的用户名和密码，根据生成的盐值计算出最终密文，和上位机发送过来的最终密文对比。

7、如果对比成功，则将该channel加入到信任区

如果对比失败，则将该通道删除

### 上位机请求实时数据
1、上位机发送请求命令（前提是上位机已经处于信任区）

2、服务器接收到命令后判断是否信任，是则加入“实时数据请求区”

不信任则忽略

### 上位机退出“实时数据请求区”

1、上位机发送请求命令（前提是上位机已经处于“实时数据请求区”）

2、服务器接收到命令后判断是否“实时数据请求区”，是则退出“实时数据请求区”

不信任则忽略

## 介绍
服务器主要包括三个方面的功能——从设备端接收原始数据并进行解析；将（实时或者历史）原数据转发给PC上位机；将解析后的数据和原数据存储在服务器的MongoDB数据库中。

本服务器基于Netty和Spring框架。

* 数据接收

设备通过UDP，传输数据到服务器。

* 数据转发

PC上位机通过TCP连接服务器8089端口，实施接受经过服务器转发的设备采集数据。

* 数据存储

服务器接受设备数据，并存储在MongoDB数据库中。

单个数据包大小：<1KB；每秒钟14-15个包

存储格式：

**ADC数据**

```
{
	"_id" : ObjectId("5c831c7503047748eb06632b"),
	"nodeId" : 3,
	"yyyy_mm_dd" : NumberLong(0),
	"headtime" : NumberLong(1298),
	"isodate" : "2019-05-05T12:00:00",
	"insertIsodate" : "2019-05-05T11:59:59",
	"io1" : 0,
	"io2" : 0,
	"data_count" : NumberLong(67),
	"dataType" : "ADC"
	"test" : "Default Name/2019-05-05T12:00:00",
	"raw_data" : BinData(0,"AAAAABIFAAAYAgAAAwAAEkRlZmF1bHQgTmFtZQAAAAAAAAAAAAAAAAAAAAAAAAAAGJkAAggDLPMYlQACCAQs8xiUAAEIBizzGJcAAggHLPMYmQACCAos8xiXAAIIDizzGJYAAggQLPMYmQACCBQs9BiXAAIIFyz0GJgAAggZLPMYlQACCBos8xiYAAIIGizzGJcAAggYLPMYlgACCBUs8xiZAAIIEyzzGJYAAggPLPMYlQACCAws8xiYAAIICCzzGJgAAggGLPMYlQABCAQs8xiJAAIIAiz0GJYAAggDLPMYmQACCAUs9BiTAAIICCzzGJkAAggKLPMYmAACCA8s8xibAAIIESzzGJcAAggVLPMYlQACCBcs8xicAAIIGSzzGJcAAggaLPMYkgACCBos8xiZAAIIFyz0GJYAAggWLPMYlgACCBMs8xiYAAIIDyzzGJYAAggMLPMYkgACCAos8xiTAAIIBizzGJEAAggELPQYlwACCAMs8xiVAAIIAyz0GJUAAggFLPMYlQABCAgs8xiTAAIICSzyGJQAAggOLPQYkgACCBEs8xiUAAIIFCzyGJUAAggYLPMYlwABCBks8hiTAAIIGizzGJEAAggaLPMYlQACCBgs9BiRAAIIFSzzGJIAAggTLPMYkwACCA8s8xiYAAIIDSz0GJQAAggJLPMYlAABCAcs8xiTAAIIBCzzGJUAAQgDLPQYlwABCAMs9BiUAAIIBSzzGJYAAggILPMYlQACCAos8xiXAAEIDiz0GJYAAggRLPM="),
	"adc_val" : {
	"ch1" : [
		6297,
		6293,
		...
	],
	"ch2" : [
		2,
		2,
		...
	],
	"ch3" : [
		2051,
		2052,
		...
	],
	"ch4" : [
		11507,
		11507,
		...
	]
}
}

```

**CAN数据**

```
{
	"_id" : ObjectId("5c831c7503047748eb06632b"),
	"nodeId" : 3,
	"yyyy_mm_dd" : NumberLong(0),
	"headtime" : NumberLong(1298),
	"isodate" : "2019-05-05T12:00:00",
	"insertIsodate" : "2019-05-05T11:59:59",
	"io1" : 0,
	"io2" : 0,
	"data_count" : NumberLong(67),
	"dataType" : "CAN"
	"test" : "Default Name/2019-05-05T12:00:00",
	"raw_data" : BinData(0,"AAAAABIFAAAYAgAAAwAAEkRlZmF1bHQgTmFtZQAAAAAAAAAAAAAAAAAAAAAAAAAAGJkAAggDLPMYlQACCAQs8xiUAAEIBizzGJcAAggHLPMYmQACCAos8xiXAAIIDizzGJYAAggQLPMYmQACCBQs9BiXAAIIFyz0GJgAAggZLPMYlQACCBos8xiYAAIIGizzGJcAAggYLPMYlgACCBUs8xiZAAIIEyzzGJYAAggPLPMYlQACCAws8xiYAAIICCzzGJgAAggGLPMYlQABCAQs8xiJAAIIAiz0GJYAAggDLPMYmQACCAUs9BiTAAIICCzzGJkAAggKLPMYmAACCA8s8xibAAIIESzzGJcAAggVLPMYlQACCBcs8xicAAIIGSzzGJcAAggaLPMYkgACCBos8xiZAAIIFyz0GJYAAggWLPMYlgACCBMs8xiYAAIIDyzzGJYAAggMLPMYkgACCAos8xiTAAIIBizzGJEAAggELPQYlwACCAMs8xiVAAIIAyz0GJUAAggFLPMYlQABCAgs8xiTAAIICSzyGJQAAggOLPQYkgACCBEs8xiUAAIIFCzyGJUAAggYLPMYlwABCBks8hiTAAIIGizzGJEAAggaLPMYlQACCBgs9BiRAAIIFSzzGJIAAggTLPMYkwACCA8s8xiYAAIIDSz0GJQAAggJLPMYlAABCAcs8xiTAAIIBCzzGJUAAQgDLPQYlwABCAMs9BiUAAIIBSzzGJYAAggILPMYlQACCAos8xiXAAEIDiz0GJYAAggRLPM="),
}

```

**Config数据**

```
{
	"_id" : ObjectId("5c831c7503047748eb06632b"),
	"test" : "test1_2019-02-02T13:50:23",
	"isodate" : "2019-02-02T13:50:23"
	"insertIsodate" : "2019-05-05T11:59:59",
	"dataCol" : "2019-05",
	"config" : XXX(String)
}
```

# 上位机和服务器的交互
### 信息中不可包含的字符
"+"：用于分割命令和信息，CMD+INFO

";"：用于分割INFO，将INFO(info1;info2;...)分割为多个子info

":"：用于分割子info的key:value（或者大小关系）

","：用于分割子info中的value(多用于分割上下界和多个数据并列or，如year:2019,2033，表示年份从2019到2033;如dataType:ADC,CAN，表示数据类型包括ADC或者CAN)

"\\n"：一个包的结尾，用于解析TCP包粘包问题。

```
MongoFindDocs+test:test1_20190121;headtime:8245840,8245840
```

**备注：**

在java中需要转义的几个字符：

```
( [ { \ ^ - $ ** } ] ) ? * + .
```

`\SPL`：表示接收两个报文间的分隔符，本次使用`\t`

`\SDSPL`：发送的数据每个都要加`\n`

### 数据库查询
|上位机命令|信息|服务器返回|结束|说明|
|-|-|-|-|-|
|MongoFindDocsNames|key1:value1;key2:value2;...`\SPL`|MongoFindDocsNames:xxx`\SDSPL`|MongoFindDocsNames:OVER`\SDSPL`|查询所有的doc名称|
|MongoFindDocs|key1:value1;key2:value2;...`\SPL`|MongoFindDocs:xxx`\SDSPL`|MongoFindDocs:OVER`\SDSPL`|根据条件查询doc，并发送给上位机|
|GetTestConfig|测试名称`\SPL`|GetTestConfig:配置文件`\SDSPL`|GetTestConfig:OVER`\SDSPL`|查找配置文件|


```
eg.查询所有的doc名称
MongoFindDocsNames\SPL

eg.查询"isodate在日期2019-02-02T13:50:23到2019-02-02T14:50:23"的数据实验名称（test）
MongoFindDocsNames+isodate:2019-02-02T13:50:23,2019-02-02T14:50:23\SPL

eg.查询"isodate在日期2019-02-02到2019-02-03"的数据实验名称（test）,不包括2019-02-03
MongoFindDocsNames+isodate:2019-02-02,2019-02-03\SPL

eg.查询"insertIsodate在日期2019-02-02T13:50:23到2019-02-02T14:50:23"的数据实验名称（test）
MongoFindDocsNames+insertIsodate:2019-02-02T13:50:23,2019-02-02T14:50:23\SPL

eg.查询"insertIsodate在日期2019-02-02到2019-02-03"的数据实验名称（test）,不包括2019-02-03
MongoFindDocsNames+insertIsodate:2019-02-02,2019-02-03\SPL

```

```
eg.获取所有的doc
MongoFindDocs

eg.获取"测试名称：test1_2019-02-02T13:50:23"
MongoFindDocs+test:test1_2019-02-02T13:50:23

```

### 指令

|上位机命令|信息|服务器返回|说明|
|-|-|-|-|
|Login|登录用户名;MD5加密数据`\SPL`|Login:OK`\SDSPL`|登录用户|
|StartTest|测试名称;配置文件长度;配置文件`\SPL`|StartTest:OK`\SDSPL`或者StartTest:ERROR`\SDSPL`|开始测试，保存配置文件|
|GetRtdata|测试名称`\SPL`或者all`\SPL`|GetRtdata:OK`\SDSPL`|获取实时数据,改状态下不能进行其他操作，需要先关闭GetRtdata才能进行其他操作|
|StopGetRtdata|none`\SPL`|StopGetRtdata:OK`\SDSPL`|停止获取实时数据|
|HeartBeat|none`\SPL`|HeartBeat:GET`\SDSPL`|心跳包|
|Disconnect|none`\SPL`|Disconnect:OK`\SDSPL`|断开连接|
|StopMongoFindDocs|`\SPL`|StopMongoFindDocs:OK`\SDSPL`|停止接收数据|

**说明**

* 关于登录(Login)

`Login+登录用户名;MD5加密数据` 的MD5加密数据是经过两次MD5加密的。

用户密码(UTF-8字符串)  \-\-\-MD5\-\-\-\>  密码密文结合salt  \-\-\-MD5\-\-\-\>  最终密文

* 关于查找配置文件(GetTestConfig)

`GetTestConfig+测试名称\SPL` 用于查找测试名称对应的配置文件

eg.

```
PC端发送：
GetTestConfig+test1_2019_1_2\SPL
服务器端发送：
GetTestConfig+配置文件（String格式）\SPL
```


# 数据库操作

```bash
# 删除所有数据
db.COL.dorp()
# 查询所有数据
db.COL.find().pretty()

```

# 参考

[Netty实战精髓-w3cSchool](https://www.w3cschool.cn/essential_netty_in_action/ "Netty实战精髓-w3cSchool")

[Netty实战-何平译](https://book.douban.com/subject/27038538/ "Netty实战-何平译")

[对Netty组件的理解](http://neyzoter.cn/2018/09/07/Netty-EventLoopGroup-EventLoop-Channel-Channle-ChannlePipeline-et/ "对Netty组件的理解（Channel、Pipeline、EventLoop等）")

[Netty笔记](http://neyzoter.cn/wiki/Netty/ "Netty笔记")

[Java菜鸟教程](http://www.runoob.com/java/java-tutorial.html "Java菜鸟教程")

[Java笔记](http://neyzoter.cn/wiki/Java/ "Java笔记")

[Maven笔记](http://neyzoter.cn/wiki/MAVEN/ "Maven笔记")

[MongoDB菜鸟教程](http://www.runoob.com/mongodb/mongodb-tutorial.html "MongoDB菜鸟教程")

[MongoDB笔记](http://neyzoter.cn/wiki/MongoDB/ "MongoDB笔记")

[Spring笔记](http://neyzoter.cn/wiki/Spring/ "Spring笔记")


