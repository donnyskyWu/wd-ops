package cn.iocoder.yudao.module.oa.service.dict;

import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictDataDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictTypeDO;
import cn.iocoder.yudao.module.oa.dal.mysql.dict.SysDictDataMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.dict.SysDictTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

@Service
@RequiredArgsConstructor
public class DictService {

    private final SysDictDataMapper sysDictDataMapper;
    private final SysDictTypeMapper sysDictTypeMapper;

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
        if (value == null || value.isBlank()) {
            return false;
        }
        Long count = sysDictDataMapper.selectCount(new LambdaQueryWrapper<SysDictDataDO>()
                .eq(SysDictDataDO::getDictType, dictType)
                .eq(SysDictDataDO::getDictValue, value)
                .eq(SysDictDataDO::getStatus, "ENABLED"));
        return count != null && count > 0;
    }

    public boolean typeExists(String dictType) {
        if (dictType == null || dictType.isBlank()) {
            return false;
        }
        Long count = sysDictTypeMapper.selectCount(new LambdaQueryWrapper<SysDictTypeDO>()
                .eq(SysDictTypeDO::getType, dictType)
                .eq(SysDictTypeDO::getStatus, "ENABLED"));
        return count != null && count > 0;
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
        List<SysDictDataDO> rows = sysDictDataMapper.selectList(new LambdaQueryWrapper<SysDictDataDO>()
                .eq(SysDictDataDO::getDictType, dictType)
                .eq(SysDictDataDO::getStatus, "ENABLED")
                .orderByAsc(SysDictDataDO::getSort)
                .orderByAsc(SysDictDataDO::getDictValue));
        dataCache.put(dictType, new CacheEntry(rows, TTL_NANOS));
        return rows;
    }

    public List<SysDictTypeDO> listAllTypes() {
        return sysDictTypeMapper.selectList(new LambdaQueryWrapper<SysDictTypeDO>()
                .eq(SysDictTypeDO::getStatus, "ENABLED")
                .orderByAsc(SysDictTypeDO::getId));
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
}
