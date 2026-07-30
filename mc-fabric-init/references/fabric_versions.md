# Fabric Version Mapping for MC 1.21.x

This document provides the version mapping table for Fabric mod development targeting Minecraft 1.21.x.

## Version Table

| MC Version | Yarn Mappings | Fabric Loader | Fabric API | Loom | Java |
|------------|---------------|---------------|------------|------|------|
| 1.21 | 1.21+build.9 | 0.15.11 | 0.100.7+1.21 | 1.7-SNAPSHOT | 21 |
| 1.21.1 | 1.21.1+build.3 | 0.16.9 | 0.105.0+1.21.1 | 1.7-SNAPSHOT | 21 |
| 1.21.3 | 1.21.3+build.2 | 0.16.9 | 0.116.0+1.21.3 | 1.7-SNAPSHOT | 21 |
| 1.21.4 | 1.21.4+build.8 | 0.16.10 | 0.119.2+1.21.4 | 1.7-SNAPSHOT | 21 |
| 1.21.5 | 1.21.5+build.2 | 0.16.10 | 0.119.5+1.21.5 | 1.7-SNAPSHOT | 21 |

## How to Verify Latest Versions

Versions change frequently. To find the latest:

1. **Yarn mappings**: https://fabricmc.net/develop/yarn/
2. **Fabric Loader**: https://fabricmc.net/develop/loader/
3. **Fabric API**: https://modrinth.com/mod/fabric-api/versions or https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/
4. **Loom**: https://maven.fabricmc.net/fabric-loom/fabric-loom.gradle.plugin/

## Key Migration Notes

### 1.20.x -> 1.21.x Breaking Changes

- **Java 21 required** (was Java 17 for 1.20.x)
- `Registry.register()` signature changes in some sub-registries
- Item group registration changed to use `ItemGroupEvents.modifyEntriesEvent()`
- Block settings use `AbstractBlock.Settings` (factory methods renamed in some cases)
- `net.minecraft.util.Identifier` still used; in 1.21+ some registries moved to `RegistryKey`
- Worldgen: `BiomeModifications` API stable, but feature configuration classes may have changed signatures
- Recipe JSON format: `type` field now uses full `Identifier` format for vanilla types (e.g., `minecraft:crafting_shaped`)

### 1.21.0 -> 1.21.1

- Mostly compatible; minor mapping fixes
- Safe to update without code changes in most cases

### 1.21.1 -> 1.21.3+

- Significant entity/networking changes
- `SpawnRestriction` and `MobEntity` API adjustments
- Some GUI/HUD rendering changes
- Check Fabric API changelog before updating

## gradle.properties Template

```properties
minecraft_version=1.21.1
yarn_mappings=1.21.1+build.3
loader_version=0.16.9
fabric_version=0.105.0+1.21.1
loom_version=1.7-SNAPSHOT
java_version=21
```
