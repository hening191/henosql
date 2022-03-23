package com.he.common.function;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class FieldFunction {

    public static Field[] getAllDeclaredField(Class<?> clz){
        Field[] me = clz.getDeclaredFields();
        if((clz = clz.getSuperclass())!=null){
            Field[] sup = getAllDeclaredField(clz);
            Field[] fields = new Field[me.length + sup.length];
            System.arraycopy(me, 0, fields, 0, me.length);
            System.arraycopy(sup, 0, fields, me.length, sup.length);
            return fields;
        }else{
            return me;
        }
    }

    private static Set<String> getAllDeclaredFieldName(Class<?> clz){
        return Arrays.asList( getAllDeclaredField(clz) ).stream().map(Field::getName).collect(Collectors.toSet());
    }

}
