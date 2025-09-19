package com.example.uretimveri.dto;

public class HotCoilDTO {
    private Long productId;
    private Double lazerDistance;
    private Double irPiro;
    private Double pressureValue;



    public HotCoilDTO(Long productId, Double lazerDistance, Double irPiro, Double pressureValue) {
        this.productId = productId;
        this.lazerDistance = lazerDistance;
        this.irPiro = irPiro;
        this.pressureValue = pressureValue;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Double getLazerDistance() {
        return lazerDistance;
    }

    public void setLazerDistance(Double lazerDistance) {
        this.lazerDistance = lazerDistance;
    }

    public Double getIrPiro() {
        return irPiro;
    }

    public void setIrPiro(Double irPiro) {
        this.irPiro = irPiro;
    }

    public Double getPressureValue() {
        return pressureValue;
    }

    public void setPressureValue(Double pressureValue) {
        this.pressureValue = pressureValue;
    }
}
