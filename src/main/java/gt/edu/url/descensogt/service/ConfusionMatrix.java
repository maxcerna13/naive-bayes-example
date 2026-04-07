package gt.edu.url.descensogt.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Matriz de confusión para clasificación binaria.
 *
 * Clase positiva = "SI" (descendió)
 * Clase negativa = "NO" (no descendió)
 *
 *                  Predicho SI   Predicho NO
 * Real SI (pos)       TP             FN
 * Real NO (neg)       FP             TN
 *
 * TP (Verdadero Positivo): el modelo dijo SI y realmente descendió.
 * TN (Verdadero Negativo): el modelo dijo NO y realmente no descendió.
 * FP (Falso  Positivo):    el modelo dijo SI pero NO descendió.  (alarma falsa)
 * FN (Falso  Negativo):    el modelo dijo NO pero SÍ descendió.  (descenso no detectado)
 */
@Data
public class ConfusionMatrix {

    private int tp;
    private int tn;
    private int fp;
    private int fn;

    public void addPrediction(String real, String predicted){

        if(real.equals("SI") && predicted.equals("SI"))
            tp++;

        else if(real.equals("NO") && predicted.equals("NO"))
            tn++;

        else if(real.equals("NO") && predicted.equals("SI"))
            fp++;

        else if(real.equals("SI") && predicted.equals("NO"))
            fn++;
    }

    public void merge(ConfusionMatrix otra){

        this.tp += otra.tp;
        this.tn += otra.tn;
        this.fp += otra.fp;
        this.fn += otra.fn;
    }

    /**
     * Exactitud: ¿qué fracción de todas las predicciones fue correcta?
     * (TP + TN) / total
     */
    @JsonProperty
    public double accuracy(){

        int total = tp + tn + fp + fn;
        if(total == 0) return 0.0;
        return (tp + tn) / (double) total;
    }

    /**
     * Precisión: de los que el modelo predijo como SI, ¿cuántos realmente descendieron?
     * TP / (TP + FP)
     * Retorna 0 si el modelo nunca predijo SI.
     */
    @JsonProperty
    public double precision(){

        if(tp + fp == 0) return 0.0;
        return tp / (double)(tp + fp);
    }

    /**
     * Recall (Sensibilidad): de los que realmente descendieron, ¿cuántos detectó el modelo?
     * TP / (TP + FN)
     * Retorna 0 si no hubo ningún caso positivo real.
     */
    @JsonProperty
    public double recall(){

        if(tp + fn == 0) return 0.0;
        return tp / (double)(tp + fn);
    }

    /**
     * F1: media armónica entre precisión y recall.
     * Útil cuando las clases están desbalanceadas (como en este dataset).
     * Retorna 0 si precisión y recall son ambos 0.
     */
    @JsonProperty
    public double f1(){

        double p = precision();
        double r = recall();

        if(p + r == 0.0) return 0.0;
        return 2 * (p * r) / (p + r);
    }
}
