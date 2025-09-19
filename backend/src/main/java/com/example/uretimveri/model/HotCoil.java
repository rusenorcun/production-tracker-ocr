package com.example.uretimveri.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import jakarta.persistence.*;

@Entity
public class HotCoil {

    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    private Double lazerDistance;
    private Double irPiro;
    private Double pressureValue;

    @Transient
    private String productType;

    @Transient
    @JsonProperty("createdAt")
    public Timestamp getCreatedAt() {
        return (product != null) ? product.getCreatedAt() : null;
    }

    // getter-setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }
}
