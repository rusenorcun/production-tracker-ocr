package com.example.uretimveri.controller;

import com.example.uretimveri.dto.BulkDeleteRequest;
import com.example.uretimveri.model.HotCoil;
import com.example.uretimveri.repository.HotCoilRepository;
import com.example.uretimveri.service.HotCoilService;
import com.example.uretimveri.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hot_coil")
public class HotCoilController {

    private final HotCoilService hotCoilService;
    private final HotCoilRepository hotCoilRepository;
    private final ProductService productService;

    public HotCoilController(HotCoilService hotCoilService,
                             HotCoilRepository hotCoilRepository,
                             ProductService productService) {
        this.hotCoilService = hotCoilService;
        this.hotCoilRepository = hotCoilRepository;
        this.productService = productService;
    }

    // DTO ile dön: product/productId karmaşası çözülür
    @GetMapping("/all")
    public List<HotCoil> all() {
        return hotCoilRepository.findAllWithProduct(); // createdAt artık dolu gelecek
    }

    // Şu anki UI: yeni Product oluştur + HotCoil bağla
    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody HotCoil hotCoil) {
        try {
            // Not: HotCoilService içinde createWithNewProduct(...) Product'ı oluşturup
            // hotCoil.setProduct(product) yapmalıdır. ID elle set edilmez.
            HotCoil saved = hotCoilService.createWithNewProduct(hotCoil);
            return ResponseEntity.ok(saved);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // 409
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Kayıt sırasında hata oluştu: " + e.getMessage());
        }
    }
    // Mevcut Product'a 1-1 HotCoil eklemek için (ileride gerekirse)
    @PostMapping("/add/{productId}")
    public ResponseEntity<?> addForExisting(@PathVariable Long productId, @RequestBody HotCoil hotCoil) {
        try {
            HotCoil saved = hotCoilService.createForExistingProduct(productId, hotCoil);
            return ResponseEntity.ok(saved);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); // 404
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // 409
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Kayıt hatası");
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody HotCoil updated) {
        try {
            return ResponseEntity.ok(hotCoilService.update(id, updated));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("HotCoil not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Güncelleme hatası");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            hotCoilService.delete(id);
            return ResponseEntity.noContent().build(); // 204
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();   // 404
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).build();  // 409 - ilişkili veri engeli
        }
    }

    @PostMapping("/bulk-delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> bulkDelete(@RequestBody BulkDeleteRequest request) {
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        // Mevcut ProductService metodunu çağırıyoruz.
        productService.bulkDeleteProducts(request.getIds());
        return ResponseEntity.noContent().build();
    }
}
