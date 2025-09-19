package com.example.uretimveri.controller;

import com.example.uretimveri.model.Product;
import com.example.uretimveri.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.uretimveri.dto.BulkDeleteRequest;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/all")
    public List<Product> getAll() {
        return service.getAll();
    }

    @PostMapping("/add")
    public Product add(@RequestBody Product product) {
        return service.save(product);
    }

    @PutMapping("/update/{id}")
    public Product update(@PathVariable Long id, @RequestBody Product updated) {
        return service.update(id, updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build(); // 204
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();   // 404
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).build();  // 409 - FK/ilişki engeli
        }
    }
    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDelete(@RequestBody BulkDeleteRequest req) {
        if (req == null || req.getIds() == null || req.getIds().isEmpty()) {
            return ResponseEntity.badRequest().build(); // 400
        }
        service.bulkDeleteProducts(req.getIds());
        return ResponseEntity.noContent().build();      // 204
    }

}
