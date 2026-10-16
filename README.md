# Global Building Toggle (Minecraft 1.21.1 / NeoForge 21.1.257 / Java 21)

Server-side global block-placement toggle. Install on the SERVER only.

## Commands
| Command | Who | Effect |
|---|---|---|
| `/building` , `/building status` | everyone | show current state |
| `/building off` | OP (level 2+) | regular players can no longer place blocks; broadcast to all |
| `/building on` | OP (level 2+) | everyone can place again; broadcast to all |

State is in-memory and resets to ON (building allowed) at every server start.

## Build
1. Install JDK 21.
2. Generate the wrapper once (needs Gradle 8.8+ installed): `gradle wrapper --gradle-version 8.10.2`
   (or copy `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` from the official NeoForge 1.21.1 MDK).
3. `./gradlew build` (Windows: `gradlew.bat build`)
4. JAR: `build/libs/global-building-toggle-1.0.0.jar`

## Server install
1. Install NeoForge 21.1.257 server (run the installer with `--installServer`).
2. Copy the JAR into `<server>/mods/`.
3. Start the server. Log should contain mod id `globalbuildingtoggle`.
4. Make yourself OP: `op <name>`. Test with `/building status`.
Clients do not need the mod (vanilla clients can join, since it registers no blocks/items/packets).

## How it works
* `BlockEvent.EntityPlaceEvent` (cancelled for non-OP ServerPlayers): NeoForge fires this for blocks placed via
  item use, including modded items, and restores the blocks when cancelled. This is the generic, list-free backstop.
* `PlayerInteractEvent.RightClickBlock`: only `setUseItem(FALSE)` for block items / filled buckets, so block
  interaction (chests, crafting, enchanting, anvils...) still works and the client gets an instant correction.
* `PlayerInteractEvent.RightClickItem`: blocks use-based placement (filled buckets, lily pads, frogspawn).
* Food block-items (sweet/glow berries) are not blocked from being eaten.
* Breaking is never touched.

## Limitations
* Mods that call `level.setBlock(...)` directly from their own packet/tool code (e.g. some wrenches, bit-chiselers,
  build wands, copy/paste tools, structure tools) without going through item `useOn` or firing `EntityPlaceEvent`
  cannot be intercepted generically. Test your modpack's tools and, if one bypasses, ask its author to fire
  `EntityPlaceEvent` or use that mod's own permission/config.
* Fake players (Create deployers, Mekanism/other block placers) are allowed by default;
  set `RESTRICT_FAKE_PLAYERS = true` in `BuildingEvents` to restrict them.
* Dispensers, endermen, falling blocks, fluid flow, fire spread, growth etc. are world behavior, not player
  placement, and are unaffected.
* Flint and steel (places fire), and bone meal-style item-use block changes are covered by the snapshot backstop
  and may be denied while building is off.
* "OP" = permission level 2+ (`OP_LEVEL`). Permission mods that grant level 2 count as OP.
* Not compiled in the authoring environment (no network). If a dependency version fails to resolve, bump
  the `net.neoforged.moddev` plugin version in `build.gradle`.

## Test checklist
Setup: server running, you = OP, second account = non-OP (or `deop` yourself temporarily).
Normal building (state ON)
- [ ] `/building status` says ENABLED for both accounts
- [ ] Non-OP places stone, a sign, a door, a bed, torch, water bucket, lava bucket
Disabled building
- [ ] OP runs `/building off`; all online players get the broadcast
- [ ] Non-OP running `/building off` or `/building on` is refused
- [ ] `/building status` (non-OP) shows DISABLED
- [ ] Non-OP cannot place stone, slabs, stairs, doors, beds, torches, signs, redstone, rails
- [ ] Non-OP cannot empty water/lava/powder-snow buckets, place lily pad or frogspawn
- [ ] Placement attempts show action-bar message, no ghost block remains, item count unchanged
- [ ] Non-OP can still break blocks (and drops/pickup work)
- [ ] Non-OP can still eat sweet berries / glow berries
- [ ] Creative-mode non-OP also cannot place
OP bypass
- [ ] OP can place all of the above while disabled
- [ ] `/building on` re-enables for non-OP; broadcast appears
Modded blocks
- [ ] Non-OP cannot place blocks from modded items while disabled; OP can
- [ ] Test each modded builder tool (wands, wrenches, chisels): note which bypass (see Limitations)
Workstations
- [ ] While disabled, non-OP can open and use crafting table, furnace, chest, barrel, ender chest, shulker (placed by OP),
      enchanting table, anvil, smithing table, grindstone, loom, stonecutter, villager trading
- [ ] Holding a block while right-clicking a chest still opens the chest
- [ ] Restart server: state resets to ENABLED
