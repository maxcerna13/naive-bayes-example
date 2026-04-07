package gt.edu.url.descensogt.controller;

import gt.edu.url.descensogt.model.FeatureVector;
import gt.edu.url.descensogt.model.MatchRecord;
import gt.edu.url.descensogt.model.PredictionResult;
import gt.edu.url.descensogt.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/descenso")
@RequiredArgsConstructor
public class PredictionController {

    private final CsvLoader loader;
    private final FeatureExtractor extractor;
    private final KFoldTrainer trainer;
    private final NaiveBayesModel model;

    @PostMapping("/train")
    public ConfusionMatrix train(){

        List<MatchRecord> raw =
                loader.load("data/descenso.csv");

        List<FeatureVector> features =
                raw.stream()
                        .map(extractor::transform)
                        .toList();

        return trainer.train3Fold(features, model);
    }

    @PostMapping("/predict")
    public PredictionResult predict(
            @RequestBody MatchRecord record){

        FeatureVector f = extractor.transform(record);

        return model.predict(f);
    }
}
