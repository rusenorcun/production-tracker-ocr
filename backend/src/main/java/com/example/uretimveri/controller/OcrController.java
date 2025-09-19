package com.example.uretimveri.controller;

import com.example.uretimveri.ocr.*;
import com.example.uretimveri.service.Img2DataPlatesService;
import com.example.uretimveri.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/slabs")
@Slf4j
public class OcrController {

    private final OcrService ocrService;
    private final Img2DataPlatesService platesService;

    private static final Pattern NOT_DIGIT = Pattern.compile("[^0-9]");

    // 1) SADECE TESPIT — DB YOK
    @PostMapping(value="/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecognizeUiResponse recognize(@RequestPart("file") MultipartFile file) {
        OcrApiResponse py = ocrService.recognize(file);

        List<RecognizeUiItem> items = new ArrayList<>();
        if (py != null && py.getItems() != null) {
            for (OcrItem it : py.getItems()) {
                if (it == null || it.getText() == null) continue;
                RecognizeUiItem ui = new RecognizeUiItem();
                ui.setImage(it.getImage());
                ui.setRoiIndex(it.getRoiIndex());
                ui.setText(it.getText());
                ui.setLvdt(selectLvdt(it.getText())); // 10–18 hane öncelik
                items.add(ui);
            }
        }

        RecognizeUiResponse out = new RecognizeUiResponse();
        out.setJobId(py != null ? py.getJobId() : null);
        out.setSourceImage(py != null ? py.getSourceImage() : null);
        out.setDetectedCount(py != null && py.getCount() != null ? py.getCount() : items.size());
        out.setItems(items);
        return out;
    }

    // 2) SEÇILENLERI KAYDET — DB YAZ
    @PostMapping(value="/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SaveResponse> save(@RequestBody SaveRequest req) {
        if (req == null || req.getLvdts() == null || req.getLvdts().isEmpty()) {
            SaveResponse r = new SaveResponse();
            r.setSavedCount(0); r.setCreated(Map.of());
            return ResponseEntity.ok(r);
        }
        // tekilleştir (aynı LVDT’yi iki kez açmayalım)
        List<Long> uniq = req.getLvdts().stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, Long> created = platesService.saveAll(uniq);

        SaveResponse r = new SaveResponse();
        r.setSavedCount(created.size());
        r.setCreated(created);
        return ResponseEntity.ok(r);
    }

    // --- yardımcı: metinden LVDT seçimi ---
    private Long selectLvdt(String text) {
        if (text == null) return null;
        String[] parts = text.trim().split("\\|");
        List<String> nums = new ArrayList<>();
        for (String p : parts) {
            String only = NOT_DIGIT.matcher(p == null ? "" : p).replaceAll("");
            if (!only.isEmpty()) nums.add(only);
        }
        if (nums.isEmpty()) return null;

        Optional<String> longLike = nums.stream()
                .filter(s -> s.length() >= 10 && s.length() <= 18)
                .findFirst();

        String chosen = longLike.orElse(nums.get(nums.size() - 1));
        try { return Long.parseLong(chosen); }
        catch (NumberFormatException e) { return null; }
    }
    
}
