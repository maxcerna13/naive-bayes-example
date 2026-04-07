package gt.edu.url.descensogt.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MatchRecord {

    private String equipo;

    private int jj;
    private int pts;
    private int jg;
    private int je;
    private int jp;
    private int gf;
    private int gc;
    private int diff;

    private String descendio;
}
