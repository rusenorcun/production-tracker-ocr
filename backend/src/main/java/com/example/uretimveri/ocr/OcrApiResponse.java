package com.example.uretimveri.ocr;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class OcrApiResponse {
    @JsonProperty("job_id")
    private String jobId;
    @JsonProperty("source_image")
    private String sourceImage;
    @JsonProperty("results_csv")
    private String resultsCsv;
    @JsonProperty("saved_image")
    private String savedImage;
    private Integer count;
    private List<OcrItem> items;

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getSourceImage() { return sourceImage; }
    public void setSourceImage(String sourceImage) { this.sourceImage = sourceImage; }
    public String getResultsCsv() { return resultsCsv; }
    public void setResultsCsv(String resultsCsv) { this.resultsCsv = resultsCsv; }
    public String getSavedImage() { return savedImage; }
    public void setSavedImage(String savedImage) { this.savedImage = savedImage; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public List<OcrItem> getItems() { return items; }
    public void setItems(List<OcrItem> items) { this.items = items; }
}
