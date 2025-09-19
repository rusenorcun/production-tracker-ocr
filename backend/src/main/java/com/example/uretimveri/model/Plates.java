package com.example.uretimveri.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import jakarta.persistence.*;

@Entity
@Table(name = "plates")
public class Plates {

    @Id
    private Long productId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "speed_value")
    private Integer speedValue;

    @Column(name = "pressure_value")
    private Integer pressureValue;

    @Column(name = "lvdt")
    private Long lvdt;

    // Getters and Setters
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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

    @Transient
    @JsonProperty("createdAt")
    public Timestamp getCreatedAt() {
        return (product != null) ? product.getCreatedAt() : null;
    }
}
