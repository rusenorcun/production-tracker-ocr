package com.example.uretimveri.dto;

public class PlatesDTO {
    private Long productId;
    private Integer speedValue;
    private Integer pressureValue;
    private Long lvdt;

    public PlatesDTO() {}

    public PlatesDTO(Long productId, Integer speedValue, Integer pressureValue, Long lvdt) {
        this.productId = productId;
        this.speedValue = speedValue;
        this.pressureValue = pressureValue;
        this.lvdt = lvdt;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getSpeedValue() {
        return speedValue;
    }

    public void setSpeedValue(Integer speedValue) {
        this.speedValue = speedValue;
    }

    public Integer getPressureValue() {
        return pressureValue;
    }

    public void setPressureValue(Integer pressureValue) {
        this.pressureValue = pressureValue;
    }

    public Long getLvdt() {
        return lvdt;
    }

    public void setLvdt(Long lvdt) {
        this.lvdt = lvdt;
    }
}
