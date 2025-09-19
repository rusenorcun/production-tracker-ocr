package com.example.uretimveri.service;

import com.example.uretimveri.dto.ColdCoilDTO;
import com.example.uretimveri.model.ColdCoil;
import com.example.uretimveri.model.Product;
import com.example.uretimveri.repository.ColdCoilRepository;
import com.example.uretimveri.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ColdCoilService {

    private final ColdCoilRepository coldCoilRepository;
    private final ProductRepository productRepository;

    public ColdCoilService(ColdCoilRepository coldCoilRepository,
                           ProductRepository productRepository) {
        this.coldCoilRepository = coldCoilRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public ColdCoil createWithNewProduct(ColdCoil payload) {
        Product p = new Product();
        p.setProductType("cold_coil");
        p = productRepository.save(p);                  // TRIGGER cold_coil satırını açar

        Long id = p.getProductId();
        ColdCoil existing = coldCoilRepository.findById(id).orElse(null);

        if (existing == null) {
            payload.setProduct(p);
            return coldCoilRepository.save(payload);
        }

        if (payload.getLoadCell() != null) existing.setLoadCell(payload.getLoadCell());
        if (payload.getIrPiro() != null)   existing.setIrPiro(payload.getIrPiro());
        if (payload.getTermokup() != null) existing.setTermokup(payload.getTermokup());

        return coldCoilRepository.save(existing);
    }

    @Transactional
    public ColdCoil createForExistingProduct(Long productId, ColdCoil payload) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product bulunamadı: " + productId));

        ColdCoil existing = coldCoilRepository.findById(productId).orElse(null);

        if (existing == null) {
            payload.setProduct(p);
            return coldCoilRepository.save(payload);
        }

        if (payload.getLoadCell() != null) existing.setLoadCell(payload.getLoadCell());
        if (payload.getIrPiro() != null)   existing.setIrPiro(payload.getIrPiro());
        if (payload.getTermokup() != null) existing.setTermokup(payload.getTermokup());

        return coldCoilRepository.save(existing);
    }

    @Transactional
    public ColdCoil update(Long id, ColdCoil updated) {
        ColdCoil existing = coldCoilRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ColdCoil bulunamadı: " + id));

        if (updated.getLoadCell() != null)  existing.setLoadCell(updated.getLoadCell());
        if (updated.getIrPiro() != null)    existing.setIrPiro(updated.getIrPiro());
        if (updated.getTermokup() != null)  existing.setTermokup(updated.getTermokup());

        return coldCoilRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!coldCoilRepository.existsById(id)) {
            throw new EntityNotFoundException("ColdCoil bulunamadı: " + id);
        }
        coldCoilRepository.deleteById(id);
        if(!coldCoilRepository.existsById(id)) {
            productRepository.deleteById(id);
        }
    }

    @Transactional(readOnly = true)
    public List<ColdCoilDTO> getAllDTO() {
        return coldCoilRepository.findAll().stream()
                .map(c -> new ColdCoilDTO(
                        c.getProductId(),       // c.getProduct().getProductId() yerine direkt ID
                        c.getLoadCell(),
                        c.getIrPiro(),
                        c.getTermokup()
                ))
                .toList();
    }
}
