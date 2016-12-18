package com.shzisg.mybatis.mapper.page;

import java.util.List;

public class Page<E> {

    private long total;
    private int page;
    private int size;
    private String orderBy;
    private boolean asc;
    private List<E> content;
    
    public Page() {
        
    }
    
    public Page(List<E> content, long total, int page, int size) {
        this.content = content;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
    
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public boolean isAsc() {
        return asc;
    }

    public void setAsc(boolean asc) {
        this.asc = asc;
    }

    public List<E> getContent() {
        return this.content;
    }

    public void setContent(List<E> content) {
        this.content = content;
    }
}
