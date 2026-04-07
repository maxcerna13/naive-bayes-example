package gt.edu.url.descensogt.service;

import gt.edu.url.descensogt.model.MatchRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvLoader {

    public List<MatchRecord> load(String path){

        List<MatchRecord> list = new ArrayList<>();

        try{

            ClassPathResource resource =
                    new ClassPathResource(path);

            BufferedReader br =
                    new BufferedReader(
                            new InputStreamReader(
                                    resource.getInputStream()
                            )
                    );

            String linea;

            br.readLine(); // saltar encabezado

            while((linea = br.readLine()) != null){

                String[] cols = linea.split(",");

                // Columnas del CSV:
                // 0=Temporada, 1=Equipo, 2=JJ, 3=JG, 4=JE, 5=JP,
                // 6=GF, 7=GC, 8=DIF, 9=PTS, 10=POS, 11=Descendio?
                MatchRecord registro = new MatchRecord();

                registro.setEquipo(cols[1]);

                registro.setJj(Integer.parseInt(cols[2]));
                registro.setJg(Integer.parseInt(cols[3]));
                registro.setJe(Integer.parseInt(cols[4]));
                registro.setJp(Integer.parseInt(cols[5]));

                registro.setGf(Integer.parseInt(cols[6]));
                registro.setGc(Integer.parseInt(cols[7]));
                registro.setDiff(Integer.parseInt(cols[8])); // diferencial de goles (GF - GC)

                registro.setPts(Integer.parseInt(cols[9]));

                registro.setDescendio(cols[11]);

                list.add(registro);
            }

        }catch(Exception e){

            throw new RuntimeException(
                    "Error leyendo CSV en resources/"+path,
                    e
            );
        }

        return list;
    }
}