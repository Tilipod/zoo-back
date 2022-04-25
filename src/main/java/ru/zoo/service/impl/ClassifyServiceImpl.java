package ru.zoo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.zoo.service.ClassifyService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassifyServiceImpl implements ClassifyService {

    @Value("${pathToClassify}")
    private String pathToClassify;

    @Value("${pathToModel}")
    private String pathToModel;

    @Value("${countOutputs}")
    private int countOutputs;

    @Value("#{'${dictionary}'.split(',')}")
    private List<String> dictionary;

    private MultiLayerNetwork network;

    private MultiLayerNetwork loadNetwork() {
        if (pathToModel == null) {
            log.error("Пустой путь к модели, невозможно загрузить нейронную сеть");
            return null;
        }

        try {
            return ModelSerializer.restoreMultiLayerNetwork(pathToModel);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private File saveImageToTempFile(MultipartFile image) {
        File directory = new File(pathToClassify);

        try {
            if (directory.exists()) {
                FileUtils.cleanDirectory(directory);
            } else if (!directory.mkdir()) {
                log.warn("Не удалось создать временную директорию");
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.warn("Не удалось удалить временную директорию");
        }

        File file = new File(pathToClassify.concat(Objects.requireNonNull(image.getOriginalFilename())));
        try {
            if (!file.exists() && !file.createNewFile()) {
                log.warn("Невозможно создать файл для классификации изображения");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (OutputStream out = new FileOutputStream(file)) {
            IOUtils.copy(image.getInputStream(), out);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return directory;
    }

    private DataSetIterator createClassifyDatasetIter(MultipartFile image) throws IOException {
        // Векторизация данных
        File file = saveImageToTempFile(image);
        FileSplit datasetSplit = new FileSplit(file, NativeImageLoader.ALLOWED_FORMATS, new Random(1));

        ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();
        ImageRecordReader datasetRR = new ImageRecordReader(384, 384, 3, labelMaker);

        try {
            datasetRR.initialize(datasetSplit);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }

        DataSetIterator datasetIter = new RecordReaderDataSetIterator(datasetRR, 1, 1, countOutputs);

        // Масштабируем пиксель от 0-255 до 0-1
        DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
        scaler.fit(datasetIter);
        datasetIter.setPreProcessor(scaler);
        datasetIter.reset();

        return datasetIter;
    }

    @Override
    public String classifyImage(MultipartFile image) throws IOException {
        if (network == null) {
            network = loadNetwork();
        }

        DataSetIterator classifyIter = createClassifyDatasetIter(image);

        INDArray out = network.output(classifyIter.next().getFeatures());
        log.info("Результат: {}", out);

        return dictionary.get(searchMax(out));
    }

    private int searchMax(INDArray out) {
        double value = -1;
        int index = -1;

        for (int i = 0; i < out.size(-1); i++) {
            if (Double.compare(out.getDouble(i), value) > 0) {
                index = i;
                value = out.getDouble(i);
            }
        }

        return index;
    }
}
