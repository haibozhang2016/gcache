package com.gmall.gcache.common.cache.spring;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.gmall.gcache.common.cache.util.RedisCache;
import com.gmall.gcache.common.cache.config.L2CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理器
 */
@Component
public class RedisCaffeineCacheManager implements CacheManager {

	private final Logger logger = LoggerFactory.getLogger(RedisCaffeineCacheManager.class);

	private ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

	private L2CacheConfig l2CacheConfig;


	private RedisCache redisService;

	private boolean dynamic = true;

	private Set<String> cacheNames;

	public RedisCaffeineCacheManager(L2CacheConfig l2CacheConfig,
									 RedisCache redisService) {
		super();
		this.l2CacheConfig = l2CacheConfig;
		this.redisService = redisService;
		this.dynamic = l2CacheConfig.isDynamic();
		this.cacheNames = l2CacheConfig.getCacheNames();
	}

	@Override
	public Cache getCache(String name) {
		Cache cache = cacheMap.get(name);
		if(cache != null) {
			return cache;
		}
		if(!dynamic && !cacheNames.contains(name)) {
			return null;
		}
		cache = new RedisCaffeineCache(name, redisService, caffeineCache(), l2CacheConfig);
		Cache oldCache = cacheMap.putIfAbsent(name, cache);
		logger.debug("create cache instance, the cache name is : {}", name);
		return oldCache == null ? cache : oldCache;
	}

	public com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache(){
		Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();
		if(l2CacheConfig.getCaffeine().getExpireAfterAccess() > 0) {
			cacheBuilder.expireAfterAccess(l2CacheConfig.getCaffeine().getExpireAfterAccess(), TimeUnit.SECONDS);
		}
		if(l2CacheConfig.getCaffeine().getExpireAfterWrite() > 0) {
			cacheBuilder.expireAfterWrite(l2CacheConfig.getCaffeine().getExpireAfterWrite(), TimeUnit.SECONDS);
		}
		if(l2CacheConfig.getCaffeine().getInitialCapacity() > 0) {
			cacheBuilder.initialCapacity(l2CacheConfig.getCaffeine().getInitialCapacity());
		}
		if(l2CacheConfig.getCaffeine().getMaximumSize() > 0) {
			cacheBuilder.maximumSize(l2CacheConfig.getCaffeine().getMaximumSize());
		}
		if(l2CacheConfig.getCaffeine().getRefreshAfterWrite() > 0) {
			cacheBuilder.refreshAfterWrite(l2CacheConfig.getCaffeine().getRefreshAfterWrite(), TimeUnit.SECONDS);
		}
		return cacheBuilder.build();
	}

	@Override
	public Collection<String> getCacheNames() {
		return this.cacheNames;
	}

	public void clearLocal(String cacheName, Object key) {
		Cache cache = cacheMap.get(cacheName);
		if(cache == null) {
			return;
		}

		RedisCaffeineCache redisCaffeineCache = (RedisCaffeineCache) cache;
		redisCaffeineCache.clearLocal(key);
	}
}
