package cn.iocoder.yudao.module.oa.service.dict;

import cn.iocoder.yudao.module.oa.dal.dataobject.dict.FootballSystemDictDataDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.FootballSystemDictTypeDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictDataDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictTypeDO;
import cn.iocoder.yudao.module.oa.service.system.SystemDictAdapter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictService {

    private final SystemDictAdapter systemDictAdapter;

    private final Map<String, CacheEntry> dataCache = new ConcurrentHashMap<>();
    private final LongAdder dataCacheMisses = new LongAdder();
    private static final long TTL_NANOS = Duration.ofMinutes(5).toNanos();

    private ScheduledExecutorService sweeper;

    @PostConstruct
    void init() {
        sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "dict-cache-sweeper");
            t.setDaemon(true);
            return t;
        });
        sweeper.scheduleAtFixedRate(this::sweep, 5, 5, TimeUnit.MINUTES);
    }

    @PreDestroy
    void shutdown() {
        if (sweeper != null) {
            sweeper.shutdownNow();
        }
    }

    private void sweep() {
        long now = System.nanoTime();
        dataCache.entrySet().removeIf(e -> e.getValue().expireAtNanos < now);
    }

    private static class CacheEntry {
        final List<SysDictDataDO> rows;
        final long expireAtNanos;

        CacheEntry(List<SysDictDataDO> rows, long ttlNanos) {
            this.rows = rows;
            this.expireAtNanos = System.nanoTime() + ttlNanos;
        }
    }

    public boolean isValidValue(String dictType, String value) {
        return systemDictAdapter.isValidValue(dictType, value);
    }

    public boolean typeExists(String dictType) {
        return systemDictAdapter.typeExists(dictType);
    }

    public List<SysDictDataDO> listByType(String dictType) {
        if (dictType == null || dictType.isBlank()) {
            return List.of();
        }
        CacheEntry cached = dataCache.get(dictType);
        long now = System.nanoTime();
        if (cached != null && cached.expireAtNanos > now) {
            return cached.rows;
        }
        dataCacheMisses.increment();
        List<SysDictDataDO> rows = systemDictAdapter.listEnabledDataByType(dictType).stream()
                .map(this::toDataDO)
                .collect(Collectors.toList());
        dataCache.put(dictType, new CacheEntry(rows, TTL_NANOS));
        return rows;
    }

    public List<SysDictTypeDO> listAllTypes() {
        return systemDictAdapter.listEnabledTypes().stream()
                .map(this::toTypeDO)
                .collect(Collectors.toList());
    }

    public void evictCache() {
        dataCache.clear();
    }

    public long getCacheMisses() {
        return dataCacheMisses.sum();
    }

    public int getCacheSize() {
        return dataCache.size();
    }

    private SysDictDataDO toDataDO(FootballSystemDictDataDO d) {
        SysDictDataDO row = new SysDictDataDO();
        row.setId(d.getId());
        row.setDictType(d.getDictType());
        row.setLabel(d.getLabel());
        row.setDictValue(d.getValue());
        row.setSort(d.getSort());
        row.setStatus(SystemDictAdapter.toOpsStatus(d.getStatus()));
        row.setColorType(d.getColorType());
        row.setRemark(d.getRemark());
        return row;
    }

    private SysDictTypeDO toTypeDO(FootballSystemDictTypeDO t) {
        SysDictTypeDO row = new SysDictTypeDO();
        row.setId(t.getId());
        row.setType(t.getType());
        row.setName(t.getName());
        row.setStatus(SystemDictAdapter.toOpsStatus(t.getStatus()));
        return row;
    }
}
