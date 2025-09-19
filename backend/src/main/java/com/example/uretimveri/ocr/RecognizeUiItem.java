package com.example.uretimveri.ocr;

public class RecognizeUiItem {
    private Integer roiIndex;
    private String text;     // Python'un ham metni
    private Long lvdt;       // seçilmiş LVDT (10–18 haneli varsa o, yoksa son sayı)
    private String image;

    public Integer getRoiIndex() { return roiIndex; }
    public void setRoiIndex(Integer roiIndex) { this.roiIndex = roiIndex; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Long getLvdt() { return lvdt; }
    public void setLvdt(Long lvdt) { this.lvdt = lvdt; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
