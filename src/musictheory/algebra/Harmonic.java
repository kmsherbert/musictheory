package musictheory.algebra;

import musictheory.music.Note;
import musictheory.music.Chord;
import musictheory.music.Phrase;

import java.util.HashMap;

/**
 * Algebra implementing the Chromatic Harmonic Algebra described in report.pdf
 */
public class Harmonic implements Algebra {
    /**
     * "harmonic array" - the number of half-steps between successive harmonics
     *
     * later numbers should be taken with a grain of salt, as there is some drastic
     * round off error
     *
     * in fact, the array stops where it does because successive harmonics start
     * occurring on the same note...
     */
    public static final int[] h = {
        0, 19, 28, 34, 38, 42, 44, 47, 49, 51, 53, 54, 56, 57
    }; // harmonic shift sequence
    private static final float default_eps = (float).0001;

    private float eps;

    /**
     * Constructs a Harmonic algebra, ignoring any note with an amplitude (relative dynamic) less than eps
     */
    public Harmonic(float eps) {
        super();
        this.eps = eps;
    }

    /**
     * uses a default epsilon value of .0001
     */
    public Harmonic() {
        this(default_eps);
    }

    private Chord inverse(Note N) {
        Note[] notes = new Note[h.length];
        float[] amps = new float[h.length];

        for (int i = 0; i < h.length; i ++) {
            notes[i] = new Note(N.n + h[i]);
            amps[i] = 2;
            if ((i & 1) == 1) amps[i] *= -1; // alternating sequence
        }

        return new Chord(notes, amps);
    }

    private Chord inverse(Chord C) {
        Chord[] inverses = new Chord[C.length];
        for (int i = 0; i < C.length; i ++) {
            inverses[i] = magnify(C.A(i), inverse(C.N(i)));
        }
        return sum(inverses);
    }

    private Chord magnify(float A, Chord C) {
        Note[] notes = new Note[C.length];
        float[] mags = new float[C.length];
        for (int i = 0; i < C.length; i ++) {
            notes[i] = C.N(i);
            mags[i] = A * C.A(i);
        }
        return new Chord(notes, mags);
    }

    private Chord sum(Chord... Cs) {
        HashMap<Integer, Float> map = new HashMap<>();
        for (Chord C: Cs) {
            for (int i = 0; i < C.length; i ++) {
                Float amp = map.get(C.N(i).n);
                if (amp == null) amp = (float)0.0;
                amp += C.A(i);
                if (Math.abs(amp) < eps) map.remove(C.N(i).n);
                else map.put(C.N(i).n, amp);
            }
        }

        return new Chord(map);
    }

    private Chord product(Chord C1, Chord C2) {
        HashMap<Integer, Float> map = new HashMap<>();

        for (int i = 0; i < C1.length; i ++) {
            for (int j = 0; j < C2.length; j ++) {
                float amp = C1.A(i) * C2.A(j) / 2;

                int ni = C1.N(i).n;
                int nj = C2.N(j).n;

                int nhi = hi(ni, nj);
                Float ahi = map.get(nhi);
                if (ahi == null) ahi = (float)0.0;
                ahi += amp;
                if (Math.abs(ahi) < eps) map.remove(nhi);
                else map.put(nhi, ahi);

		if (ni == nj) continue;	// squaring a note produces cos(a)+1. Ignore the +1

                int nlo = lo(ni, nj);
                Float alo = map.get(nlo);
                if (alo == null) alo = (float)0.0;
                alo += amp;
                if (Math.abs(alo) < eps) map.remove(nlo);
                else map.put(nlo, alo);
            }
        }

        return new Chord(map);
    }

    private int hi(double ni, double nj) {
        double arg = Math.pow(2,ni/12) + Math.pow(2,nj/12);
        return (int)Math.round(12 * Math.log(arg)/Math.log(2));
    }

    private int lo(double ni, double nj) {
        double arg = Math.pow(2,ni/12) - Math.pow(2,nj/12);
	arg = Math.abs(arg);	// this is allowed because cos(a) = cos(-a)
        return (int)Math.round(12 * Math.log(arg)/Math.log(2));
    }







    public Phrase sum(Phrase... Ps) {
        if (! Phrase.sameKey(Ps))
            throw new IllegalArgumentException("Cannot mix phrases of different key");
        if (! Phrase.sameTempo(Ps))
            throw new IllegalArgumentException("Cannot mix phrases of different tempo");
        if (! Phrase.sameDuration(Ps))
            throw new IllegalArgumentException("Cannot mix phrases of different duration");

        int cpb = Phrase.lcm_cpb(Ps);

        Chord[] sums = new Chord[cpb/Ps[0].cpb * Ps[0].length];
        for (int i = 0; i < sums.length; i ++) {
            Chord[] addends = new Chord[Ps.length];
            for (int j = 0; j < Ps.length; j ++) {
                addends[j] = Ps[j].C(i / (cpb/Ps[j].cpb));
            }
            sums[i] = sum(addends);
        }

        return new Phrase(sums, Ps[0].key, Ps[0].bpm, cpb);
    }

    public Phrase magnify(float A, Phrase P) {
        Chord[] mags = new Chord[P.length];
        for (int i = 0; i < P.length; i ++) {
            mags[i] = magnify(A, P.C(i));
        }
        return new Phrase(mags, P.key, P.bpm, P.cpb);
    }

    public Phrase product(Phrase P1, Phrase P2) {
        if (! Phrase.sameKey(P1, P2))
            throw new IllegalArgumentException("Cannot mix phrases of different key");
        if (! Phrase.sameTempo(P1, P2))
            throw new IllegalArgumentException("Cannot mix phrases of different tempo");
        if (! Phrase.sameDuration(P1, P2))
            throw new IllegalArgumentException("Cannot mix phrases of different duration");

        int cpb = Phrase.lcm_cpb(P1, P2);

        Chord[] products = new Chord[cpb/P1.cpb * P1.length];
        for (int i = 0; i < products.length; i ++) {
            Chord C1 = P1.C(i / (cpb/P1.cpb));
            Chord C2 = P2.C(i / (cpb/P2.cpb));
            products[i] = product(C1, C2);
        }

        return new Phrase(products, P1.key, P1.bpm, cpb);
    }

    public Phrase inverse(Phrase P) {
        Chord[] inverse = new Chord[P.length];
        for (int i = 0; i < P.length; i ++) {
            inverse[i] = inverse(P.C(i));
        }
        return new Phrase(inverse, P.key, P.bpm, P.cpb);
    }

}
