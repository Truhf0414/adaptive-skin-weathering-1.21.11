# Adaptive Skin Weathering — Fabric 1.21.11 Port (Unofficial)

*English | [中文](#adaptive-skin-weathering--fabric-12111-移植版非官方)*

An unofficial Fabric **1.21.11** port of Blagoslav's *[ASW] Adaptive Skin Weathering*.
Dirt, sand, stone dust, snow and blood build up on player skins based on how you move, what you walk on, the damage you take and the biome you're in.
Not affiliated with or endorsed by the original authors. Published with the original author's permission.

## What it adds

- Dirt layers that depend on the surface (mud/grass, sand, stone dust, snow/ice) and the biome, tinted to match the player's skin tone
- Blood overlays from damage, with dripping particles at low HP
- Wetness, soot (when on fire) and frost overlays; rain and water wash them off over time
- Shivering animation in the cold, and dirt picked up when landing from a fall
- First-person arms show the same overlays
- `/weathering` commands (dirt, blood, clear, shake, breath, rate) for operators
- Config screen through Cloth Config + Mod Menu (optional); integrations with Supplementaries soap and Create fans

## Requirements

Everything below has to match — mismatched versions just won't load.

| | Version |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.2 or newer |
| Fabric API | 0.141.4+1.21.11 (tested) |
| Java | 21 |
| Cloth Config *(optional, for the config screen)* | 21.11.153 or newer |
| Mod Menu *(optional, for the config screen)* | 17.0.0 or newer |

## Install

Drop the mod jar (`1.2.1-1.21.11-port`) into `mods/`.
For multiplayer, install it on **both the server and every client**: the server tracks the weathering state and syncs it to clients, which do the rendering.

## Changes from upstream

The upstream source isn't public, so this port is based on the decompiled `1.2.1-26.2` release. What differs:

- Ported to the 1.21.11 Mojang-mapped API and Fabric API names (Loom remap, Java 21)
- **Fix:** invisible players no longer show the weathering overlay floating in mid-air, and no longer drip water/dirt/blood/snow particles that give away their position (both happened upstream too)
- `/weathering` command replies were hardcoded Russian; they are now translatable (`en_us`, `ru_ru`, `zh_tw`)
- Added a Traditional Chinese (`zh_tw`) translation
- Added two lang keys missing upstream (`sequentialDirt` option and its tooltip)

**Known issue (same as upstream):** the breath-steam effect is computed but renders fully transparent, because it samples a transparent texel of `generic_0.png`. It is left as-is to match the original.

## Build

```
./gradlew build
```

Artifact: `build/libs/`.

## Credits

Original mod by **Blagoslav** (GitHub: [zhiroslavPrime](https://github.com/zhiroslavPrime)), assets by **Launt1m** — [Modrinth page](https://modrinth.com/mod/adaptive-skin-weathering).
This is an unofficial port; all the original design is their work. Ported to 1.21.11 by Truhf0414.

## License

Licensed under **LGPL-3.0-or-later**, the license declared by the original mod — see [`COPYING.LESSER`](COPYING.LESSER) (LGPL-3.0) and [`COPYING`](COPYING) (GPL-3.0, which the LGPL incorporates).
This port keeps the original license and attribution, and its full source lives in this repository.

---

# Adaptive Skin Weathering — Fabric 1.21.11 移植版（非官方）

*[English](#adaptive-skin-weathering--fabric-12111-port-unofficial) | 中文*

Blagoslav 的 *[ASW] Adaptive Skin Weathering* 的非官方 Fabric **1.21.11** 移植版。
泥土、沙子、石頭粉塵、雪和血跡，會依你的移動方式、踩的地面、受到的傷害和所在的生物群系，累積在玩家皮膚上。
與原作者無關，也非原作者背書。本移植版經原作者同意後發佈。

## 加入了什麼

- 依地面種類（泥巴／草地、沙子、石頭粉塵、雪／冰）和生物群系而不同的髒污層，顏色會配合玩家膚色
- 受傷產生的血跡，低血量時會滴血
- 濕氣、煙灰（著火時）和霜凍效果；雨水和水會慢慢把它們洗掉
- 寒冷時的發抖動畫，從高處落地時會沾上髒污
- 第一人稱的手臂也會顯示同樣的效果
- 給管理員用的 `/weathering` 指令（dirt、blood、clear、shake、breath、rate）
- 透過 Cloth Config + Mod Menu 開設定畫面（選用）；支援 Supplementaries 肥皂和 Create 鼓風機

## 需求

以下版本都要對上，對不上就載入不了。

| | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.2 以上 |
| Fabric API | 0.141.4+1.21.11（測試過） |
| Java | 21 |
| Cloth Config *（選用，設定畫面用）* | 21.11.153 以上 |
| Mod Menu *（選用，設定畫面用）* | 17.0.0 以上 |

## 安裝

把 mod jar（`1.2.1-1.21.11-port`）放進 `mods/`。
多人遊戲時，**伺服器和每個玩家的 client 都要裝**：伺服器負責記錄狀態並同步給 client，client 負責畫出效果。

## 與上游的差異

上游的原始碼沒有公開，所以這個移植版是以反編譯的 `1.2.1-26.2` 版本為基礎。不同的地方是：

- 移植到 1.21.11 的 Mojang 名稱 API 和 Fabric API 名稱（Loom remap、Java 21）
- **修正**：隱形的玩家不會再有髒污外層浮在半空中，也不會再滴出水、泥、血、冰晶粒子暴露位置（上游也有這兩個問題）
- `/weathering` 指令的回覆原本是寫死的俄文，現在可以翻譯（`en_us`、`ru_ru`、`zh_tw`）
- 新增繁體中文（`zh_tw`）翻譯
- 補上上游缺少的兩個翻譯 key（`sequentialDirt` 選項和它的提示文字）

**已知問題（上游也一樣）**：呼氣白霧有在計算，但畫出來是全透明的，因為它取樣到 `generic_0.png` 裡一個透明的像素。為了跟原作一致，保留原樣。

## Build

```
./gradlew build
```

產物在 `build/libs/`。

## 原作者

原作者 **Blagoslav**（GitHub：[zhiroslavPrime](https://github.com/zhiroslavPrime)），素材 **Launt1m** — [Modrinth 頁面](https://modrinth.com/mod/adaptive-skin-weathering)。
這是非官方移植版，原本的設計都是他們的心血。1.21.11 移植：Truhf0414。

## 授權

採用 **LGPL-3.0-or-later**，也就是原作宣告的授權。全文見 [`COPYING.LESSER`](COPYING.LESSER)（LGPL-3.0）與 [`COPYING`](COPYING)（GPL-3.0，LGPL 以它為基礎）。
本移植版保留原作的授權與署名，完整原始碼都在這個 repo 裡。
