package com.ieps.pojo;

import java.io.Serializable;

/**
 * Created by ljw
 */
public class Echarts implements Serializable {
    private String name;
    private Integer num;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }
}

