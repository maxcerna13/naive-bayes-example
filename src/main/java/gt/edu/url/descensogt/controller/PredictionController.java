package gt.edu.url.descensogt.controller;

import gt.edu.url.descensogt.model.FeatureVector;
import gt.edu.url.descensogt.model.MatchRecord;
import gt.edu.url.descensogt.model.PredictionResult;
import gt.edu.url.descensogt.model.ProjectionRequest;
import gt.edu.url.descensogt.model.ScenarioResult;
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
    private final ProjectionService projectionService;

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

    /**
     * Proyecta las estadísticas de un equipo a una jornada futura y
     * devuelve tres escenarios: mantiene forma, gana todos, pierde todos.
     *
     * Ejemplo de body:
     * {
     *   "current": {
     *     "equipo": "Comunicaciones",
     *     "jj": 40, "pts": 45,
     *     "jg": 13, "je": 6, "jp": 21,
     *     "gf": 35, "gc": 55, "diff": -20
     *   },
     *   "targetJornada": 44
     * }
     */
    @PostMapping("/predict-jornada")
    public List<ScenarioResult> predictJornada(
            @RequestBody ProjectionRequest request){

        return projectionService.project(request.getCurrent(), request.getTargetJornada(), request.getFormaReciente());
    }
}
