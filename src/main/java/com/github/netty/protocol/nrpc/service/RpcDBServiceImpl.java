package com.github.netty.protocol.nrpc.service;

import java.util.*;

/**
 * RpcDBServiceImpl
 * @author wangzihao
 */
public class RpcDBServiceImpl implements RpcDBService {
    private final Map<String,ExpiryMap<String,byte[]>> memExpiryGroupMap = new HashMap<>(16);
    private static final String SHARING_GROUP = "/sharing";

    @Override
    public boolean exist2(String key, String group) {
        return getMemExpiryMap(group).containsKey(key);
    }

    @Override
    public void put(String key, byte[] data) {
        put4(key,data, -1, SHARING_GROUP);
    }

    @Override
    public void put3(String key, byte[] data, int expireSecond) {
        put4(key,data,expireSecond,SHARING_GROUP);
    }

    @Override
    public void put4(String key, byte[] data, int expireSecond, String group) {
        getMemExpiryMap(group).put(key,data,expireSecond);
    }

    @Override
    public int count(String group) {
        Map map = memExpiryGroupMap.get(group);
        if(map == null){
            return 0;
        }
        return map.size();
    }

    @Override
    public boolean exist(String key) {
        return exist2(key,SHARING_GROUP);
    }

    @Override
    public byte[] get(String key) {
        return get2(key,SHARING_GROUP);
    }

    @Override
    public byte[] get2(String key, String group) {
        return getMemExpiryMap(group).get(key);
    }

    @Override
    public void changeKey(String oldKey, String newKey) {
        changeKey3(oldKey,newKey,SHARING_GROUP);
    }

    @Override
    public void changeKey3(String oldKey, String newKey, String group) {
        getMemExpiryMap(group).changeKey(oldKey,newKey);
    }

    @Override
    public void remove(String key) {
        remove2(key,SHARING_GROUP);
    }

    @Override
    public void remove2(String key, String group) {
        getMemExpiryMap(group).remove(key);
    }

    @Override
    public void removeBatch(List<String> keys) {
        removeBatch2(keys,SHARING_GROUP);
    }

    @Override
    public void removeBatch2(List<String> keys, String group) {
        if(keys == null || keys.isEmpty()){
            return;
        }

        ExpiryMap<String, byte[]> map = getMemExpiryMap(group);
        if(keys instanceof RandomAccess) {
            int size = keys.size();
            for (int i=0; i<size; i++){
                String key = keys.get(i);
                map.remove(key);
            }
        }else {
            for (String key : keys) {
                map.remove(key);
            }
        }
    }

    private ExpiryMap<String, byte[]> getMemExpiryMap(String group) {
        ExpiryMap<String,byte[]> memExpiryMap = memExpiryGroupMap.get(group);
        if(memExpiryMap == null){
            synchronized (memExpiryGroupMap) {
                memExpiryMap = memExpiryGroupMap.get(group);
                if(memExpiryMap == null) {
                    memExpiryMap = new ExpiryMap<>(-1);
                    memExpiryGroupMap.put(group, memExpiryMap);
                }
            }
        }
        return memExpiryMap;
    }

    /**
     * timed expiration Map will automatically expire and be deleted
     * common scenario: localCache
     */
    public class ExpiryMap <K, V> extends HashMap<K, V> {
        private final Object lock;
        private Map<K, Long> expiryMap;
        private long defaultExpiryTime;

        public ExpiryMap(long defaultExpiryTime){
            this(16, defaultExpiryTime);
        }

        public ExpiryMap(int initialCapacity, long defaultExpiryTime){
            super(initialCapacity);
            this.defaultExpiryTime = defaultExpiryTime < 0 ? -1 : defaultExpiryTime;
            this.expiryMap = new HashMap<K,Long>(initialCapacity);
            this.lock = new Object();
        }

        @Override
        public V put(K key, V value) {
            return put(key,value,defaultExpiryTime);
        }

        /**
         * @param key key
         * @param value value
         * @param expiryTime The key value pair is valid in milliseconds
         * @return old value
         */
        public V put(K key, V value, long expiryTime) {
            expiryMap.put(key, System.currentTimeMillis() + expiryTime);
            return super.put(key, value);
        }

        public void changeKey(K oldKey, K newKey) {
            Long expiry = expiryMap.remove(oldKey);
            //如果已经过期
            if(expiry == null || expiry - System.currentTimeMillis() <= 0){
                return;
            }
            put(newKey,super.remove(oldKey),expiry);
        }

        @Override
        public boolean containsKey(Object key) {
            return !checkExpiry( key) && super.containsKey(key);
        }

        @Override
        public int size() {
            checkExpiry();
            return super.size();
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public boolean containsValue(Object value) {
            Iterator<Entry<K, V>> iterator = super.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<K, V> entry = iterator.next();
                K key = entry.getKey();
                V cValue = entry.getValue();
                if (cValue != value || !value.equals(cValue)) {
                    continue;
                }

                if (!checkExpiry(key, false)) {
                    return true;
                }

                remove(iterator, key);
                return false;
            }

            return false;
        }

        private void remove(Iterator<Entry<K,V>> it, K key){
            expiryMap.remove(key);
            it.remove();
        }

        @Override
        public V remove(Object key) {
            expiryMap.remove(key);
            V v = super.remove(key);
            return v;
        }

        @Override
        public V get(Object key) {
            if (key == null) {
                return null;
            }
            if(checkExpiry(key)) {
                return null;
            }
            return super.get(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            for (Entry<? extends K, ? extends V> e : m.entrySet()) {
                expiryMap.put(e.getKey(), System.currentTimeMillis() + defaultExpiryTime);
            }
            super.putAll(m);
        }

        @Override
        public Collection<V> values() {
            checkExpiry();
            return super.values();
        }

        @Override
        public Set<K> keySet() {
            checkExpiry();
            return super.keySet();
        }

        @Override
        public Set<Entry<K,V>> entrySet() {
            synchronized (lock) {
                Set<Entry<K, V>> set = super.entrySet();
                Iterator<Entry<K, V>> iterator = set.iterator();
                while (iterator.hasNext()) {
                    Entry<K, V> entry = iterator.next();
                    K key = entry.getKey();
                    if (checkExpiry(key, false)) {
                        remove(iterator, key);
                    }
                }
                return set;
            }
        }

        public Long getExpiry(K key) {
            return expiryMap.get(key);
        }

        private void checkExpiry(){
            entrySet();
        }

        /**
         *
         * @Description: Is late
         * @param key True overdue
         * @param isRemoveSuper true Super delete
         * @return
         */
        private boolean checkExpiry(Object key, boolean isRemoveSuper){
            Long expiryTime = expiryMap.get(key);
            if(expiryTime == null) {
                return true;
            }

            long currentTime = System.currentTimeMillis();
            boolean disable = currentTime > expiryTime;

//        System.out.println( key + " expiryTime"+expiryTime);
//        System.out.println( key + " currentTime"+currentTime);

            if(disable){
                if(isRemoveSuper) {
                    remove(key);
                }
            }
            return disable;
        }

        private boolean checkExpiry(Object key){
            return checkExpiry(key,true);
        }
    }

}
