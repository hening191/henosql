package com.he.service;

import com.he.common.comment.Comment;

import java.util.concurrent.ConcurrentHashMap;

@Comment("henosql缓存信息")
public class HNSCache {

    private static HNSCache instance = null;

    private HNSCache(){}

    public static HNSCache getInstance(){
        if(instance == null){instance = new HNSCache();}
        return instance;
    }

    private final ConcurrentHashMap<Class<?>,String> aliasMap = new ConcurrentHashMap<>();

    void putAliasMap(Class<?> clz, String str){
        aliasMap.put(clz , str);
    }

    @Comment("获取数据表对应类的别名")
    public String getAliasMap(Class<?> clz){
        return aliasMap.get(clz);
    }
}
