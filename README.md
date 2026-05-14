# StellarDuelBridge
> A lightweight honor duel system designed for Vanilla+ Paper servers.

![Paper](https://img.shields.io/badge/Paper-1.21.10-2C2D30?style=flat-square&logo=paper)
![Java](https://img.shields.io/badge/Java-21-2C2D30?style=flat-square&logo=openjdk)
![Gradle](https://img.shields.io/badge/Build-Gradle-2C2D30?style=flat-square&logo=gradle)
![Storage](https://img.shields.io/badge/Storage-SQLite-2C2D30?style=flat-square&logo=sqlite)

Player-facing display name: **荣誉决斗 (Honor Duel)**

---

## ⚔️ Overview

`StellarDuelBridge` is a ritual-style private duel system for survival-focused Paper servers.

It is built for **Vanilla+ gameplay** where players want a clean, honorable way to resolve conflict without turning the whole server into a practice PvP network.

### What it is
- A lightweight duel bridge for survival servers
- A state-driven duel flow (invite → arena → fight → restore)
- A controlled combat scope: only matched duel participants can damage each other

### What it is not
- Not a large-scale arena network plugin
- Not an ELO/ranked practice core
- Not a full PvP ruleset replacement plugin

---

## ✨ Features

- Lightweight **bridge architecture** for survival environments
- Clear **Honor Duel state machine** (`INVITED` → `MODE_SELECTING` → `PREPARING` → `TELEPORTING` → `COUNTDOWN` → `FIGHTING` → `ENDING`)
- Random arena selection with cooldown and anti-repeat option
- Multiverse-friendly arena world workflow
- Passive PvPManager compatibility boundary
- SQLite-backed duel stats and match history
- GUI-based duel mode selection and confirmation
- `REAL_GEAR` mode (real equipment duel)
- `FAIR_KIT` mode (uniform kit duel)
- `EMPTY_RITUAL` mode (empty-hand / ceremonial weapon duel)
- `teleportAsync` + chunk preload for smoother arena entry/return
- Configurable GUI (`gui.yml`)
- Configurable core behavior (`config.yml`, `messages.yml`, `arenas.yml`)
- Asynchronous database operations via `CompletableFuture`
- `PlayerSnapshot` capture/restore with deferred recovery after respawn/join

---

## 🏗️ Architecture

### Responsibility split

```text
StellarDuelBridge
  ├─ ArenaManager       (arena config, random selection, cooldown lifecycle)
  ├─ DuelSessionManager (invite/session state machine and match flow)
  ├─ SnapshotService    (player snapshot capture/restore)
  ├─ GUI Layer          (mode confirmation inventory)
  ├─ Storage Layer      (SQLite async stats + match records)
  └─ Listeners          (combat gate, command block, movement/teleport guards)
```

### Ecosystem boundary

```text
DuelBridge      -> Controls duel eligibility, session flow, arena orchestration, result handling
PvPManager      -> Handles global combat-tag / PvP restrictions at server policy level
Multiverse-Core -> Handles world lifecycle and arena-world management
WorldGuard      -> Region policy boundary (detection-ready; strict region checks are not active in V1)
```

> V1 hook layer is detection-oriented. DuelBridge does not attempt to replace external world/PvP policy plugins.

---

## 🔁 Duel Flow

```text
/duel <player>
   │
   ├─ target: /duel accept
   │
   ├─ MODE_SELECTING (GUI confirm)
   │     └─ REAL_GEAR / FAIR_KIT / EMPTY_RITUAL
   │
   ├─ Arena allocate (AVAILABLE only)
   ├─ PlayerSnapshot capture
   ├─ Mode loadout preparation
   ├─ Chunk preload + teleportAsync
   │
   ├─ COUNTDOWN
   ├─ FIGHTING
   │
   ├─ End condition: death / surrender / quit / timeout
   ├─ Async stats + match record write (SQLite)
   ├─ Snapshot restore
   └─ teleportAsync return (or fallback sync teleport)
```

---

## 🛡️ Duel Modes

| Mode | Purpose | Equipment Behavior | Typical Use |
|---|---|---|---|
| `REAL_GEAR` | Real risk/reality duel | Uses live inventory/equipment; snapshot restore policy is configurable | Survival disputes, gear proof duels |
| `FAIR_KIT` | Skill-first duel | Clears inventory and applies configured kit | Fair technical sparring |
| `EMPTY_RITUAL` | Ceremony/lightweight duel | Clears inventory; optional basic ritual weapon | Events, social ritual battles |

---

## ⌨️ Commands

### Player commands

| Command | Description |
|---|---|
| `/duel <player>` | Send an honor duel invitation |
| `/duel accept` | Accept latest pending invitation |
| `/duel deny` | Deny latest pending invitation |
| `/duel cancel` | Cancel your outgoing invitation |
| `/duel leave` | Surrender and leave your current duel |
| `/duel stats [player]` | View duel stats (self or target) |
| `/duel help` | Show command help |

### Admin commands

| Command | Description |
|---|---|
| `/dueladmin create <arenaId>` | Create arena config entry |
| `/dueladmin delete <arenaId>` | Delete arena config entry |
| `/dueladmin setspawn <arenaId> <1\|2>` | Set duel spawn point |
| `/dueladmin setspectator <arenaId>` | Set spectator point (reserved for future spectator flow) |
| `/dueladmin setreturn` | Set global return point |
| `/dueladmin enable <arenaId>` | Enable arena |
| `/dueladmin disable <arenaId>` | Disable arena |
| `/dueladmin list` | Show arena status list |
| `/dueladmin reload` | Reload config/messages/arenas |

---

## 🔐 Permissions

| Node | Default | Description |
|---|---|---|
| `stellarduelbridge.command.duel` | `true` | Use player duel command |
| `stellarduelbridge.command.admin` | `op` | Use admin command root |
| `stellarduelbridge.admin.*` | `op` | All arena/admin management permissions |
| `stellarduelbridge.bypass.*` | `op` | All bypass permissions |
| `stellarduelbridge.bypass.cooldown` | `op` | Bypass invite cooldown |
| `stellarduelbridge.bypass.disabled-world` | `op` | Bypass disabled world restriction |
| `stellarduelbridge.bypass.command-block` | `op` | Bypass blocked command list during duel |

---

## 📦 Installation

1. Use **Paper 1.21.10** and **Java 21**.
2. Build or download the plugin jar.
3. Put `StellarDuelBridge-<version>.jar` into `plugins/`.
4. Start the server once to generate:
   - `config.yml`
   - `gui.yml`
   - `messages.yml`
   - `arenas.yml`
5. Configure arena worlds (recommended with Multiverse-Core).
6. Restart or run `/dueladmin reload`.

---

## 🗺️ Arena Setup

### Minimal setup path

```text
/dueladmin create duel_forest_01
/dueladmin setspawn duel_forest_01 1
/dueladmin setspawn duel_forest_01 2
/dueladmin enable duel_forest_01
```

### Complete example (with dedicated world)

```text
/mv create duel_arena_01 normal
/mvtp duel_arena_01

/dueladmin create duel_forest_01
/dueladmin setspawn duel_forest_01 1
/dueladmin setspawn duel_forest_01 2
/dueladmin setspectator duel_forest_01
/dueladmin setreturn
/dueladmin enable duel_forest_01
/dueladmin list
```

---

## ⚙️ Configuration (Selected Keys)

```yaml
settings:
  display-name: "荣誉决斗"
  invite-expire-seconds: 60
  request-cooldown-seconds: 15
  countdown-seconds: 5
  max-duration-seconds: 300
  return-mode: ORIGINAL_LOCATION # or LOBBY (requires setreturn)

integration:
  pvpmanager:
    enabled: true
    mode: PASSIVE
  multiverse:
    enabled: true
  worldguard:
    enabled: false

storage:
  type: SQLITE
  sqlite:
    file: "duel-stats.db"
  mysql:
    enabled: false
    fallback-to-sqlite-if-unavailable: true

modes:
  default: REAL_GEAR
  allow-player-selection: true

combat:
  freeze-during-countdown: true
  invulnerable-after-teleport-ticks: 60
  blocked-commands:
    - spawn
    - home
    - tpa
```

> V1 note: `type: MYSQL` is not implemented yet; with fallback enabled, runtime falls back to SQLite.

---

## 🧠 Technical Details

- **Language / Runtime:** Java 21 (`toolchain` + `release 21`)
- **Server API:** Paper API `1.21.10-R0.1-SNAPSHOT`
- **Storage:** SQLite (`sqlite-jdbc`) with schema/version bootstrap
- **Async model:** `CompletableFuture` + dedicated single-thread DB executor
- **Teleport model:** `World#getChunkAtAsync` preload + `Player#teleportAsync`
- **Text API:** Adventure + MiniMessage
- **GUI:** Inventory-based mode confirmation UI (`InventoryHolder` pattern)
- **State safety:** Session maps keyed by UUID and guarded by explicit duel states
- **Recovery:** Deferred snapshot restore on `PlayerRespawnEvent` / `PlayerJoinEvent`

---

## 🚀 Performance Philosophy

StellarDuelBridge is designed to stay light:

- Keep duel scope narrow (no global combat rewrite)
- Avoid blocking the main thread for database writes
- Use async teleport pipeline for arena entry/return
- Keep arena logic simple and predictable
- Prioritize stable survival-server behavior over feature-heavy practice systems

It is intentionally **not** a large PvP practice core.

---

## 🛠️ Development

### Build

```bash
./gradlew clean build
```

On Windows PowerShell:

```powershell
.\gradlew.bat clean build
```

Jar output:

```text
build/libs/StellarDuelBridge-<version>.jar
```

### Project layout (core)

```text
src/main/java/org/stellarvan/stellarDuelBridge
  ├─ arena
  ├─ command
  ├─ config
  ├─ duel
  ├─ gui
  ├─ hook
  ├─ listener
  ├─ snapshot
  └─ storage
```

---

## 🧭 Roadmap (Post V1)

- [ ] ELO / ranked rating layer
- [ ] Spectator mode (using arena spectator points)
- [ ] Real MySQL provider (production-ready)
- [ ] PlaceholderAPI expansion
- [ ] Web API for stats/match history
- [ ] ItemsAdder-based enhanced GUI skin

> These are planned directions and are **not** part of V1 scope.

---

## 🔌 Compatibility

| Plugin / Platform | Status | Notes |
|---|---|---|
| Paper 1.21.10 | ✅ Supported Target | Primary runtime target |
| PvPManager | 🟡 Passive Compatible | Detection-only hook in V1; server PvP policies remain external |
| Multiverse-Core | 🟡 Recommended | Detection-only hook in V1; use for arena world management |
| WorldGuard | 🟡 Optional Boundary | Detection-ready; strict region check flow not active in V1 |
| Vault | ⚪ Reserved | Detected only, no economy feature in V1 |
| PlaceholderAPI | ⚪ Reserved | Detected only, no expansion shipped in V1 |
| ItemsAdder | ⚪ Reserved | GUI config placeholders exist, no runtime integration flow in V1 |

---

## ⚠️ Known Limitations (V1)

- No spectator gameplay flow yet
- No ELO/ranked/MMR logic yet
- No cross-server duel support
- No automatic arena map reset pipeline
- No production MySQL implementation yet
- Not designed as a full practice PvP plugin
- Some configuration keys are reserved for future integrations

---

## 📄 License

This project is released under the **MIT License**.

---

## 🙏 Credits

- Designed for **StellarWorld**
- Built for **Vanilla+ survival gameplay**

