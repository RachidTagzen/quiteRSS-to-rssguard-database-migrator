# QuiteRSS to RSSGuard Database Migrator

![Java](https://img.shields.io/badge/Java-24-blue?logo=java) ![SQLite](https://img.shields.io/badge/SQLite-3.x-green?logo=sqlite) ![Platform](https://img.shields.io/badge/Linux_Mint-22_Cinnamon-mint?logo=linuxmint)

> :warning: **DISCLAIMER**: This tool comes with ABSOLUTELY NO WARRANTY. The maintainers are not responsible for any data loss or database corruption. Always maintain verified backups before migration.

## Key Features

- Batch migration of feeds, categories and articles
- Preserves:
  - Feed hierarchy and folder structure
  - Article read/unread states
  - Starred articles


## 🚫 Known Limitations
- Base64 images from QuiteRSS won't display in RSSGuard. The image storage format isn't compatible between applications (Don't ask me why!).


- All migrated feeds are set to "RSS 0.91/0.92/0.93" by default. This means:
    - Many feeds won't fetch content after migration
    - You'll need to manually verify and update feed types in RSSGuard

#### Supported RSSGuard Feed Types:
    - ATOM 1.0
    - RDF (RSS 1.0)
    - RSS 0.91/0.92/0.93
    - RSS 2.0/2.0.1
    - iCalendar
    - JSON 1.0/1.1
    - Sitemap

## System Requirements
- ⚠️ Valid empty RSSGuard database (tested with schema_version=10)
- ✅ Java 24 (Temurin distribution recommended)
- ✅ SQLite 3.x database files
- ⚠️ Minimum 2GB RAM for large databases


## 💾 Backup Notice
This tool makes DIRECT modifications to your database files. **Always**:
- Create a complete backup
- Verify the backup is restorable
- Test migration on a copy first


## Usage

### Method 1: From Source (Compile Yourself)
```bash
javac -d bin src/*.java
jar cvfe migrator.jar Main -C bin .
java -jar migrator_v1.0.jar /path/to/feeds.db /path/to/database.db
```

### Method 2: Precompiled JAR (Recommended for Most Users)
- Download the latest migrator.jar from ![Releases](https://github.com/RachidTagzen/quiteRSS-to-rssguard-database-migrator/releases)
- Make executable (Linux) :
```bash
chmod +x migrator_v1.0.jar
```
- Run directly (Don't forget to set the jar as executable) :
```bash
java -jar migrator_v1.0.jar /path/to/feeds.db /path/to/database.db
```

## Performance Tuning
 Setting | Migration Value | Normal Default | Risk Level | Effect | Description |
 |-----|-----|-----|-----|-----|-----|
 | SYNCHRONOUS | OFF | FULL | 🔴 High | Maximum write speed | Dangerous! Can corrupt database if system crashes |
 | JOURNAL_MODE | OFF | WAL | 🔴 High | No crash protection | More aggressive than MEMORY - removes all crash protection |
 | CACHE_SIZE | 20000 pages | 2000 pages | 🟡 Medium | 32MB cache reduces I/O | Increases memory usage |
 | LOCKING_MODE | EXCLUSIVE | NORMAL | 🟢 Low | Prevents concurrent access | Safe but blocks other processes |
 | TEMP_STORE | MEMORY | DEFAULT | 🟢 Low | Faster temporary operations | Uses RAM for temp storage |

**Risk Legend**:
- 🔴 High: May corrupt database if interrupted
- 🟡 Medium: May cause performance issues
- 🟢 Low: Generally safe


## Support Policy
### ✅ Verified Working On:
- Linux Mint 22
- Java 24 (Temurin)

### ⚠️ Untested But Might Work:
- Other Linux distributions
- Java 17-23

### ❌ Untested Systems:
- Windows/WSL
- macOS
- ARM architectures
