package gt.edu.url.descensogt.service;

import gt.edu.url.descensogt.model.FeatureVector;
import gt.edu.url.descensogt.model.PredictionResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clasificador Naive Bayes Categórico (discretizado en bins).
 *
 * El algoritmo calcula para cada clase c:
 *   P(c | x1, x2, ..., xn) ∝ P(c) * P(x1|c) * P(x2|c) * ... * P(xn|c)
 *
 * Como los valores son muy pequeños, se trabaja en escala logarítmica:
 *   log P(c | x) = log P(c) + Σ log P(xi | c)
 *
 * Variables continuas se discretizan en "bins" (intervalos) para poder
 * contar frecuencias como si fueran variables categóricas.
 */
@Service
public class NaiveBayesModel {

    /** Cuántas veces aparece cada clase en el entrenamiento. */
    private final Map<String, Integer> conteoClases = new HashMap<>();

    /**
     * Tabla de frecuencias: feature → clase → bin → conteo.
     * Ejemplo: counts["pts"]["SI"][3] = 5 significa que en 5 registros
     * con clase SI, la feature "pts" cayó en el bin 3.
     */
    private final Map<String, Map<String, Map<Integer, Integer>>> frecuencias = new HashMap<>();

    /** Prior de cada clase (probabilidad antes de ver las features). */
    private final Map<String, Double> priors = new HashMap<>();

    /**
     * Número de intervalos (bins) en los que se divide cada feature continua.
     * Más bins = más detalle, pero se necesitan más datos para estimar bien.
     */
    private static final int BINS = 10;

    /**
     * Alpha para suavizado de Laplace.
     * Evita que una probabilidad sea exactamente 0 cuando un bin no aparece
     * en el entrenamiento. Con alpha=1 se añade un conteo ficticio a cada bin.
     * Fórmula: P(xi = v | c) = (conteo(v,c) + alpha) / (total(c) + BINS * alpha)
     */
    private static final double LAPLACE_ALPHA = 1.0;

    /**
     * Peso extra que se aplica al prior de la clase "SI" (descendió).
     *
     * Motivación: el dataset está desbalanceado (~16% SI vs ~84% NO).
     * Sin ajuste, el modelo tiende a predecir siempre NO porque esa clase
     * domina el entrenamiento. Aumentar el prior del SI compensa ese sesgo.
     */
    private static final double PESO_DESCENSO = 1.4;

    /**
     * Umbral de probabilidad para predecir "SI" (descendió).
     *
     * En lugar del umbral estándar de 0.5, se baja a 0.45 para que el modelo
     * sea más sensible a los casos de descenso (que son la minoría).
     * Esto reduce falsos negativos (descensos no detectados) a costa de
     * algunos falsos positivos extra.
     *
     * P(SI | features) >= UMBRAL_DESCENSO  →  predice "SI"
     */
    private static final double UMBRAL_DESCENSO = 0.45;

    // -------------------------------------------------------------------------
    // Entrenamiento
    // -------------------------------------------------------------------------

    public void train(List<FeatureVector> data) {

        reset();

        // Paso 1: contar ocurrencias por clase y por feature
        for (FeatureVector f : data) {

            String clase = f.getLabel();

            conteoClases.put(clase, conteoClases.getOrDefault(clase, 0) + 1);

            // Discretizamos cada feature continua en un bin antes de contar
            registrar("pts",  bin(f.getPtsRate()),  clase);
            registrar("win",  bin(f.getWinRate()),  clase);
            registrar("draw", bin(f.getDrawRate()), clase);
            registrar("loss", bin(f.getLossRate()), clase);
            registrar("gf",   bin(f.getGfRate()),   clase);
            registrar("gc",   bin(f.getGcRate()),   clase);
            registrar("diff", bin(f.getDiffRate()), clase);
        }

        // Garantizar que ambas clases existan aunque no haya ejemplos de una
        conteoClases.putIfAbsent("SI", 0);
        conteoClases.putIfAbsent("NO", 0);

        int total = data.size();
        int numClases = 2;

        // Paso 2: calcular priors con suavizado de Laplace
        // P(c) = (conteo(c) + 1) / (total + numClases)
        for (String clase : conteoClases.keySet()) {

            double prior = (conteoClases.get(clase) + 1.0) / (total + numClases);

            // Ajuste por desbalance: aumentamos el prior de "SI" para que el modelo
            // no ignore los casos de descenso (solo ~16% del dataset).
            if(clase.equals("SI")){
                prior = prior * PESO_DESCENSO;
            }

            priors.put(clase, prior);
        }
    }

    // -------------------------------------------------------------------------
    // Predicción
    // -------------------------------------------------------------------------

