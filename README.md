# Spring-boot-protocol
多协议服务器, Springboot协议扩展包, 允许单端口提供多协议服务.其中内置多种网络传输(标准与规范)的实现库, 轻松添加或扩展协议. 例: HttpServlet, RPC, MQTT（物联网通讯协议）, RTSP(流媒体协议), DNS（域名解析协议）.

    1.可以替代tomcat或jetty. 导包后一个@EnableNettyServletEmbedded注解即用. 
    
    2.HttpServlet性能比tomcat的NIO高出 20%(TPS)
    
    3.RPC性能略胜阿里巴巴的Dubbo, 使用习惯保持与springcloud相同, 可以不改springcloud代码替换Feign调用
    
    4.MQTT等物联网协议可以在不依赖协议网关, 单机同时支持N种协议 (例: HTTP,MQTT,RTSP,DNS. 底层原理是,接到数据包后,进行协议路由.)
    
    5.可以添加自定义传输协议. (例: 定长传输, 分隔符传输)
    
    6.高并发下服务器内存依然保持平稳

作者邮箱 : 842156727@qq.com

github地址 : https://github.com/wangzihaogithub


### 使用方法

#### 1.添加依赖, 在pom.xml中加入 （注: 1.x.x版本是用于springboot1.0，2.x.x版本用于springboot2.0）

    <dependency>
      <groupId>com.github.wangzihaogithub</groupId>
      <artifactId>spring-boot-protocol</artifactId>
      <version>2.0.0</version>
    </dependency>
	
	
#### 2.开启netty容器

    @EnableNettyServletEmbedded//切换容器的注解
    @SpringBootApplication
    public class ExampleApplication {
    
        public static void main(String[] args) {
            SpringApplication.run(ExampleApplication.class, args);
        }
    }

#### 3.启动, 已经成功替换tomcat, 切换至 NettyTcpServer!
	2019-02-28 22:06:16.192  INFO 9096 --- [er-Boss-NIO-2-1] c.g.n.springboot.server.NettyTcpServer   : NettyTcpServer@1 start (port = 10004, pid = 9096, protocol = [my-protocol, http, nrpc, mqtt], os = windows 8.1) ...
	2019-02-28 22:06:16.193  INFO 9096 --- [           main] c.g.example.ProtocolApplication10004     : Started ProtocolApplication10004 in 2.508 seconds (JVM running for 3.247)    
---

#### 更多功能例子example-> [请点击这里查看示例代码](https://github.com/wangzihaogithub/netty-example "https://github.com/wangzihaogithub/netty-example")

##### 示例1. 编写并添加一个自定义传输协议

##### 示例2. springcloud中替换Feign的调用方式

##### 示例3. 对springboot-websocket的支持

##### 示例4. 协议网关, 监控各个协议的流量





