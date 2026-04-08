package gt.edu.url.descensogt.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProjectionRequest {

    /** Stats actuales del equipo (con el jj de la jornada actual). */
    private MatchRecord current;

    /** Jornada futura a la que se quiere proyectar (ej: 44). */
    private int targetJornada;

    /**
     * Resultados de los últimos partidos (opcional).
     * Si se provee, se agrega el escenario "Forma reciente" que proyecta
     * usando esas tasas en vez del promedio de toda la temporada.
     */
    private FormaReciente formaReciente;
}
