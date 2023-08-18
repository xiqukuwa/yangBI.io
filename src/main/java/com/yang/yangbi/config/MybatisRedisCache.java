package com.yang.yangbi.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.cache.Cache;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.jedis.JedisClusterConnection;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
@Slf4j
public class MybatisRedisCache implements Cache {

	// 读写锁
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

	private RedisTemplate redisTemplate;

	private RedisTemplate getRedisTemplate(){
		//通过ApplicationContextHolder工具类获取RedisTemplate
		if (redisTemplate == null) {
			redisTemplate = (RedisTemplate) ApplicationContextHolder.getBeanByName("redisTemplate");
		}
		return redisTemplate;
	}


	private final String id;


	public MybatisRedisCache(String id) {
		if (id == null) {
			throw new IllegalArgumentException("Cache instances require an ID");
		}
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public void putObject(Object key, Object value) {
		//使用redis的Hash类型进行存储
		getRedisTemplate().opsForHash().put(id,key.toString(),value);
	}

	@Override
	public Object getObject(Object key) {
		try {
			//根据key从redis中获取数据
			return getRedisTemplate().opsForHash().get(id,key.toString());
		} catch (Exception e) {
			e.printStackTrace();
			log.error("缓存出错 ");
		}
		return null;
	}

	@Override
	public Object removeObject(Object key) {
		if (key != null) {
			getRedisTemplate().delete(key.toString());
		}
		return null;
	}

	@Override
	public void clear() {
		log.debug("清空缓存");
		if (redisTemplate == null) {
			redisTemplate = (RedisTemplate<String, Object>) ApplicationContextHolder.getBeanByName("redisTemplate");
		}
		try {
			Set<String> keys = scanMatch(this.id);
			if (!CollectionUtils.isEmpty(keys)) {
				redisTemplate.delete(keys);
			}
		} catch (Exception e) {
			log.error("清空缓存", e);
		}
	}

	private static final Integer SCAN_COUNT = 10000;
	/**
	 * 使用scan遍历key
	 * 为什么不使用keys 因为Keys会引发Redis锁，并且增加Redis的CPU占用,特别是数据庞大的情况下。这个命令千万别在生产环境乱用。
	 * 支持redis单节点和集群调用
	 *
	 * @param matchKey
	 * @return
	 */
	public Set<String> scanMatch(String matchKey) {
		Set<String> keys = new HashSet();
		RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
		RedisConnection redisConnection = connectionFactory.getConnection();
		Cursor<byte[]> scan = null;
		//集群
		if(redisConnection instanceof JedisClusterConnection){
			RedisClusterConnection clusterConnection = connectionFactory.getClusterConnection();
			Iterable<RedisClusterNode> redisClusterNodes = clusterConnection.clusterGetNodes();
			Iterator<RedisClusterNode> iterator = redisClusterNodes.iterator();
			while (iterator.hasNext()) {
				RedisClusterNode next = iterator.next();
				scan = clusterConnection.scan(next, ScanOptions.scanOptions().match(matchKey).count(Integer.MAX_VALUE).build());
				while (scan.hasNext()) {
					keys.add(new String(scan.next()));
				}
				try {
					if(scan !=null){
						scan.close();
					}
				} catch (Exception e) {
					log.error("scan遍历key关闭游标异常",e);
				}
			}
			return keys;
		}
		//单机
		if(redisConnection instanceof JedisConnection){
			scan = redisConnection.scan(ScanOptions.scanOptions().match(matchKey + "*").count(SCAN_COUNT).build());
			while (scan.hasNext()) {
				//找到一次就添加一次
				keys.add(new String(scan.next()));
			}
			try {
				if (scan != null) {
					scan.close();
				}
			} catch (Exception e) {
				log.error("scan遍历key关闭游标异常", e);
			}
			return keys;
		}

		return keys;

	}

	@Override
	public int getSize() {
		Long size = (Long) getRedisTemplate().execute((RedisCallback<Long>) RedisServerCommands::dbSize);
		return size.intValue();
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return this.readWriteLock;
	}
}
