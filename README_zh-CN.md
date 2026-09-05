# 局域网服务器属性增强 (LAN Server Properties)

**[English README](README.md)**

本项目复刻自[LanServerProperties](https://github.com/rikka0w0/LanServerProperties)
所有内容均为AI生成

本模组为 Minecraft 原版"对局域网开放"（LAN）界面（即新版中的"多人游戏选项"界面）增加更多服务器设置项。

下载：[CurseForge](https://www.curseforge.com/minecraft/mc-mods/lan-server-properties/) · [Modrinth](https://modrinth.com/mod/P7dR8mSH) · [GitHub Releases](https://github.com/rikka0w0/LanServerProperties/releases)

## 功能特性

安装本模组后，原版局域网界面可以额外做到：

* 自定义**端口**（不再使用随机端口）。
* **关闭 PvP**。
* **关闭正版验证（离线模式）**，让未购买 Minecraft 的玩家也能加入局域网服务器。
* 服务器**已发布后**，仍可从同一界面修改其设置。
* 修改服务器的**最大玩家数**。
* 可选在离线模式下使用**正版 UUID**（来自微软 / Mojang），避免世界在正版与离线模式之间切换时丢失背包物品。

### Minecraft 26.x 中的界面位置

自 Minecraft 26.x 起，原版局域网设置被整合进暂停菜单 → **多人游戏选项（Multiplayer Options…）** 界面（不再有独立的"对局域网开放"界面）。在 26.2 中，本模组把自身设置行追加在原版"其他玩家"设置（游戏模式 / 允许作弊）下方、同一内容列之内，并在其上方增加"保存/启用偏好"行：

* 保存偏好 / 启用偏好 行
* 加载偏好 / PvP 行
* 在线模式 / 最大玩家数 行
* 常离线玩家列表 行

## 支持的版本

| 加载器 | Minecraft 版本 | 说明 |
| --- | --- | --- |
| Forge | 1.20.6、1.21、1.21.1、1.21.3 | 由 `forge/` 子项目构建 |
| NeoForge | 1.20.5 – 1.21.3 | 由 `neoforge/` 子项目构建 |
| Fabric | **26.2** | 本分支的 `fabric/` 子项目构建（需要 Java 25） |

* Minecraft 1.20.5 与 1.21.2 没有 Forge 版本（这两个版本 Forge 本身不支持）。
* 本项目更早的版本支持 Minecraft 1.12.2 – 1.21.x（Fabric / Forge / NeoForge）。本分支中 `fabric/` 子项目已迁移至 26.x 工具链、仅面向 Minecraft 26.2，而 `forge/` 与 `neoforge/` 子项目仍面向 1.21.x 时代。

## 注意事项

* 本模组为**客户端模组**，装在专用服务器上不会有任何效果。
* 只需要**开设局域网的一方**安装本模组。
* 即使你安装了本模组，原版客户端也能加入你的服务器（不过你运行的其他模组仍可能阻止原版客户端加入）。
* 安装本模组不会影响你加入其他原版或模组服务器。

## 依赖

### Forge 与 NeoForge 版本
先安装与 Minecraft 版本匹配的 Forge / NeoForge，再安装本模组。

### Fabric 版本（26.2）
* Minecraft 26.2 需要 **Java 25**。
* 需要 **Fabric Loader 0.19.3 及以上**。
* **Fabric API** 可选，但强烈建议安装。

## 开发者

如需修改与调试代码，请把 `neoforge`、`forge` 或 `fabric` 文件夹作为 Gradle 项目导入（例如 Eclipse IDE），然后运行 `genEclipseRuns` 任务。

Windows 用户需要把命令中的 `./` 和 `../` 分别替换为 `.\` 和 `..\`。

### 代码结构

* 自 1.17.1 起，Fabric 与 Forge 版本尽可能共享公共代码（基于 Minecraft 官方映射）。
* 自 1.21 起加入 NeoForge 支持，并彻底移除 coremod 与访问转换器（AT），全面改用 **Mixin**。
* 自 Minecraft 26.1 起游戏**不再混淆**，Fabric 工具链随之变化：`fabric/` 子项目改用新的 `net.fabricmc.fabric-loom` 插件，**无需 mappings**，依赖改为普通 `implementation`，并使用 Gradle 9.5 与 Java 25；局域网设置整合进原版 `MultiplayerOptionsScreen`。
* 面向 26.2 的适配代码独立存放在 `fabric/src/main/java`（例如 `CommonWidgets`、`ConfigContainer`，以及对 `MultiplayerOptionsScreen` / `IntegratedServer` / `UUIDUtil` 的 Mixin），共享目录 `src/main/java` 仍是 1.21 时代代码，供 `forge/` 与 `neoforge/` 构建使用。

### 编译 Fabric 产物（26.2）
```
git clone https://github.com/rikka0w0/LanServerProperties.git
cd LanServerProperties/fabric
./gradlew build
```
产物位于 `build/libs/lanserverproperties-<版本号>-fabric.jar`。

在 Fabric 下调试时，可能需要创建 `run/config/fabric_loader_dependencies.json`，内容如下：
```json
{
  "version": 1,
  "overrides": {
    "lanserverproperties": {
      "-depends": {
        "minecraft": "IGNORED",
        "fabricloader": "IGNORED"
      }
    }
  }
}
```

### 编译 Forge 产物
```
git clone https://github.com/rikka0w0/LanServerProperties.git
cd LanServerProperties/forge
./gradlew build
```

### 编译 NeoForge 产物
```
git clone https://github.com/rikka0w0/LanServerProperties.git
cd LanServerProperties/neoforge
./gradlew build
```

### 指定 JRE 路径

不同 Minecraft 版本对 Java 的要求：

* 自 26.1 起，Minecraft（及本分支的 Fabric 构建）需要 **Java 25**。
* 自 1.20.5 起，Minecraft 需要 Java 21。
* 自 1.18 起，Minecraft 需要 Java 17。
* 自 1.17 起，Minecraft 需要 Java 16。
* 更早的版本需要 Java 8。

Linux：
```
./gradlew -Dorg.gradle.java.home=/path_to_jdk_directory <commands>
```

Windows：
```
.\gradlew.bat -Dorg.gradle.java.home="C:/Program Files/Java/jdk-25" runClient
```
