package com.example.uretimveri.controller;

import com.example.uretimveri.repository.ColdCoilRepository;
import com.example.uretimveri.repository.HotCoilRepository;
import com.example.uretimveri.repository.PlatesRepository;
import com.example.uretimveri.repository.ProductRepository;
import com.example.uretimveri.service.ProductService;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class MainController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final HotCoilRepository hotCoilRepository;
    private final ColdCoilRepository coldCoilRepository;
    private final PlatesRepository platesRepository;

    public MainController(ProductService productService,
                          ProductRepository productRepository,
                          HotCoilRepository hotCoilRepository,
                          ColdCoilRepository coldCoilRepository,
                          PlatesRepository platesRepository) {

        this.productService = productService;
        this.productRepository = productRepository;
        this.hotCoilRepository = hotCoilRepository;
        this.coldCoilRepository = coldCoilRepository;
        this.platesRepository = platesRepository;

    }

    /** Ana sayfa: Son eklenen ürünleri getirir (dinamik limit) */
    @GetMapping({"/", "/home", "/index"})
    public String home(@RequestParam(name = "limit", defaultValue = "5") int limit, Model model) {
        model.addAttribute("recentEntries", productService.getRecentProducts(limit));
        model.addAttribute("totalProducts", productRepository.count());
        model.addAttribute("hotCount", hotCoilRepository.count());
        model.addAttribute("coldCount", coldCoilRepository.count());
        model.addAttribute("platesCount", platesRepository.count());
        return "index"; // templates/index.html
    }

    /** JSON doğrulama için küçük endpoint (opsiyonel) */
    @GetMapping("/api/products/recent")
    @ResponseBody
    public Object recent(@RequestParam(name = "limit", defaultValue = "5") int limit) {
        return productService.getRecentProducts(limit);
    }

    /** Kimlik test (opsiyonel) */
    @GetMapping("/whoami")
    @ResponseBody
    public Map<String, Object> who(org.springframework.security.core.Authentication a) {
        return Map.of(
                "user", a.getName(),
                "roles", org.springframework.security.core.authority.AuthorityUtils.authorityListToSet(a.getAuthorities())
        );
    }
    @GetMapping("/products")
    public String products(){
        return "product_page"; // templates/product_page.html
    }
    @GetMapping("/img2data")
    public String img2dataPlatesPage() {
    return "img2data"; // templates/img2data-plates.html
    }

}
