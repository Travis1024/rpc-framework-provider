# rpc-framework-provider

### 模块说明

| 模块             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| Common-API       | 服务消费者和服务提供者的公共API**（消费者和提供者共同依赖）** |
| RPC-core         | RPC核心模块，定义序列化、异常、编码、pojo、枚举类**（消费者和提供者共同依赖）** |
| RPC-server-start | RPC服务端starter，负责发布 RPC 服务，接收和处理 RPC 请求，反射调用服务端 |
| Provider         | 服务提供者启动类，controller                                 |

<img src="https://travisnotes.oss-cn-shanghai.aliyuncs.com/mdpic/202303251504048.png" alt="rpc_code_framework" style="zoom:60%;" />

