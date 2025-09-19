package com.example.uretimveri.service;

import java.util.Objects;
import java.util.stream.Collectors;
import com.example.uretimveri.model.Product;
import com.example.uretimveri.repository.ProductRepository;
// Alt tablo repolarını kendi isimlerine göre import et
import com.example.uretimveri.repository.HotCoilRepository;
import com.example.uretimveri.repository.ColdCoilRepository;
import com.example.uretimveri.repository.PlatesRepository;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repo;


    public ProductService(ProductRepository repo,
                          HotCoilRepository hotCoilRepo,
                          ColdCoilRepository coldCoilRepo,
                          PlatesRepository platesRepo) {
        this.repo = repo;
    }


    @Transactional(readOnly = true)
    public List<Product> getAll() {
        return repo.findAll();
    }

    @Transactional
    public Product save(Product product) {
        // Yeni kayıt veya full update için kullanılabilir
        return repo.save(product);
    }

    @Transactional
    public Product update(Long id, Product updated) {
        return repo.findById(id).map(p -> {
            // Null gelmeyen alanları güncelle (güvenli kısmi update)
            if (updated.getProvider() != null)    p.setProvider(updated.getProvider());
            if (updated.getProductType() != null) p.setProductType(updated.getProductType());
            if (updated.getMaterial() != null)    p.setMaterial(updated.getMaterial());
            if (updated.getStatus() != null)      p.setStatus(updated.getStatus());
            return repo.save(p);
        }).orElseThrow(() -> new EntityNotFoundException("Product bulunamadı: " + id));
    }

    @Transactional
    public void delete(Long id) {
        // Önce var mı kontrol et
        if (!repo.existsById(id)) {
            throw new EntityNotFoundException("Product bulunamadı: " + id);
        }

        // Veritabanında ON DELETE CASCADE tanımlı olduğu varsayılarak,
        // alt tabloları manuel silmeye gerek yoktur. JPA bunu yönetir.
        // Eğer cascade yoksa, bu yaklaşım DataIntegrityViolationException fırlatacaktır.
        try {
            repo.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            // İlişkiler yüzünden silinemiyorsa anlaşılır mesaj
            throw new DataIntegrityViolationException("Kayıt ilişkili veriler nedeniyle silinemedi (FK).", ex);
        }
    }
    @Transactional
    public void bulkDeleteProducts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;

        // null ve tekrarları temizle
        List<Long> distinct = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (distinct.isEmpty()) return;

        // ÖNEMLİ: ON DELETE CASCADE aktif olduğu için alt tablolar otomatik silinir.
        // Toplu ve hızlı silme:
        repo.deleteAllByIdInBatch(distinct);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<com.example.uretimveri.model.Product> getRecentProducts(int limit) {
        int size = Math.max(1, Math.min(limit, 50));        // 1..50 aralığına sabitle
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); // Entity alan adı
        return repo  // sende repo değişkeninin adı farklıysa onu kullan
                .findAll(PageRequest.of(0, size, sort))
                .getContent();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<com.example.uretimveri.model.Product> getRecentProducts() {
        return getRecentProducts(5);
    }
}
