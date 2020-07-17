# redis_generator_id 分布式ID生成器
generator id by redis lua

基于redis+lua的分布式ID生成器。

要了解redis的EVAL，EVALSHA命令：

http://redis.io/commands/eval

http://redis.io/commands/evalsha

利用redis的lua脚本执行功能，在每个节点上通过lua脚本生成唯一ID。

Redis是单线程的，所以也可以用生成全局唯一的ID。可以用Redis的原子操作 INCR和INCRBY来实现。

可以使用Redis集群来获取更高的吞吐量

本例中生成的ID是17位的：

如：2020年1月15日生成的ID：200150000000001，前两位年份，中间三位是该年第几天,后面十二位是自增序列。

每天自增序列零点置零，同时天数序列+1

    year     day			seq
    
    20 	    015            000000000001
    
    前两位年份  该年第几天   自增序列

集群实现原理


假定集群里有3个节点

则节点1返回的seq是：
0, 3, 6, 9, 12 ...

节点2返回的seq是
1, 4, 7, 10, 13 ...

节点3返回的seq是
2, 5, 8, 11, 14 ...

这样每个节点返回的数据都是唯一的。

redis 2.8以上即可支持

每日可满足1000亿级内ID生产不重复，支持传入表名称生成获取该表趋势增长的ID

本例可以有效避免时钟回调导致依赖于时间戳的ID生产冲突，避免redis单点故

使用3台阿里云redis集群测试 QPS约5.5W

可以线性扩展，3个结点足以满足绝大部分的应用.
