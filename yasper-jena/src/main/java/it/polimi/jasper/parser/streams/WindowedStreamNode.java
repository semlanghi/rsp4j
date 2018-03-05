package it.polimi.jasper.parser.streams;

import com.espertech.esper.client.soda.AnnotationPart;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.soda.View;
import it.polimi.jasper.esper.EsperWindowAssigner;
import it.polimi.jasper.esper.EPLFactory;
import it.polimi.yasper.core.rspql.Stream;
import it.polimi.yasper.core.spe.windowing.assigner.WindowAssigner;
import it.polimi.yasper.core.enums.WindowType;
import it.polimi.yasper.core.spe.windowing.operator.WindowOperator;
import it.polimi.yasper.core.utils.EncodingUtils;
import lombok.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.jena.graph.Node_URI;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Riccardo on 12/08/16.
 */
@Data
@NoArgsConstructor
@ToString(exclude = {"regex", "p"})
@RequiredArgsConstructor
public class WindowedStreamNode implements WindowOperatorNode, WindowOperator {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    final private String regex = "([0-9]+)\\s*(ms|s|m|h|d|GRAPH|TRIPLES)";
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    final private Pattern p = Pattern.compile(regex);
    @NonNull

    private Node_URI iri;
    private Integer beta;
    private Integer omega;
    private String unit_omega;
    private String unit_beta;
    private StreamNode stream;
    private WindowType type = WindowType.Logical;
    private View window;

    public WindowedStreamNode addConstrain(String match) {
        // TODO hide visibility out of the package
        Matcher matcher = p.matcher(match);
        if (matcher.find()) {
            MatchResult res = matcher.toMatchResult();
            this.beta = this.omega = Integer.parseInt(res.group(1));
            this.unit_beta = this.unit_omega = res.group(2);
            if ("GRAPH".equals(unit_omega) || "TRIPLE".equals(unit_omega)) {
                this.type = WindowType.Physical;
            }

        }
        return this;
    }

    public WindowedStreamNode addSlide(String match) {
        // TODO hide visibility out of the package
        Matcher matcher = p.matcher(match);
        if (matcher.find()) {
            MatchResult res = matcher.toMatchResult();
            this.beta = Integer.parseInt(res.group(1));
            this.unit_beta = res.group(2);
            if ("GRAPH".equals(unit_beta) || "TRIPLE".equals(unit_beta)) {
                this.type = WindowType.Physical;
            }
        }
        return this;
    }

    public WindowedStreamNode addStreamUri(Node_URI uri) {
        // TODO hide visibility out of the package
        if (stream == null) {
            stream = new StreamNode(uri);
        }
        stream.setIri(uri);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        WindowedStreamNode window = (WindowedStreamNode) o;

        if ((isNamed() && !window.isNamed()) || (!isNamed() && window.isNamed())) {
            return false;
        }

        if (isNamed() && window.isNamed()) {
            return iri.equals(window.getIri());
        }

        if (!isNamed() && !window.isNamed()) {
            return stream.equals(window.getStream());
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = iri != null ? iri.hashCode() : 0;
        result = 31 * result + (beta != null ? beta.hashCode() : 0);
        result = 31 * result + (omega != null ? omega.hashCode() : 0);
        result = 31 * result + (unit_omega != null ? unit_omega.hashCode() : 0);
        result = 31 * result + (unit_beta != null ? unit_beta.hashCode() : 0);
        result = 31 * result + (stream != null ? stream.hashCode() : 0);
        return result;
    }

    public boolean isNamed() {
        return iri != null;
    }

    public String getStreamURI() {
        return stream.getIri().getURI();
    }

    @Override
    public String getName() {
        return iri.getURI();
    }

    @Override
    public int getT0() {
        return -1;
    }

    @Override
    public int getRange() {
        return omega.intValue();
    }

    @Override
    public int getStep() {
        return beta.intValue();
    }

    @Override
    public String getUnitRange() {
        return unit_omega;
    }

    @Override
    public String getUnitStep() {
        return unit_beta;
    }

    @Override
    public WindowAssigner apply(Stream s) {
        View window = EPLFactory.getWindow(getRange(), getUnitRange(), getType());
        List<AnnotationPart> annotations = EPLFactory.getAnnotations(getName(), getRange(), getStep(), getStreamURI());
        EPStatementObjectModel epStatementObjectModel = EPLFactory.toEPL(getStep(), getUnitStep(), getType(), getStreamURI(), window, annotations);
        try {
            return new EsperWindowAssigner(EncodingUtils.encode(getStreamURI()), epStatementObjectModel);
        } catch (ConfigurationException e) {
            throw new RuntimeException("Error During Stream Registration");
        }
    }
}
