# 网络根因分析教程（通话故障）

本教程展示一个更复杂的 SPG Reasoner 示例，用于通信网络的根因分析。它从用户的通话问题出发，检查基站成功率，沿回传链路定位最近的信号异常节点，并关联到事件；同时也考虑 SIM 欠费与测速异常等替代原因。

## 为什么选择这个案例（白皮书关联）

SPG 白皮书提到多实体关联的风险推理场景（例如“AI 电话诈骗提醒”），强调将实体网络与事件网络连接起来进行推断。本教程借用这个思路，将“通话问题”与“网络事件”关联起来，用规则解释问题来源。

## 场景故事

客服收到多起通话失败投诉。每个问题需要做到：

1) 检查服务基站成功率是否偏低。  
2) 沿回传链路找到最近的信号异常节点。  
3) 关联最合理的事件作为根因。  
4) 如果 SIM 欠费，优先判定为欠费事件。  
5) 如果基站健康但测速很差，则归因于本地低速事件。

## 使用文件

- DSL：`reasoner-examples/src/main/resources/kgdsl/network_root_cause.kgdsl`
- 运行器：`reasoner-examples/src/main/java/com/antgroup/openspg/examples/NetworkRootCauseLocalRunnerExample.java`
- GraphML 数据：`reasoner-examples/src/main/resources/graphml/network_root_cause.graphml`

## 如何运行

在仓库根目录执行：

```bash
mvn -pl reasoner-examples -am exec:java \
  -Dexec.mainClass=com.antgroup.openspg.examples.NetworkRootCauseLocalRunnerExample
```

## 图模型（简化版）

节点：

- `User`, `CallIssue`, `SimCard`, `BaseStation`, `NetworkNode`, `SpeedTest`, `Event`

边：

- `reportedBy`（CallIssue -> User）
- `at`（CallIssue -> BaseStation）
- `usesSim`（User -> SimCard）
- `speedTest`（User -> SpeedTest）
- `connectedTo`（BaseStation -> NetworkNode）
- `hasEvent`（NetworkNode/SimCard/SpeedTest -> Event）
- `rootCause`（CallIssue -> Event）——由规则推断生成

## Java 示例如何串起来

`NetworkRootCauseLocalRunnerExample` 主要做四件事：

1) **加载 KGDSL**  
从资源文件读取 `network_root_cause.kgdsl`，便于只改 DSL 不改 Java。

2) **推断并注册 Schema**  
调用 `GraphMLLocalGraphLoader.buildCatalog(..., dsl)` 从 GraphML 自动推断节点/边属性，
同时补齐 `Define` 中声明的边类型并注册 `PropertyGraphCatalog`，让 Reasoner 校验
`bs.successRate`、`ev.eventType` 等字段，不再需要手写 schema 映射。

3) **设置起始节点**  
通过 `startIdList` 指定要分析的 `CallIssue`，这些就是输入问题的起点。

4) **加载 GraphML 数据**  
`NetworkRootCauseGraphMLLoader` 读取 `network_root_cause.graphml` 并构建内存图，
便于直接编辑数据集且无需外部数据源。

注意点：

- `task.setDsl(...)` 绑定 DSL。
- `task.setStartIdList(...)` 控制分析哪些问题。
- 本示例 **不启用** `spg.reasoner.lube.subquery.enable`，保证三个 `Define` 顺序执行并写入 `rootCause`。
- `params.put(ConfigKey.KG_REASONER_OUTPUT_GRAPH, "true")` 会输出推断出的边。

## 根因规则（DSL 做了什么）

DSL 中包含三个 `Define` 块，分别生成 `rootCause`：

1) **SIM 欠费**  
如果 `sim.status == 'ARREARS'`，并且事件类型为 `SIM_ARREARS`，则建立根因关系。

2) **回传链路故障**  
如果基站成功率低、回传节点信号损失高、事件为 `BACKHAUL_OUTAGE` 且严重度高，则建立根因关系。  
规则使用 `keep_shortest_path` 保留最短链路。

3) **本地低速**  
如果 SIM 正常、基站健康，但测速很差，则关联 `LOW_SPEED` 事件作为根因。

最后的 `get(...)` 查询会返回：通话问题、SIM 状态、基站指标、测速结果、网络节点指标、事件类型，以及 `__path__` 路径。

## `network_root_cause.kgdsl` 做了什么（分解版）

这个文件主要完成两件事：

1) **用规则写入 `rootCause` 边**（三个 `Define`）。  
2) **查询并输出结果**（最终 `get(...)`）。

细分如下：

- **Define 1（SIM 欠费）**  
  匹配 `CallIssue -> User -> SimCard -> Event`，满足欠费条件后写入根因。

- **Define 2（回传故障）**  
  匹配 `CallIssue -> BaseStation -> (connectedTo)^1..3 -> NetworkNode -> Event`，  
  用 KPI 规则筛选，保留最短路径。

- **Define 3（低速）**  
  匹配 `CallIssue -> User -> SpeedTest -> Event`，结合基站健康判断低速根因。

- **最终查询**  
  从 `__start__` 的 `CallIssue` 出发，联结 `rootCause` 和上下文指标，输出结果和路径。

## 如何理解输出

每一行（或每条边）代表一个 `CallIssue` 及其推断出的根因：

- `ci.issueType`：用户描述的问题类型
- `bs.successRate` / `n.signalLossRate`：网络侧健康状况
- `sim.status`：是否欠费
- `st.downMbps`：本地测速指标
- `ev.eventType`：推断的根因事件
- `__path__`：回传链路路径

### 图模式输出示例（edge_list）

当 `kg.reasoner.output.graph=true` 时，结果以边的形式打印，例如：

```
edge_list_start
  (0) Edge(s=CallIssue_-2010113363008216754,p=CallIssue_rootCause_Event,o=Event_6782456717300958980,direction=OUT,version=0,property={
    "__to_id__":"EV-3",
    "__to_id_type__":"Event",
    "__from_id__":"CI-3",
    "__from_id_type__":"CallIssue",
    "process":{
      "hitRule":[
        {"ruleName":"R2","ruleValue":"bs.successRate >= 0.98","hitValue":"bs.successRate=0.98"},
        {"ruleName":"R3","ruleValue":"st.downMbps < 1.0","hitValue":"st.downMbps=0.4"},
        {"ruleName":"R4","ruleValue":"ev.eventType == \"LOW_SPEED\"","hitValue":"ev.eventType=LOW_SPEED"},
        {"ruleName":"R1","ruleValue":"sim.status != \"ARREARS\"","hitValue":"sim.status=OK"}
      ],
      "failedRule":[],
      "targetId":"EV-3",
      "targetLabel":"Event",
      "startNode":{"bizId":"CI-3","label":"CallIssue"}
    }
  })
edge_list_end
```

如何理解：

- **CI-3 -> EV-3**：通话问题 CI-3 被判定由事件 EV-3 引起。
- **process.hitRule**：命中的规则来自“低速”分支（SIM 正常、基站健康、测速过低）。
- **failedRule** 为空：该分支没有规则失败。

## 试试这些变化

1) 调整成功率阈值（例如 `0.9` 改成 `0.95`）。  
2) 加一条新的 `CallIssue`，不欠费也没有测速，看回传规则如何表现。  
3) 给回传链路增加一跳，观察 `keep_shortest_path` 的效果。
