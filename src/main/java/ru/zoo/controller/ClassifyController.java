package ru.zoo.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.zoo.service.ClassifyService;

import java.io.IOException;

@RestController
@RequestMapping("/classify")
@RequiredArgsConstructor
@Api(description = "Контроллер для классификации животных по изображению")
public class ClassifyController {

    private final ClassifyService classifyService;

    @PostMapping("/classify")
    @ApiOperation(value = "Определить класс изображения")
    public ResponseEntity<String> classifyImage(@RequestParam MultipartFile image) throws IOException {
        return ResponseEntity.ok(classifyService.classifyImage(image));
    }
}
