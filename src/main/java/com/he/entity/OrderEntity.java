package com.he.entity;

import com.he.common.annotation.comment.Comment;
import com.he.util.StringUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.function.Consumer;

@Comment("额外传入的排序")
public class OrderEntity {
    @Comment("排序字段")
    String key;
    @Comment("排序方式")
    OrderType type;

    public String getKey() {
        return key;
    }

    public OrderType getType() {
        return type;
    }

    public OrderEntity(String key, OrderType type) throws Exception {
        if(StringUtil.isEmpty(key) || type == null)throw new Exception("构建排序类时，排序字段key或排序类型type不可为空");
        this.key = key;
        this.type = type;
    }

}
