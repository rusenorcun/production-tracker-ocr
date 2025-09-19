package com.example.uretimveri.ocr;

import java.util.List;

public class RecognizeUiResponse {
    private String jobId;
    private Integer detectedCount;
    private String sourceImage;
    private List<RecognizeUiItem> items;

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public Integer getDetectedCount() { return detectedCount; }
    public void setDetectedCount(Integer detectedCount) { this.detectedCount = detectedCount; }
    public String getSourceImage() { return sourceImage; }
    public void setSourceImage(String sourceImage) { this.sourceImage = sourceImage; }
    public List<RecognizeUiItem> getItems() { return items; }
    public void setItems(List<RecognizeUiItem> items) { this.items = items; }
}