    public PredictionResult predict(FeatureVector f) {

        // Calculamos el log-score de cada clase y luego convertimos a probabilidad
        double logSI = puntaje("SI", f);
        double logNO = puntaje("NO", f);

        // Volvemos al espacio original de probabilidades
        double pSI = Math.exp(logSI);
        double pNO = Math.exp(logNO);

        // Normalizamos para obtener probabilidades que sumen 1
        double suma = pSI + pNO;
        double probSI = pSI / suma;
        double probNO = pNO / suma;

        PredictionResult resultado = new PredictionResult();
        resultado.setEquipo(f.getEquipo());
        resultado.setProbabilityYes(probSI);
        resultado.setProbabilityNo(probNO);

        // Decisión: usar umbral ajustado (< 0.5) para compensar desbalance de clases
        resultado.setPrediction(probSI >= UMBRAL_DESCENSO ? "SI" : "NO");

        return resultado;
    }

    // -------------------------------------------------------------------------
    // Evaluación
    // -------------------------------------------------------------------------

    public ConfusionMatrix evaluate(List<FeatureVector> testSet) {

        ConfusionMatrix cm = new ConfusionMatrix();

        for (FeatureVector f : testSet) {
            PredictionResult resultado = predict(f);
            cm.addPrediction(f.getLabel(), resultado.getPrediction());
        }

        return cm;
    }

    // -------------------------------------------------------------------------
    // Métodos internos
    // -------------------------------------------------------------------------

    /**
     * Calcula el log-score de una clase dada un vector de features.
     * log P(clase | features) ∝ log P(clase) + Σ log P(feature_i | clase)
     */
    private double puntaje(String clase, FeatureVector f){

        double logPrior = Math.log(priors.getOrDefault(clase, 1.0 / (conteoClases.size() + 1)));

        double logVerosimilitud = 0;
        logVerosimilitud += logProb("pts",  bin(f.getPtsRate()),  clase);
        logVerosimilitud += logProb("win",  bin(f.getWinRate()),  clase);
        logVerosimilitud += logProb("draw", bin(f.getDrawRate()), clase);
        logVerosimilitud += logProb("loss", bin(f.getLossRate()), clase);
        logVerosimilitud += logProb("gf",   bin(f.getGfRate()),   clase);
        logVerosimilitud += logProb("gc",   bin(f.getGcRate()),   clase);
        logVerosimilitud += logProb("diff", bin(f.getDiffRate()), clase);

        return logPrior + logVerosimilitud;
    }

    /** Registra una observación: la feature 'nombre' tomó valor 'bin' para la clase dada. */
    private void registrar(String nombre, int valorBin, String clase) {

        frecuencias.putIfAbsent(nombre, new HashMap<>());
        frecuencias.get(nombre).putIfAbsent(clase, new HashMap<>());

        Map<Integer, Integer> mapa = frecuencias.get(nombre).get(clase);
        mapa.put(valorBin, mapa.getOrDefault(valorBin, 0) + 1);
    }

    /**
     * Calcula log P(feature=valorBin | clase) con suavizado de Laplace.
     *
     * Sin suavizado, si un bin nunca apareció en el entrenamiento,
     * su probabilidad sería 0 y anularía todo el producto.
     * Con Laplace (alpha=1): P(v|c) = (conteo(v,c) + 1) / (total(c) + BINS)
     */
    private double logProb(String nombre, int valorBin, String clase) {

        Map<String, Map<Integer, Integer>> tablaFeature = frecuencias.get(nombre);

        int conteo = 0;
        if(tablaFeature != null){
            Map<Integer, Integer> mapa = tablaFeature.get(clase);
            if(mapa != null){
                conteo = mapa.getOrDefault(valorBin, 0);
            }
        }

        int totalClase = conteoClases.getOrDefault(clase, 0);

        // Suavizado de Laplace: evita probabilidad 0 para bins no vistos
        double prob = (conteo + LAPLACE_ALPHA) / (totalClase + BINS * LAPLACE_ALPHA);

        return Math.log(prob);
    }

    /**
     * Convierte un valor continuo [0, 1] en un bin entero [0, BINS-1].
     * Ejemplo con BINS=10: 0.35 → bin 3 (el intervalo [0.3, 0.4))
     */
    private int bin(double valor) {

        int b = (int) Math.floor(valor * BINS);

        // Clamp para valores en los bordes (exactamente 0.0 o 1.0)
        if(b >= BINS) b = BINS - 1;
        if(b < 0)     b = 0;

        return b;
    }

    private void reset() {

        conteoClases.clear();
        frecuencias.clear();
        priors.clear();
    }
}
