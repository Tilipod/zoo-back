package ru.zoo.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ClassifyService {

    String classifyImage(MultipartFile image) throws IOException;
}
