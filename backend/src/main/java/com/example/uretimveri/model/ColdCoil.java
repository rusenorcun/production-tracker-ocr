package com.example.uretimveri.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import jakarta.persistence.*;

@Entity
@Table(name = "cold_coil")
public class ColdCoil {

    @Id
    private Long productId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "load_cell")
    private Integer loadCell;

    @Column(name = "ir_piro")
    private Integer irPiro;

    @Column(name = "termokup")
    private Integer termokup;

    // Getter - Setter
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

    @Transient
    @JsonProperty("createdAt")
    public Timestamp getCreatedAt() {
        return (product != null) ? product.getCreatedAt() : null;
    }
}
