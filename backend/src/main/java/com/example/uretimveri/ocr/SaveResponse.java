package com.example.uretimveri.ocr;

import java.util.Map;

public class SaveResponse {
    private int savedCount;
    private Map<Long, Long> created; // product_id -> lvdt

    public int getSavedCount() { return savedCount; }
    public void setSavedCount(int savedCount) { this.savedCount = savedCount; }
    public Map<Long, Long> getCreated() { return created; }
    public void setCreated(Map<Long, Long> created) { this.created = created; }
}
