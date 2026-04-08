package gt.edu.url.descensogt.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultados de los últimos N partidos del equipo.
 * Se usa para proyectar con forma reciente en vez del promedio de temporada.
 */
@Data
@NoArgsConstructor
public class FormaReciente {

    /** Victorias en los últimos partidos. */
    private int jg;

    /** Empates en los últimos partidos. */
    private int je;

    /** Derrotas en los últimos partidos. */
    private int jp;
}
