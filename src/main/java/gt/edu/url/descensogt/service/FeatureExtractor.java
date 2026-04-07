package gt.edu.url.descensogt.service;

import gt.edu.url.descensogt.model.FeatureVector;
import gt.edu.url.descensogt.model.MatchRecord;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Transforma un MatchRecord (datos crudos del CSV) en un FeatureVector
 * con las variables normalizadas por partidos jugados.
 *
 * ¿Por qué normalizar?
 * Los equipos pueden haber jugado distinta cantidad de partidos según
 * la temporada (22 o 36 partidos). Dividir entre JJ convierte los
 * valores absolutos en tasas comparables entre temporadas.
 *
 * Features generadas (todas en el rango [0, 1]):
 *   ptsRate  = puntos       / JJ
 *   winRate  = victorias    / JJ
 *   drawRate = empates      / JJ
 *   lossRate = derrotas     / JJ
 *   gfRate   = goles a favor  / JJ
 *   gcRate   = goles en contra / JJ
 *   diffRate = diferencia de goles / JJ  (puede ser negativo → bin 0)
 */
@Data
@NoArgsConstructor
@Component
public class FeatureExtractor {

    public FeatureVector transform(MatchRecord r){

        FeatureVector f = new FeatureVector();

        double jj = r.getJj(); // partidos jugados (denominador normalizador)

        f.setEquipo(r.getEquipo());

        f.setPtsRate(r.getPts()  / jj);
        f.setWinRate(r.getJg()   / jj);
        f.setDrawRate(r.getJe()  / jj);
        f.setLossRate(r.getJp()  / jj);
        f.setGfRate(r.getGf()    / jj);
        f.setGcRate(r.getGc()    / jj);
        f.setDiffRate(r.getDiff()/ jj); // diferencial puede ser negativo; el bin() lo maneja con clamp

        // Normalizar la etiqueta a mayúsculas para evitar problemas con "Si"/"SI"/"si"
        f.setLabel(r.getDescendio().trim().toUpperCase());

        return f;
    }
}
