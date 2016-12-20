package com.shzisg.mybatis.mapper.page;

import java.util.List;

public class Page<E> {

    private long total;
    private int page;
    private int size;
    private List<E> content;
    
    public Page() {
        
    }
    
    public Page(List<E> content, long total, int page, int size) {
        this.content = content;
        this.total = total;
        this.page = page;
        this.size = size;
    }
    
    public static <Content> Page<Content> from(List<Content> content, PageRequest request) {
        return new Page<>(content, request.context, request.getPage(), request.getSize());
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

    public List<E> getContent() {
        return this.content;
    }

    public void setContent(List<E> content) {
        this.content = content;
    }
}
