package quitersstorssguard.records;

public record SqlitePragmaSettings(
        String synchronous,
        String journalMode,
        int cacheSize,
        int pageSize,
        String tempStore,
        String lockingMode,
        int mmapSize,
        int autoVacuum
    ) {}
