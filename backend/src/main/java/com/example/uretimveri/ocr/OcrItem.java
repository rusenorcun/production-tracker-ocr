package com.example.uretimveri.ocr;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OcrItem {
    private String image;
    @JsonProperty("roi_index")
    private Integer roiIndex;
    private String text;

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Integer getRoiIndex() { return roiIndex; }
    public void setRoiIndex(Integer roiIndex) { this.roiIndex = roiIndex; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
