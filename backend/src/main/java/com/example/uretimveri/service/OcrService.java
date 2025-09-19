package com.example.uretimveri.service;

import com.example.uretimveri.ocr.OcrApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OcrService {

    private final WebClient pythonClient;

    @SuppressWarnings("null")
    public OcrApiResponse recognize(MultipartFile file) {
        try {
            MultipartBodyBuilder mb = new MultipartBodyBuilder();
            mb.part("file", file.getResource())
              .filename(file.getOriginalFilename())
              .contentType(file.getContentType() != null
                      ? MediaType.parseMediaType(file.getContentType())
                      : MediaType.APPLICATION_OCTET_STREAM);

            return pythonClient.post()
                    .uri("/process")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(mb.build()))
                    .retrieve()
                    .bodyToMono(OcrApiResponse.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("Python HTTP hata: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw ex;
        } catch (Exception ex) {
            log.error("Python entegrasyon hatası", ex);
            throw ex;
        }
    }
}
