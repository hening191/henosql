package com.he.entity;

import lombok.Data;

@Data
public class Carrier {
    Class<?> leftTable;
    Class<?> rightTable;
    String leftKey;
    String rightKey;

    public Carrier(){}

    public Carrier(Class<?> leftTable, Class<?> rightTable, String leftKey, String rightKey) {
        this.leftTable = leftTable;
        this.rightTable = rightTable;
        this.leftKey = leftKey;
        this.rightKey = rightKey;
    }
}
