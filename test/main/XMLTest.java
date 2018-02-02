package main;

import glade.grammar.GrammarUtils;
import glade.grammar.GrammarFuzzer;
import glade.grammar.synthesize.GrammarSynthesis;
import glade.util.Log;
import org.junit.Test;

import javax.xml.stream.XMLInputFactory;
import java.io.StringReader;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class XMLTest {

    @Test
    public void test1() {
        List<String> examples = List.of(
                "<a xy=\"xy\">xy<a xy=\"xy\">xy<a>xy</a>xy</a>xy</a>",
                "<a>xy<![CDATA[xy]]>xy</a>",
                "<a>xy<!--xy-->xy</a>",
                "<a>123</a>",
                "<a><a>x</a></a>",
                "<a>xy</a>",
                "<a>xy<?xy xy?>xy</a>",
                "<a>xy<a xy=\"xy\"/>xy</a>",
                "<a xy=\"xy\"/>",
                "<a/>"
        );
        Predicate<String> oracle = (q) -> {
            try {
                XMLInputFactory.newDefaultFactory().createXMLEventReader(new StringReader(q)).forEachRemaining(r->{
                    //System.out.println(r);
                });
                return true;
            } catch (Throwable e) {
                //e.printStackTrace();
                return false;
            }
        };

        // learn grammar
        GrammarUtils.Grammar grammar = GrammarSynthesis.learn(examples, oracle);

        // fuzz using grammar
        Iterable<String> samples = new GrammarFuzzer.GrammarMutationSampler(grammar, new GrammarFuzzer.SampleParameters(new double[]{
                0.2, 0.2, 0.2, 0.4}, // (multinomial) distribution of repetitions
                0.8,                                          // probability of using recursive production
                0.1,                                          // probability of a uniformly random character (vs. a special character)
                100),
                1000, 20, new Random(0));

        int pass = 0;
        int count = 0;
        int numSamples = 20;
        for(String sample : samples) {
            Log.info("SAMPLE: " + sample);
            if(oracle.test(sample)) {
                Log.info("PASS");
                pass++;
            } else {
                Log.info("FAIL");
            }
            Log.info("");
            count++;
            if(count >= numSamples) {
                break;
            }
        }
        float rate = (float) pass / numSamples;
        Log.info("PASS RATE: " + rate);

        //assertEquals(pass, numSamples);
        assertTrue(rate > 0.75f);

    }
}
