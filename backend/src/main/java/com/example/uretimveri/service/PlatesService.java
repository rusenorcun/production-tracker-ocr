package com.example.uretimveri.service;

import com.example.uretimveri.dto.PlatesDTO;
import com.example.uretimveri.model.Plates;
import com.example.uretimveri.model.Product;
import com.example.uretimveri.repository.PlatesRepository;
import com.example.uretimveri.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlatesService {

    private final PlatesRepository platesRepository;
    private final ProductRepository productRepository;

    public PlatesService(PlatesRepository platesRepository,
                         ProductRepository productRepository) {
        this.platesRepository = platesRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Plates createWithNewProduct(Plates payload) {
        Product p = new Product();
        p.setProductType("plates");
        p = productRepository.save(p);                   // TRIGGER plates satırını açar

        Long id = p.getProductId();
        Plates existing = platesRepository.findById(id).orElse(null);

        if (existing == null) {
            payload.setProduct(p);
            return platesRepository.save(payload);
        }

        if (payload.getLvdt() != null)          existing.setLvdt(payload.getLvdt());
        if (payload.getPressureValue() != null) existing.setPressureValue(payload.getPressureValue());
        if (payload.getSpeedValue() != null)    existing.setSpeedValue(payload.getSpeedValue());

        return platesRepository.save(existing);
    }

    @Transactional
    public Plates createForExistingProduct(Long productId, Plates payload) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product bulunamadı: " + productId));

        Plates existing = platesRepository.findById(productId).orElse(null);

        if (existing == null) {
            payload.setProduct(p);
            return platesRepository.save(payload);
        }

        if (payload.getLvdt() != null)          existing.setLvdt(payload.getLvdt());
        if (payload.getPressureValue() != null) existing.setPressureValue(payload.getPressureValue());
        if (payload.getSpeedValue() != null)    existing.setSpeedValue(payload.getSpeedValue());

        return platesRepository.save(existing);
    }

    @Transactional
    public Plates update(Long id, Plates updated) {
        Plates existing = platesRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plates bulunamadı: " + id));

        if (updated.getSpeedValue() != null)    existing.setSpeedValue(updated.getSpeedValue());
        if (updated.getPressureValue() != null) existing.setPressureValue(updated.getPressureValue());
        if (updated.getLvdt() != null)          existing.setLvdt(updated.getLvdt());

        return platesRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!platesRepository.existsById(id)) {
            throw new EntityNotFoundException("Plates bulunamadı: " + id);
        }
        platesRepository.deleteById(id);
        if(!platesRepository.existsById(id)) {
            productRepository.deleteById(id);
        }
    }

    @Transactional(readOnly = true)
    public List<PlatesDTO> getAllDTO() {
        return platesRepository.findAll().stream()
                .map(p -> new PlatesDTO(
                        p.getProductId(),      // @MapsId varsa direkt ID
                        p.getSpeedValue(),
                        p.getPressureValue(),
                        p.getLvdt()
                ))
                .toList();
    }
}
