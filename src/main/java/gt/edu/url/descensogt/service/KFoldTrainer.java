package gt.edu.url.descensogt.service;

import gt.edu.url.descensogt.model.FeatureVector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Validación cruzada con K pliegues (K-Fold Cross Validation).
 *
 * El dataset se divide en K partes iguales (pliegues).
 * En cada iteración:
 *   - 1 pliegue se usa como conjunto de prueba
 *   - Los K-1 pliegues restantes se usan para entrenar
 *
 * Esto se repite K veces (rotando qué pliegue es el de prueba)
 * y se acumulan los resultados en una sola matriz de confusión.
 *
 * Ventaja: cada ejemplo se evalúa exactamente una vez,
 * por lo que la estimación del rendimiento es más confiable
 * que una sola división train/test.
 *
 * Nota: la métrica reportada es una estimación del rendimiento
 * esperado del modelo, no una evaluación sobre datos completamente
 * nuevos. Para un test definitivo se recomienda reservar un
 * conjunto de prueba externo antes de ejecutar el K-Fold.
 */
@Service
@RequiredArgsConstructor
public class KFoldTrainer {

    public ConfusionMatrix train3Fold(
            List<FeatureVector> dataset,
            NaiveBayesModel model){

        // Mezclamos aleatoriamente para evitar que los pliegues
        // queden ordenados por temporada o por clase
        List<FeatureVector> datos = new ArrayList<>(dataset);
        Collections.shuffle(datos);

        int k = 3;
        int tamPliegue = datos.size() / k;

        ConfusionMatrix total = new ConfusionMatrix();

        System.out.println("=== Validación cruzada " + k + "-Fold ===");
        System.out.println("Total de ejemplos: " + datos.size());
        System.out.println("Tamaño aproximado por pliegue: " + tamPliegue);
        System.out.println();

        for(int i = 0; i < k; i++){

            // Determinar el rango del pliegue de prueba
            int inicio = i * tamPliegue;
            int fin    = (i == k - 1) ? datos.size() : inicio + tamPliegue;

            List<FeatureVector> prueba       = new ArrayList<>(datos.subList(inicio, fin));
            List<FeatureVector> entrenamiento = new ArrayList<>();

            for(int j = 0; j < datos.size(); j++){
                if(j < inicio || j >= fin){
                    entrenamiento.add(datos.get(j));
                }
            }

            // Entrenar con K-1 pliegues y evaluar en el pliegue restante
            model.train(entrenamiento);
            ConfusionMatrix matrizPliegue = model.evaluate(prueba);

            System.out.printf("Pliegue %d | train=%d  test=%d | " +
                            "Accuracy=%.2f  Precision=%.2f  Recall=%.2f  F1=%.2f%n",
                    i + 1,
                    entrenamiento.size(),
                    prueba.size(),
                    matrizPliegue.accuracy(),
                    matrizPliegue.precision(),
                    matrizPliegue.recall(),
                    matrizPliegue.f1());

            total.merge(matrizPliegue);
        }

        System.out.println();
        System.out.printf("TOTAL   | Accuracy=%.2f  Precision=%.2f  Recall=%.2f  F1=%.2f%n",
                total.accuracy(),
                total.precision(),
                total.recall(),
                total.f1());
        System.out.println("=========================================");

        // Reentrenar con todos los datos para que el modelo quede
        // listo para producción con la mayor cantidad de información posible
        model.train(datos);

        return total;
    }
}
