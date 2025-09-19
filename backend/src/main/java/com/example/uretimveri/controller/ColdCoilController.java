package com.example.uretimveri.controller;

import com.example.uretimveri.model.ColdCoil;
import com.example.uretimveri.repository.ColdCoilRepository;
import com.example.uretimveri.service.ColdCoilService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cold_coil")
public class ColdCoilController {

    private final ColdCoilService service;
    private final ColdCoilRepository coldCoilRepository;

    public ColdCoilController(ColdCoilService service, ColdCoilRepository coldCoilRepository) {
        this.service = service;
        this.coldCoilRepository = coldCoilRepository;
    }

    @GetMapping("/all")
    public List<ColdCoil> all() {
        return coldCoilRepository.findAllWithProduct();
    }

    // Şu anki UI akışı: yeni Product oluştur + ColdCoil bağla
    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody ColdCoil coldCoil) {
        try {
            return ResponseEntity.ok(service.createWithNewProduct(coldCoil));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // 409
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Kayıt sırasında hata oluştu: " + e.getMessage());
        }
    }

    // İhtiyaç olursa: var olan Product'a 1-1 ColdCoil ekle
    @PostMapping("/add/{productId}")
    public ResponseEntity<?> addForExisting(@PathVariable Long productId, @RequestBody ColdCoil coldCoil) {
        try {
            return ResponseEntity.ok(service.createForExistingProduct(productId, coldCoil));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); // 404
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // 409
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Kayıt hatası");
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ColdCoil updated) {
        try {
            return ResponseEntity.ok(service.update(id, updated));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ColdCoil not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Güncelleme hatası");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build(); // 204
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();   // 404
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).build();  // 409 - ilişkili veri engeli
        }
    }
}
