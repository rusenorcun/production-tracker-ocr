// File: src/main/java/com/example/uretimveri/dto/ProductDTO.java
package com.example.uretimveri.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Tüm ürün tipleri için tek DTO.
 * - productType: "hot_coil" | "cold_coil" | "plates"
 * - productTypeLabel: TR karşılık ("Sıcak Bobin" | "Soğuk Bobin" | "Slab") — otomatik üretilir
 * Index, DataTables vb. her yerde aynı DTO’yu döndürerek tek noktadan yönetim sağlar.
 */
public class ProductDTO {

    private Long productId;
    private String productType;   // "hot_coil" | "cold_coil" | "plates"
    private String provider;
    private String material;
    private LocalDateTime createdAt;

    public ProductDTO() {}

    public ProductDTO(Long productId,
                      String productType,
                      String provider,
                      String material,
                      LocalDateTime createdAt) {
        this.productId = productId;
        this.productType = productType;
        this.provider = provider;
        this.material = material;
        this.createdAt = createdAt;
    }

    // ---- Getters / Setters ----
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * TR etiket — JSON'a "productTypeLabel" alanı olarak çıkar.
     * Thymeleaf’te de doğrudan ${e.productTypeLabel} ile kullanabilirsin.
     */
    @JsonProperty("productTypeLabel")
    public String getProductTypeLabel() {
        return labelOf(productType);
    }

    // ---- Yardımcılar ----
    private static String labelOf(String code) {
        if (code == null) return "";
        return switch (code.toLowerCase()) {
            case "hot_coil"  -> "Sıcak Bobin";
            case "cold_coil" -> "Soğuk Bobin";
            case "plates"    -> "Slab";
            default          -> code; // bilinmeyen durumlarda orijinal kodu göster
        };
    }

    /**
     * Basit kurucu — farklı entity’lerden kolayca üretmek için.
     * Örn: ProductDTO.of(p.getId(), "hot_coil", p.getProvider(), p.getMaterial(), p.getCreatedAt())
     */
    public static ProductDTO of(Long productId,
                                String productType,
                                String provider,
                                String material,
                                LocalDateTime createdAt) {
        return new ProductDTO(productId, productType, provider, material, createdAt);
    }
}
