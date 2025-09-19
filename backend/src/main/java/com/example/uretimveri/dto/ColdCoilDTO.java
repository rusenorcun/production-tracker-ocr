package com.example.uretimveri.dto;

public class ColdCoilDTO {
    private Long productId;
    private Integer loadCell;
    private Integer irPiro;
    private Integer termokup;

    public ColdCoilDTO() {}

    public ColdCoilDTO(Long productId, Integer loadCell, Integer irPiro, Integer termokup) {
        this.productId = productId;
        this.loadCell = loadCell;
        this.irPiro = irPiro;
        this.termokup = termokup;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getLoadCell() {
        return loadCell;
    }

    public void setLoadCell(Integer loadCell) {
        this.loadCell = loadCell;
    }

    public Integer getIrPiro() {
        return irPiro;
    }

    public void setIrPiro(Integer irPiro) {
        this.irPiro = irPiro;
    }

    public Integer getTermokup() {
        return termokup;
    }

    public void setTermokup(Integer termokup) {
        this.termokup = termokup;
    }
}
