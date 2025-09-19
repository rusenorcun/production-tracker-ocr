package com.example.uretimveri.service;

import com.example.uretimveri.model.Plates;
import com.example.uretimveri.model.Product;
import com.example.uretimveri.repository.PlatesRepository;
import com.example.uretimveri.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class Img2DataPlatesService {

    private final ProductRepository productRepo;
    private final PlatesRepository platesRepo;

    private static final String PRODUCT_TYPE_PLATES = "plates";
    private static final String PROVIDER_IMG2DATA   = "İsdemir-Server";
    private static final String STATUS_IMAGE        = "IMAGE";


    @Transactional
    public Map<Long, Long> saveAll(List<Long> lvdtValues) {
        if (lvdtValues == null || lvdtValues.isEmpty()) return Collections.emptyMap();

        Map<Long, Long> created = new LinkedHashMap<>();
        for (Long lvdt : lvdtValues) {
            if (lvdt == null) continue;

  
            Product p = new Product();
            p.setProductType(PRODUCT_TYPE_PLATES);
            p.setProvider(PROVIDER_IMG2DATA);
            p.setStatus(STATUS_IMAGE);
            p = productRepo.save(p); // Long

            Long pid = p.getProductId(); // Long

            Product finalP = p;
            Plates child = platesRepo.findById(pid).orElseGet(() -> {
                Plates pl = new Plates();
                pl.setProductId(pid);  // Plates.productId = Long
                pl.setProduct(finalP); // finalP 
                return pl;
            });

            // LVDT 
            child.setLvdt(lvdt);
            platesRepo.save(child);

            created.put(pid, lvdt);
        }
        return created;
    }
}
