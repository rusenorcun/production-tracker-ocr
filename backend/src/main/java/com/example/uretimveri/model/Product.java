package com.example.uretimveri.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(name = "provider")
    private String provider;

    @Column(name = "product_type")
    private String productType;

    @Column(name = "material")
    private String material;

    @Column(name = "status")
    private String status;

    // DB default CURRENT_TIMESTAMP kullanıyorsan:
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private Timestamp createdAt;

    // ---- Getters / Setters ----
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
