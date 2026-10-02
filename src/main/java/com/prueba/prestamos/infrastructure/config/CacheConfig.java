package com.prueba.prestamos.infrastructure.config;

import com.prueba.prestamos.application.PrestamoService;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.boot.cache.autoconfigure.CacheManagerCustomizer;
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@Configuration
@EnableCaching
public class CacheConfig {

 
    @Bean
    JCacheManagerCustomizer cachesPrestamos() {
        return cacheManager -> {
            if (cacheManager.getCache(PrestamoService.CACHE_PRESTAMOS_USUARIO) == null) {
                cacheManager.createCache(PrestamoService.CACHE_PRESTAMOS_USUARIO,
                        Eh107Configuration.fromEhcacheCacheConfiguration(
                                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                                Object.class, Object.class, ResourcePoolsBuilder.heap(1000))
                                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(10)))));
            }
        };
    }

  
    @Bean
    CacheManagerCustomizer<JCacheCacheManager> cacheTransaccional() {
        return cacheManager -> cacheManager.setTransactionAware(true);
    }
}
