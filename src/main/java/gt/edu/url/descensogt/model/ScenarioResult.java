package gt.edu.url.descensogt.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioResult {

    /** Nombre del escenario (ej: "Mantiene forma", "Gana todos", "Pierde todos"). */
    private String escenario;

    /** Stats proyectadas al targetJornada bajo este escenario. */
    private MatchRecord statsProyectadas;

    /** Predicción del modelo sobre esas stats proyectadas. */
    private PredictionResult prediccion;
}
