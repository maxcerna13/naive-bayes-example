package gt.edu.url.descensogt.service;

import gt.edu.url.descensogt.model.FormaReciente;
import gt.edu.url.descensogt.model.MatchRecord;
import gt.edu.url.descensogt.model.ScenarioResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Proyecta las estadísticas de un equipo desde la jornada actual
 * hasta una jornada futura bajo estos escenarios:
 *
 *   1. Mantiene forma:  tasas del promedio de toda la temporada
 *   2. Forma reciente:  tasas calculadas de los últimos N partidos (opcional)
 *   3. Gana todos:      gana todos los partidos restantes
 *   4. Pierde todos:    pierde todos los partidos restantes
 *
 * Los goles en escenarios 3 y 4 se estiman con la tasa de temporada
 * para mantener coherencia con el modelo Naive Bayes.
 */
@Service
@RequiredArgsConstructor
public class ProjectionService {

    private final FeatureExtractor extractor;
    private final NaiveBayesModel model;

    public List<ScenarioResult> project(MatchRecord actual, int targetJornada, FormaReciente forma) {

        int restantes = targetJornada - actual.getJj();

        if (restantes < 0) {
            throw new IllegalArgumentException(
                "targetJornada (" + targetJornada + ") debe ser mayor que la jornada actual (" + actual.getJj() + ")");
        }

        List<ScenarioResult> resultados = new ArrayList<>();

        MatchRecord mantiene = proyectarMantiene(actual, restantes, targetJornada);
        resultados.add(new ScenarioResult("Promedio temporada", mantiene, model.predict(extractor.transform(mantiene))));

        // Escenario de forma reciente solo si se proveyeron los datos
        if (forma != null) {
            MatchRecord reciente = proyectarFormaReciente(actual, restantes, targetJornada, forma);
            resultados.add(new ScenarioResult("Forma reciente", reciente, model.predict(extractor.transform(reciente))));
        }

        MatchRecord gana   = proyectarGanaTodos(actual, restantes, targetJornada);
        MatchRecord pierde = proyectarPierdeTodos(actual, restantes, targetJornada);
        resultados.add(new ScenarioResult("Gana todos",   gana,   model.predict(extractor.transform(gana))));
        resultados.add(new ScenarioResult("Pierde todos", pierde, model.predict(extractor.transform(pierde))));

        return resultados;
    }

    // -------------------------------------------------------------------------
    // Escenario 1: promedio de toda la temporada
    // -------------------------------------------------------------------------

    private MatchRecord proyectarMantiene(MatchRecord r, int restantes, int targetJornada) {

        double jj = r.getJj();

        double ptsXjuego = r.getPts() / jj;
        double jgXjuego  = r.getJg()  / jj;
        double jeXjuego  = r.getJe()  / jj;
        double jpXjuego  = r.getJp()  / jj;
        double gfXjuego  = r.getGf()  / jj;
        double gcXjuego  = r.getGc()  / jj;

        MatchRecord m = new MatchRecord();
        m.setEquipo(r.getEquipo());
        m.setJj(targetJornada);
        m.setPts((int) Math.round(r.getPts() + ptsXjuego * restantes));
        m.setJg( (int) Math.round(r.getJg()  + jgXjuego  * restantes));
        m.setJe( (int) Math.round(r.getJe()  + jeXjuego  * restantes));
        m.setJp( (int) Math.round(r.getJp()  + jpXjuego  * restantes));
        m.setGf( (int) Math.round(r.getGf()  + gfXjuego  * restantes));
        m.setGc( (int) Math.round(r.getGc()  + gcXjuego  * restantes));
        m.setDiff(m.getGf() - m.getGc());
        return m;
    }

    // -------------------------------------------------------------------------
    // Escenario 2: forma reciente (últimos N partidos)
    // -------------------------------------------------------------------------

    /**
     * Usa las tasas de W/D/L de los últimos partidos para proyectar los restantes.
     * Puntos esperados = (victorias_restantes * 3) + (empates_restantes * 1)
     * Los goles siguen usando la tasa de temporada (no tenemos ese desglose reciente).
     */
    private MatchRecord proyectarFormaReciente(MatchRecord r, int restantes, int targetJornada, FormaReciente forma) {

        int totalRecientes = forma.getJg() + forma.getJe() + forma.getJp();

        // Tasas de la forma reciente
        double winRate  = (double) forma.getJg() / totalRecientes;
        double drawRate = (double) forma.getJe() / totalRecientes;
        double lossRate = (double) forma.getJp() / totalRecientes;

        int jgExtra  = (int) Math.round(winRate  * restantes);
        int jeExtra  = (int) Math.round(drawRate * restantes);
        int jpExtra  = restantes - jgExtra - jeExtra; // el resto son derrotas
        int ptsExtra = jgExtra * 3 + jeExtra;

        // Para goles usamos la tasa de temporada (no tenemos ese dato reciente)
        double gfXjuego = r.getGf() / (double) r.getJj();
        double gcXjuego = r.getGc() / (double) r.getJj();

        MatchRecord m = new MatchRecord();
        m.setEquipo(r.getEquipo());
        m.setJj(targetJornada);
        m.setPts(r.getPts() + ptsExtra);
        m.setJg(r.getJg()  + jgExtra);
        m.setJe(r.getJe()  + jeExtra);
        m.setJp(r.getJp()  + jpExtra);
        m.setGf((int) Math.round(r.getGf() + gfXjuego * restantes));
        m.setGc((int) Math.round(r.getGc() + gcXjuego * restantes));
        m.setDiff(m.getGf() - m.getGc());
        return m;
    }

    // -------------------------------------------------------------------------
    // Escenario 3: gana todos los partidos restantes
    // -------------------------------------------------------------------------

    private MatchRecord proyectarGanaTodos(MatchRecord r, int restantes, int targetJornada) {

        double gfXjuego = r.getGf() / (double) r.getJj();
        double gcXjuego = r.getGc() / (double) r.getJj();

        MatchRecord m = new MatchRecord();
        m.setEquipo(r.getEquipo());
        m.setJj(targetJornada);
        m.setPts(r.getPts() + restantes * 3);
        m.setJg(r.getJg()  + restantes);
        m.setJe(r.getJe());
        m.setJp(r.getJp());
        m.setGf((int) Math.round(r.getGf() + gfXjuego * restantes));
        m.setGc((int) Math.round(r.getGc() + gcXjuego * restantes));
        m.setDiff(m.getGf() - m.getGc());
        return m;
    }

    // -------------------------------------------------------------------------
    // Escenario 4: pierde todos los partidos restantes
    // -------------------------------------------------------------------------

    private MatchRecord proyectarPierdeTodos(MatchRecord r, int restantes, int targetJornada) {

        double gfXjuego = r.getGf() / (double) r.getJj();
        double gcXjuego = r.getGc() / (double) r.getJj();

        MatchRecord m = new MatchRecord();
        m.setEquipo(r.getEquipo());
        m.setJj(targetJornada);
        m.setPts(r.getPts());
        m.setJg(r.getJg());
        m.setJe(r.getJe());
        m.setJp(r.getJp() + restantes);
        m.setGf((int) Math.round(r.getGf() + gfXjuego * restantes));
        m.setGc((int) Math.round(r.getGc() + gcXjuego * restantes));
        m.setDiff(m.getGf() - m.getGc());
        return m;
    }
}
