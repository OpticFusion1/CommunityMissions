# CommunityMissions (Spigot 1.21+)

CommunityMissions is a community progression plugin focused on **rotating cooperative objectives** that unlock temporary global perks for all players.

## Features

- Per-world tasks
- PlaceholderAPI support
- Rotating mission pool with configurable interval and concurrent mission count.
- Multi-objective tracking:
  - block break by material
  - mob kills by entity type
  - fish catches
  - crafting objectives by output item
  - movement distance objectives
  - aggregate online-time objectives
- Milestone rewards with:
  - global potion-like perk activations
  - custom broadcast messages
  - optional console command dispatches
- Runtime mission GUI (`/missions missions`) with progress bars and milestone status.
- Expanded commands for engagement and transparency:
  - `/missions active` (current mission progress snapshot)
  - `/missions next` (time until next rotation)
  - `/missions stats [player]` (personal contribution + points profile)
  - `/missions top points` (lifetime points leaderboard)
- Built-in contributor leaderboard (`/missions top`).
- Admin controls:
  - `/missions rotate`
  - `/missions reload`
  - `/missions contribute <mission> <player> <amount>`

## Installation

1. Build with Maven:
   ```bash
   mvn -DskipTests package
   ```
2. Drop `communitymissions.jar` into `plugins/`.
3. Start server once to generate defaults.
4. Tune `config.yml` mission mix and milestone pacing.
5. Reload with `/missions reload`.

## Permission nodes

- `communitymissions.admin` (default: op)

## Command reference

- `/missions missions` - open mission menu
- `/missions active` - show active mission progress
- `/missions next` - show time until next rotation
- `/missions stats [player]` - show player mission stats
- `/missions top [active|points]` - show leaderboard views
- `/missions rotate` - force mission rotation (admin)
- `/missions reload` - reload config + missions (admin)
- `/missions contribute <mission> <player> <amount>` - force contribution entry (admin)
