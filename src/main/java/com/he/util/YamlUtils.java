package com.he.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.io.IOException;
import java.util.*;

@Slf4j
public class YamlUtils {

    static Resource[] ymlResources = {new ClassPathResource("application.yml")};
    static Map<String, Object> map = new HashMap<>(200);

    /**
     * @return
     * @throws IOException
     */
    public static Map<String, Object> yamlHandler(String... yamlFile) throws IOException {
        //返回的结果
        Map<String, Object> result = new LinkedHashMap<>();
        Yaml yaml = new Yaml();
        //多个文件处理
        Resource[] resources = new Resource[yamlFile.length];
        for(int i = 0 ; i < yamlFile.length ; i ++){
            resources[i] = new ClassPathResource(yamlFile[i]);
        }
        Iterator<Resource> iterator = Arrays.stream( resources ).iterator();
        log.info(iterator.toString());
        while (iterator.hasNext()) {
            Resource resource = iterator.next();
            UnicodeReader reader = new UnicodeReader(resource.getInputStream());
            Object object = yaml.load(reader);
            //这里只是简单处理，需要多个方式可以自己添加
            if (object instanceof Map) {
                Map map = (Map) object;
                buildFlattenedMap(result, map, null);
            }
            reader.close();
        }
        return result;
    }

    /**
     * 这部分代码来至springboot源码部分对yaml的解析
     * YamlProcessor.java buildFlattenedMap方法
     *
     * @param result
     * @param source
     * @param path
     */
    private static void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, @Nullable String path) {
        //循环读取原数据
        source.forEach((key, value) -> {
            //如果存在路径进行拼接
            if (StringUtils.hasText(path)) {
                if (key.startsWith("[")) {
                    key = path + key;
                } else {
                    key = path + '.' + key;
                }
            }
            //数据类型匹配
            if (value instanceof String) {
                result.put(key, value);
            } else if (value instanceof Map) {
                //如果是map,就继续读取
                Map<String, Object> map = (Map) value;
                buildFlattenedMap(result, map, key);
            } else if (value instanceof Collection) {
                Collection<Object> collection = (Collection) value;
                if (collection.isEmpty()) {
                    result.put(key, "");
                } else {
                    int count = 0;
                    Iterator var7 = collection.iterator();

                    while (var7.hasNext()) {
                        Object object = var7.next();
                        buildFlattenedMap(result, Collections.singletonMap("[" + count++ + "]", object), key);
                    }
                }
            } else {
                result.put(key, value != null ? value : "");
            }
        });
    }

    public static String getString(String key) throws IOException {
        // 每次都重新加载
        map = YamlUtils.yamlHandler("application.yml");
        return String.valueOf(map.get(key));
    }

    public static String getStringDefaultValue(String key, String defaultValue) throws IOException {
        // 如果map中有key就使用map中的
        if (map.get(key) != null) {
            return (String) map.get(key);
        } else {
            // 没有就重新加载
            map = YamlUtils.yamlHandler("application.yml");
            // key不存在就使用默认值
            map.putIfAbsent(key, defaultValue);
            return String.valueOf(map.get(key));
        }
    }
}