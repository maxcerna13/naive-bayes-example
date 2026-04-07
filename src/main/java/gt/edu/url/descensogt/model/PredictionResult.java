package gt.edu.url.descensogt.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PredictionResult {

    private String equipo;

    private String prediction;

    private double probabilityYes;

    private double probabilityNo;

}
