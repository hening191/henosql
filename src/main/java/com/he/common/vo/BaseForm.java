package com.he.common.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class BaseForm{

    Integer limit = 10;
    Integer pageNum = 1;
    Integer start = 0;

    String sTime;
    String eTime;


    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
        this.start = this.limit * (this.pageNum-1);
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
        this.start = this.limit * (this.pageNum-1);
    }

    public Integer getStart() {
        return start;
    }


    public String getSTime() {
        return sTime;
    }

    public void setSTime(String sTime) {
        this.sTime = sTime;
    }

    public String getETime() {
        return eTime;
    }

    public void setETime(String eTime) {
        this.eTime = eTime;
    }

}
