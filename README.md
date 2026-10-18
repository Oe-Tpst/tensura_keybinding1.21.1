# Tensura: Key binding

[English](#english) | [中文](#中文)

---

## English

An add-on for **Tensura: Reincarnated** that provides three dedicated skill shortcut slots, independent of the native skill bar. Each slot has its own keybind, so you can cast frequently used skills without switching presets.

- **Mod id**: `tensura_keybinding`
- **Display name**: `Tensura: Key binding`
- **Version**: `1.0.0`
- **Environment**: Minecraft 1.21.1 / NeoForge 21.1.251 / JDK 21
- **License**: [MIT](LICENSE)

### Features

1. **Three skill slots**, fully independent of Tensura's native presets.
2. **Dedicated keys**: `J` / `K` / `L` trigger slots 1 / 2 / 3 by default, rebindable in *Options → Controls → Key Binds*.
3. **B-menu entry**: a new tab button on the left side of Tensura's main screen (opened with `B`) leads to the slot configuration screen.
4. **Slot configuration screen**: browse your learned skills on the left (resistance skills are filtered out), click a skill then click a slot to bind it; slots can be cleared.
5. **Persistent HUD**: three slot icons are drawn above Tensura's native ability frames, following the native HUD's position and scale.
6. **Native activation**: key presses ultimately go through Tensura's own `startHoldSkill` / `handleSkillRelease`, so cooldowns, mastery, and costs behave exactly like the native skill bar.

### Usage

1. Press `B` to open Tensura's main screen and click the new tab on the left to open **Key Slots**.
2. Select a learned skill from the list on the left (it highlights in green).
3. Click any slot on the right to bind it. Repeat for the other slots.
4. Back in game, press `J` / `K` / `L` to cast the bound skills.
5. If a skill has multiple modes, hold Tensura's *next/previous ability mode* key (Left Alt by default) and press a slot key to cycle modes. The current mode is shown on the action bar.

Slot data is stored client-side in `config/tensura_keybinding-client.json`.

### Installation

1. Install Minecraft 1.21.1 and NeoForge 21.1.251 or newer.
2. Place the following mods in your `mods/` folder:
   - [Tensura: Reincarnated](https://modrinth.com/mod/tensura-reincarnated) 2.0.1.3 or newer (NeoForge build)
   - [ManasCore](https://modrinth.com/mod/manascore) 4.0.0.2
   - [Architectury API](https://modrinth.com/mod/architectury-api) 13.0.11
   - [GeckoLib](https://modrinth.com/mod/geckolib) 4.9.3
   - [SmartBrainLib](https://modrinth.com/mod/smartbrainlib) 1.16.11
   - [TerraBlender](https://modrinth.com/mod/terrablender) 4.1.0.8
   - This mod: `tensura_keybinding-1.0.0.jar`
3. Launch the game.

> This mod must be installed on both the client and the server. A client-only install cannot trigger skills.

### Building from source

```bash
cd C:/OE/MC/mod/explore-template-1.21.1

# Compile
./gradlew processResources compileJava --offline

# Launch the client
./gradlew runClient --offline
```

> **`--offline` is required** in the current network environment: `maven.neoforged.net` times out, but all dependencies are already cached. Remove `--offline` once when adding or upgrading dependencies.

#### Environment notes

- **JDK 21**: `gradle.properties` pins `org.gradle.java.home=C:/Program Files/Java/jdk-21`. On Windows accounts whose username contains non-ASCII characters, other JDKs can break the Gradle worker (`ClassNotFoundException: GradleWorkerMain`).
- **Tensura dependency**: the build references the Modrinth **version ID** `LttkLzXM` (NeoForge 2.0.1.3). Using `2.0.1.3` resolves to the Fabric artifact instead.
- **ManasCore bundled modules**: ManasCore's API classes live under `META-INF/jars/` in its shell jar. The `extractManascoreModules` task unpacks them into `libs/manascore/` (compile-only) before compilation. Re-run it after upgrading ManasCore.

### Project structure

```text
src/main/java/com/example/tensura_keybinding/
├── TensuraKeybinding.java          # Common entry: logger + network payloads
├── TensuraKeybindingClient.java    # Client entry: config, keybinds, events, HUD
├── client/
│   ├── ClientEvents.java           # Left-side tab button on Tensura's main screen
│   ├── KeySlotData.java            # Slot data persisted to config/*.json
│   ├── KeySlotHandler.java         # Key handling, mode switching, C2S packets
│   ├── KeySlotHud.java             # Persistent HUD above the native ability bar
│   └── KeySlotScreen.java          # Slot configuration screen
└── network/
    ├── KeySlotActionPayload.java   # C2S press/release payload
    └── KeySlotActionHandler.java   # Server-side native skill activation

src/main/resources/
├── META-INF/neoforge.mods.toml
└── assets/tensura_keybinding/lang/{en_us,zh_cn}.json
```

### How it works

Tensura's `ACTIVATE_SKILL` listener cancels activation unless the skill sits in the requested preset slot with a matching mode. On key press, the server therefore:

1. Verifies the skill is learned and the mode is valid;
2. Looks for an exact slot match, then an empty slot, then falls back to the last slot;
3. Temporarily writes the skill and mode, then calls `startHoldSkill(skillId, slot, mode)`;
4. Restores the original slot in a `finally` block within the same tick;
5. Calls `handleSkillRelease(...)` when the key is released.

The original slot's skill state is not interrupted: passive/toggle state lives in the skill instance or `SkillStorage`, and cooldowns, mastery, and attribute bonuses are independent of the slot reference.

### Known limitations

- A held-key state can persist if the player quits while holding a slot key (a logout cleanup is planned).
- The HUD is not drawn when Tensura's native ability HUD is disabled.
- Right-side HUD alignment is heuristic; the left-side layout is the primary target.
- Tensura 2.0.1.3 ships no official `zh_cn.json`, so skill names remain in English. This mod only translates its own UI.

### License

Released under the [MIT License](LICENSE).

---

## 中文

**Tensura: Reincarnated** 的附属模组，为技能提供一个独立于原生技能栏的三个快捷槽，并为每个槽位绑定独立按键，方便在不切换技能栏 preset 的情况下直接释放常用技能。

- **mod id**：`tensura_keybinding`
- **显示名**：`Tensura: Key binding`
- **版本**：`1.0.0`
- **环境**：Minecraft 1.21.1 / NeoForge 21.1.251 / JDK 21
- **开源协议**：[MIT](LICENSE)

### 功能

1. **三个技能快捷槽**：与 Tensura 原生技能栏（preset）相互独立，互不干扰。
2. **独立按键**：默认 `J` / `K` / `L` 分别触发槽位 1 / 2 / 3，可在「选项 → 控制 → 按键绑定」中修改。
3. **B 界面入口**：在 Tensura 主界面（B 键）左侧新增一个入口标签按钮，点击进入槽位设置界面。
4. **槽位设置界面**：左侧是已学会技能列表（自动过滤抗性技能），先点击技能再点击槽位即可绑定；支持清除槽位。
5. **常驻 HUD**：在 Tensura 原生三个技能框上方横向显示三个槽位图标，位置与缩放跟随原生 HUD。
6. **原生触发逻辑**：按键最终走 Tensura 自己的 `startHoldSkill` / `handleSkillRelease`，冷却、精通、消耗与原生技能栏完全一致。

### 使用方法

1. 按 `B` 打开 Tensura 主界面，点击左侧新增的标签按钮进入**快捷槽设置**；
2. 在左侧列表中选择一个已学会的技能（绿色高亮）；
3. 点击右侧任意槽位完成绑定，重复操作可配置其余槽位；
4. 返回游戏后，按 `J` / `K` / `L` 释放对应槽位的技能；
5. 若技能有多段模式，按住 Tensura 的「下一个/上一个技能模式」键（默认左 Alt 等）再按槽位键即可切换模式，动作栏会显示当前模式。

槽位配置保存在 `config/tensura_keybinding-client.json`，按客户端保存。

### 安装

1. 安装 Minecraft 1.21.1 与 NeoForge 21.1.251 或更高版本；
2. 将以下模组放入 `mods/` 目录：
   - [Tensura: Reincarnated](https://modrinth.com/mod/tensura-reincarnated) 2.0.1.3 或更高版本（NeoForge 版本）
   - [ManasCore](https://modrinth.com/mod/manascore) 4.0.0.2
   - [Architectury API](https://modrinth.com/mod/architectury-api) 13.0.11
   - [GeckoLib](https://modrinth.com/mod/geckolib) 4.9.3
   - [SmartBrainLib](https://modrinth.com/mod/smartbrainlib) 1.16.11
   - [TerraBlender](https://modrinth.com/mod/terrablender) 4.1.0.8
   - 本模组 `tensura_keybinding-1.0.0.jar`
3. 启动游戏。

> 本模组同时需要安装在客户端与服务端；仅客户端安装时无法完成技能触发。

### 从源码构建

```bash
cd C:/OE/MC/mod/explore-template-1.21.1

# 编译
./gradlew processResources compileJava --offline

# 启动客户端
./gradlew runClient --offline
```

> **必须使用 `--offline`**：当前网络下 `maven.neoforged.net` 会超时；依赖已全部缓存，离线模式可正常编译和启动。新增/升级依赖时需要先联网下载一次。

#### 环境说明

- **JDK 21**：`gradle.properties` 中已固定 `org.gradle.java.home=C:/Program Files/Java/jdk-21`。若 Windows 用户名包含非 ASCII 字符，使用其他 JDK 可能导致 Gradle worker 启动失败（`ClassNotFoundException: GradleWorkerMain`）。
- **Tensura 依赖**：构建时引用 Modrinth 的**版本 ID** `LttkLzXM`（NeoForge 2.0.1.3）；写成 `2.0.1.3` 会解析到 Fabric 包。
- **ManasCore 内嵌模块**：ManasCore 的 API 类位于 shell jar 的 `META-INF/jars/`，编译前由 `extractManascoreModules` 任务解压到 `libs/manascore/`（仅 compileOnly）。升级 ManasCore 后重新运行该任务。

### 项目结构

```text
src/main/java/com/example/tensura_keybinding/
├── TensuraKeybinding.java          # 公共入口：初始化日志 + 注册网络 payload
├── TensuraKeybindingClient.java    # 客户端入口：加载配置、注册按键/事件/HUD
├── client/
│   ├── ClientEvents.java           # Tensura 主界面左侧入口按钮
│   ├── KeySlotData.java            # 三个槽位配置，持久化到 config/*.json
│   ├── KeySlotHandler.java         # 按键处理、模式切换、发送 C2S 包
│   ├── KeySlotHud.java             # 原生技能栏上方的常驻 HUD
│   └── KeySlotScreen.java          # 槽位设置界面
└── network/
    ├── KeySlotActionPayload.java   # C2S press/release 数据包
    └── KeySlotActionHandler.java   # 服务端触发原生技能逻辑

src/main/resources/
├── META-INF/neoforge.mods.toml
└── assets/tensura_keybinding/lang/{en_us,zh_cn}.json
```

### 实现说明

Tensura 的 `ACTIVATE_SKILL` 监听器要求技能必须位于当前技能栏 preset 的指定槽位且模式一致，否则会取消触发。因此服务端收到按键包后：

1. 校验技能已学会且模式合法；
2. 在预设中寻找完全匹配的槽位，没有则寻找空槽位，再没有则使用最后一个槽位；
3. 临时写入技能与模式，调用 `startHoldSkill(skillId, slot, mode)`；
4. 在 `finally` 中立即恢复原槽位（同一 tick 内完成）；
5. 松开按键时调用 `handleSkillRelease(...)`。

原槽位的技能状态不会中断——被动/持续技能状态保存在技能实例或 `SkillStorage` 中，冷却、精通、属性加成均与槽位引用无关。

### 已知限制

- 按住槽位键时退出游戏，按键「按住」状态可能残留（后续可加登出清理）。
- 若 Tensura 原生技能 HUD 被禁用，本模组 HUD 也不会绘制。
- HUD 对原生右侧布局的 x 对齐为启发式，当前主要适配左侧布局。
- Tensura 2.0.1.3 没有官方 `zh_cn.json`，技能名本身仍显示英文；本模组只提供自身 UI 的中英文本。

### 许可

本项目以 [MIT License](LICENSE) 开源。
