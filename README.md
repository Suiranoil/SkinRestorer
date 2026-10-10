# SkinRestorer — GTNH 1.7.10

GT New Horizons 2.9.0-RC-2 / Minecraft 1.7.10 的 Forge 回移版本，用于在离线服务器中按 Mojang 账号名恢复和分发玩家皮肤。

## 支持环境

- GT New Horizons 2.9.0-RC-2
- Minecraft 1.7.10
- Forge 10.13.4.1614
- Java 25
- UniMixins 0.3.1
- SimpleSkinBackport 1.0.3-GTNH

服务端使用 Java 25 运行。服务器端安装本模组即可向其他玩家发送换肤后的玩家档案；需要本人第一人称和 F5 视角立即更新的客户端，可以额外安装本模组的客户端桥接。

## 功能

- 登录时按玩家当前登录名从 Mojang 获取皮肤
- 使用 Mojang 签名纹理，保持玩家 UUID、名称和存档身份不变
- `/skin set mojang <name>` 设置皮肤来源
- `/skin reset` 或 `/skin clear` 移除手动覆盖并恢复玩家自己的 Mojang 皮肤
- `/skin refresh` 手动刷新来源皮肤
- 在线玩家和已知 UUID 的离线玩家管理
- 世界内持久化，跨维度共享，服务器重启后恢复
- classic、slim、64×32 和 64×64 皮肤
- 观察者在线刷新，保留装备、效果、骑乘和玩家状态
- Mojang 服务、磁盘、网络同步或客户端桥接失败时继续正常游戏，并保留当前有效皮肤
- 请求合并、超时、限流处理和每玩家操作冷却

皮肤格式和 slim 模型由 GTNH 的 SimpleSkinBackport 负责，本模组不重复实现渲染器。

## 安装

1. 确认服务器使用 GTNH 2.9.0-RC-2 的 Java 25 发行包。
2. 将 `skinrestorer-1.0.0-gtnh.jar` 放入服务器 `mods` 目录。
3. 服务器已经包含 UniMixins 和 SimpleSkinBackport。
4. 需要本人实时换肤的玩家，将同一个 JAR 放入客户端 `mods` 目录。
5. 启动服务器并让玩家正常登录。

客户端桥接是可选的。没有桥接的客户端仍然可以加入服务器，并能看到其他玩家由服务器分发的皮肤；本人的第一人称显示可能保持原有皮肤。

## 命令

```text
/skin set mojang <Mojang用户名>
/skin set mojang <Mojang用户名> <在线玩家或已知UUID>
/skin reset [在线玩家或已知UUID]
/skin clear [在线玩家或已知UUID]
/skin refresh [在线玩家或已知UUID]
/skin config reload
```

普通玩家只能操作自己，默认冷却时间为 30 秒，可在配置中调整。OP 或控制台可以操作在线玩家和本世界已经确认过 UUID 的离线玩家。

离线服务器中的玩家身份仍然使用离线 UUID。自动恢复通过玩家登录名查询对应的 Mojang 账号；如果登录名不是 Mojang 账号，可以由管理员使用 `set mojang` 指定来源。

## 配置

首次启动后会生成 `config/skinrestorer.cfg`。默认值包括：

- 玩家命令冷却：30 秒
- Mojang 请求超时：10 秒
- 请求线程：4
- 请求队列：64
- 成功缓存：300 秒

Mojang 服务不可用时，玩家仍然可以登录和继续游戏。设置或刷新失败会保留当前皮肤，并在聊天和服务器日志中给出原因。

## 当前范围

此分支面向 GTNH 1.7.10 回移，首版来源为 Mojang。以下功能不属于当前版本：

- URL、Ely.by、MineSkin、本地图片和自定义提供器
- SkinShuffle 和图形化换肤界面
- 单人、局域网和混合插件服务端
- SkinPort、CustomSkinLoader 等额外皮肤加载链
- 跨世界全局皮肤存储

正版 `online-mode=true` 的完整登录矩阵需要使用真实正版账号单独验证；离线服务器、Mojang 签名纹理、双客户端观察、classic/slim 和重启恢复已经按 GTNH 2.9.0-RC-2 进行了验证。

## 构建

需要 Java 25、Java 8/17 辅助工具链和网络可用的 GTNH Maven 仓库：

```powershell
$env:JAVA_HOME = "E:\dsh\workspace\minecreaft\runtime\jdk25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

.\gradlew.bat clean build --no-daemon
```

最终重混淆产物位于 `build/libs/skinrestorer-1.0.0-gtnh.jar`。

## 许可

本项目保留 SkinRestorer 原作者 Lionarius 的 MIT 许可证和版权声明。
