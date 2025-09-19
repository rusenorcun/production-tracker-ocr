package com.example.uretimveri.dto;

import java.util.List;

public class BulkDeleteRequest {
    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}