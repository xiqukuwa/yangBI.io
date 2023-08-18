# yangBI 项目介绍

> 作者：[@autho yangyang](https://github.com/xiqukuwa/yangBI.io)

基于Spring Boot + MQ+ AlGC 的智能数据分析平台。区别于传统Bl，用户只需要导入原始数据集、并输入分析诉求，就能自动生成可视化图表及分析结论，实现数据分析的降本增效

[toc]

## 项目特点

### 技术栈

- Spring Boot 
- RabbiMQ
- JDK线程池
- AIGC
- Redis



### 工具类

- Easy Excel 表格处理
- Hutool 工具库
- Gson 解析库
- Apache Commons Lang3 工具类
- Lombok 注解



## 业务功能

- 用户登录、注册、注销、更新、
- 后端自定义 Prompt预设模板并封装用户输入的数据和分析诉求，通过对接AlGC接口生成可视化图表JSON配置和分析结论，返回给前端渲染
- 由于AlIGC的输入Token限制，使用Easy Excel解析用户上传的XLSX表格数据文件并压缩为CSV，实测提高了20%的单次输入数据量、并节约了成本。
- 为防止某用户恶意占用系统资源，基于Redisson的 RateLimiter实现分布式限流，控制单用户访问的频率
- 由于AlGC的响应时间较长，基于自定义IO密集型线程池＋任务队列实现了AlGC的并发执行和异步化，提交任务后即可响应前端，提高用户体验。
- 由于本地任务队列重启丢失数据，使用RabbitMQ(分布式消息队列)来接受并持久化任务消息，通过 Direct交换机转发给解耦的Al生成模块消费并处理任务，提高了系统的可靠性
- 创建死信队处理异常情况，将图表的生成状态修改为失败
- 利用Redis缓存图表数据，提高了加载数据效率
- 添加补偿机制，使用定时任务将失败状态的图表重新放入队列中，提高用户体验



### 架构设计

- 合理分层


## 项目展示 

[项目上线地址](https://github.com/xiqukuwa/yangBI.io)





1）前端登录
![](doc/img1.png)

2）输入自己的需求与需要分析的文件，等待生成结果
![](doc/img2.png)

![](doc/img3.png)
![](doc/img4.png)
3）输入自己的需求与需要分析的文件，异步生成结果，无需等待
![](doc/img5.png)
![](doc/img6.png)

