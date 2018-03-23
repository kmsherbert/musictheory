package algebra;

import java.util.ArrayList;

import music.Phrase;
import music.Chord;
import music.Note;

/**
 * Provides a quasi-Algebra implementation of the pure harmonic algebra
 *
 * Methods are the same, except that a double[] pressure wave is always returned in place of a Phrase
 *
 * It can't use Phrases like the others, because those were defined as collections of keyboard notes, which in general the pure harmonic algebra is not.
 *
 * See report.pdf for details of calculations
 */
public class ExactHarmonic {
    /**
     * the angular frequency of the highest pitch to ask the sound card to play.
     *
     * Select it to be roughly as high as the human ear can hear.
     */
    public static final float max_w = (float)(2*Math.PI * 12500);

    private float r;

    /**
     * @param r - the sample rate to use for generating pressure wave amplitudes
     */
    public ExactHarmonic(float r) {
        this.r = r;
    }


    public double[] sum(Phrase... Ps) {
        if (! Phrase.sameKey(Ps))
            throw new IllegalArgumentException("Cannot mix phrases of different key");
        if (! Phrase.sameTempo(Ps))
            throw new IllegalArgumentException("Cannot mix phrases of different tempo");
        if (! Phrase.sameDuration(Ps))
            throw new IllegalArgumentException("Cannot mix phrases of different duration");

        ArrayList<double[]> ps = new ArrayList<>(Ps.length);
        for (int i = 0; i < Ps.length; i ++) {
            ps.add(Ps[i].phases(r));
        }

        double[] phases = new double[ps.get(0).length];
        // combine phases
        for (int i = 0; i < phases.length; i ++) {
            phases[i] = 0; // technically unnecessary in Java
            for (int j = 0; j < Ps.length; j ++) {
                phases[i] += ps.get(j)[i];
            }
        }

        return phases;
    }
    
    public double[] magnify(float A, Phrase P) {
        double[] orig = P.phases(r);
        double[] phases = new double[orig.length];

        for (int i = 0; i < phases.length; i ++) {
            phases[i] = A*orig[i];
        }

        return phases;
    }
    
    public double[] product(Phrase P1, Phrase P2) {
        if (! Phrase.sameKey(P1, P2))
            throw new IllegalArgumentException("Cannot mix phrases of different key");
        if (! Phrase.sameTempo(P1, P2))
            throw new IllegalArgumentException("Cannot mix phrases of different tempo");
        if (! Phrase.sameDuration(P1, P2))
            throw new IllegalArgumentException("Cannot mix phrases of different duration");

        double[] orig1 = P1.phases(r);
        double[] orig2 = P2.phases(r);
        double[] phases = new double[Math.min(orig1.length, orig2.length)];

        for (int i = 0; i < phases.length; i ++) {
            phases[i] = orig1[i] * orig2[i];
        }

        return phases;
    }

    public double[] inverse(Phrase P) {
        int Npc = (int)(r * P.spc); // samples / chord
        int cap = Npc / 100; // which sample in a pulse to start decay
        // int cap = 35;

        double[] phases = new double[Npc * P.length];
        for (int i = 0; i < P.length; i ++) { // i iterates over chords
            Chord C = P.C(i);

            for (int j = 0; j < Npc; j ++) { // j iterates over samples
                float t = j / r; // sec

                double phase = 0;
                for (int k = 0; k < C.length; k ++) { // k iterates over notes
                    Note N = C.N(k);

                    int l = 1;              // l iterates over harmonics
                    float amp = 2*C.A(k);   // alternates between 2A and -2A

                    // phase += amp * Math.sin(N.w * t);    // the regular note
                    while(l*N.w < max_w) {  // the inversion
                    // while (l < 10) {
                        phase += amp * Math.sin(l*N.w * t);
                        l ++;
                        amp *= -1;
                    }

                    // linearly modulate start and end of pulse
                    if (j < cap) phase *= (1.0*j)/cap;
                    if (Npc-j < cap) phase *= (Npc - 1.0*j)/cap;
                }
                phases[Npc*i+j] = phase;
            }
        }
        
        System.out.println("Done");
        return phases;
    }
}
