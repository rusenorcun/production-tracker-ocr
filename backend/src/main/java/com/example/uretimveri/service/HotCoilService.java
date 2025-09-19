package com.example.uretimveri.service;

import com.example.uretimveri.dto.HotCoilDTO;
import com.example.uretimveri.model.HotCoil;
import com.example.uretimveri.model.Product;
import com.example.uretimveri.repository.HotCoilRepository;
import com.example.uretimveri.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HotCoilService {

    private final HotCoilRepository hotCoilRepository;
    private final ProductRepository productRepository;

    public HotCoilService(HotCoilRepository hotCoilRepository,
                          ProductRepository productRepository) {
        this.hotCoilRepository = hotCoilRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public HotCoil createWithNewProduct(HotCoil payload) {
        Product p = new Product();
        p.setProductType("hot_coil");            // ürün tipi tetikleyiciye de ipucu olur
        p = productRepository.save(p);           // <-- TRIGGER hot_coil satırını açar

        Long id = p.getProductId();              // getId() ise onu kullan
        HotCoil existing = hotCoilRepository.findById(id).orElse(null);

        if (existing == null) {
            // Emniyet: trigger kapalıysa ilk kez biz açalım
            payload.setProduct(p);               // @MapsId için şart
            return hotCoilRepository.save(payload);
        }

        // Null olmayanları taşı (INSERT değil UPDATE)
        if (payload.getLazerDistance() != null) existing.setLazerDistance(payload.getLazerDistance());
        if (payload.getIrPiro() != null)        existing.setIrPiro(payload.getIrPiro());
        if (payload.getPressureValue() != null) existing.setPressureValue(payload.getPressureValue());

        return hotCoilRepository.save(existing);
    }

    @Transactional
    public HotCoil createForExistingProduct(Long productId, HotCoil payload) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product bulunamadı: " + productId));

        HotCoil existing = hotCoilRepository.findById(productId).orElse(null);

        if (existing == null) {
            payload.setProduct(p);
            return hotCoilRepository.save(payload);   // ilk kez aç
        }

        if (payload.getLazerDistance() != null) existing.setLazerDistance(payload.getLazerDistance());
        if (payload.getIrPiro() != null)        existing.setIrPiro(payload.getIrPiro());
        if (payload.getPressureValue() != null) existing.setPressureValue(payload.getPressureValue());

        return hotCoilRepository.save(existing);
    }

    @Transactional
    public HotCoil update(Long id, HotCoil updated) {
        HotCoil existing = hotCoilRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("HotCoil bulunamadı: " + id));

        // Null gelmeyen alanları patch et
        if (updated.getLazerDistance() != null) existing.setLazerDistance(updated.getLazerDistance());
        if (updated.getIrPiro() != null)        existing.setIrPiro(updated.getIrPiro());
        if (updated.getPressureValue() != null) existing.setPressureValue(updated.getPressureValue());

        return hotCoilRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!hotCoilRepository.existsById(id)) {
            throw new EntityNotFoundException("HotCoil bulunamadı: " + id);
        }
        hotCoilRepository.deleteById(id);
        if(!hotCoilRepository.existsById(id)) {
            productRepository.deleteById(id);
        }
    }

    @Transactional(readOnly = true)
    public List<HotCoilDTO> getAllHotCoilsAsDTO() {
        return hotCoilRepository.findAll().stream()
                .map(hc -> new HotCoilDTO(
                        hc.getId(),
                        hc.getLazerDistance(),
                        hc.getIrPiro(),
                        hc.getPressureValue()
                ))
                .toList();
    }
}
