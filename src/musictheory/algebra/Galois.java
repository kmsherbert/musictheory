package musictheory.algebra;

import java.util.ArrayList;
import java.util.HashMap;

import org.jlinalg.polynomial.Polynomial;
import org.jlinalg.polynomial.PolynomialFactory;
import org.jlinalg.polynomial.PolynomialLongDivisionResult;
import org.jlinalg.Matrix;
import org.jlinalg.Vector;

import musictheory.music.Phrase;
import musictheory.music.Chord;
import musictheory.music.Note;

/**
 * Algebra implementing the Galois Algebra described in report.pdf
 *
 * Also provides some convenience methods for working with polynomials.
 * Most of these are static, but since picking M such that P1=~P2 depends on a,
 * calling "irreducibleFor(P1,P2)" requires an already-created class. You may
 * want to save your polynomials somehow to avoid having to regenerate them
 * every time.
 */
public class Galois implements Algebra {
    // Polynomial constructors weren't written very well.
    // It seems I need to call this method, but never really use it...
    private static final PolynomialFactory<F2M12> CO_FACTORY =
            PolynomialFactory.getFactory(F2M12.FACTORY);

    private static final int default_o = 4; // default octave
    /** the default generator of GF(2^12) to use */
    public static final F2M12 default_a = F2M12.FACTORY.get(new int[]{1,1, 0, 1});

    /** the element this instance uses for generating GF(2^12) */
    public final F2M12 a;           // generator
    private ArrayList<F2M12> a_;    // powers of a

    /** the characteristic polynomial M to use in calculating inverses */
    public final Polynomial<F2M12> M;   // characteristic polynomial

    /**
     * Constructs a GaloisAlgebra using default_a for the generator, and omits M
     * Algebras constructed like so can't be used to calculate inverses.
     */
    public Galois() {
        this(null);
    }

    /**
     * Define M explicitly (uses default_a for the generator)
     */
    public Galois(Polynomial<F2M12> M) {
        this(default_a, M);
    }

    /**
     * Define both M and a explicitly
     */
    public Galois(F2M12 a, Polynomial<F2M12> M) {
        this.a = a;
        this.M = M;

        this.a_ = new ArrayList<>();
        a_.add(F2M12.FACTORY.one());
    }

    private F2M12 a_(int i) {
        if (i < a_.size()) return a_.get(i);
        // dynamically expand powers of a stored in memory
        for (int j = a_.size(); j <= i; j ++) {
            a_.add( a_.get(j-1).multiply(a) );
        }
        return a_.get(i);
    }

    private Matrix<F2M12> Hi(int t) {
        Matrix<F2M12> H = new Matrix<>(t, t, F2M12.FACTORY);
        for (int i = 0; i < t; i ++) {
            for (int j = 0; j < t; j ++) {
                H.set(i+1,j+1, a_(i*j));
            }
        }
        return H.inverse(); // this takes a little time, not much
    }





    public Phrase sum(Phrase... Ps) {
        if (! Phrase.sameKey(Ps))
            throw new IllegalArgumentException("Cannot mix phrases of different key");
        if (! Phrase.sameTempo(Ps))
            throw new IllegalArgumentException("Cannot mix phrases of different tempo");

        Polynomial<F2M12> sum = new Polynomial<F2M12>(F2M12.FACTORY.zero());

        for (Phrase P: Ps) sum = sum.add(asPolynomial(P));

        return asPhrase(sum, Ps[0].key, Ps[0].bpm, Ps[0].cpb);
    }

    public Phrase magnify(float A, Phrase P) {
        return P;   // this doesn't in fact do anything
    }

    public Phrase product(Phrase P1, Phrase P2) {
        if (! Phrase.sameKey(P1, P2))
            throw new IllegalArgumentException("Cannot mix phrases of different key");
        if (! Phrase.sameTempo(P1, P2))
            throw new IllegalArgumentException("Cannot mix phrases of different tempo");

        int cpb = Phrase.lcm_cpb(P1, P2);
        P1 = P1.expand(cpb);
        P2 = P2.expand(cpb);

        Polynomial<F2M12> F = asPolynomial(P1).multiply(asPolynomial(P2));
        return asPhrase(F, P1.key, P1.bpm, cpb);
    }

    public Phrase inverse(Phrase P) {
        if (M == null) throw new UnsupportedOperationException("This Galois has no M");
        Polynomial<F2M12> F = asPolynomial(P);
        Polynomial<F2M12> x = F.longDivision(M).getRemainder();
        Polynomial<F2M12> xi = inverse(x);
        // let F' = q*M + x', where q*M = F - x. In other words, F' = F + dx
        Polynomial<F2M12> I = F.add( xi.subtract(x) );
        // result will be same length as P: only degrees < M change,
        //      but this changes the entire chord sequence

        return asPhrase(I, P.key, P.bpm, P.cpb);
    }








    /*
     *  POLYNOMIAL FUNCTIONS
     */

    private Polynomial<F2M12> inverse(Polynomial<F2M12> a) {
        Polynomial<F2M12> xp = new Polynomial<>(F2M12.FACTORY.zero());
        Polynomial<F2M12> x = new Polynomial<>(F2M12.FACTORY.one());

        PolynomialLongDivisionResult<F2M12> divmod = M.longDivision(a);
        while (! divmod.getRemainder().isZero()) {
            Polynomial<F2M12> temp = x;
            x = xp.subtract( divmod.getQuotient().multiply(x) );    // x = xp - q*x
            xp = temp;
            temp = divmod.getRemainder();
            divmod = a.longDivision(temp);
            a = temp;
        }

        // if last non-zero remainder is non-scalar, there is no inverse
        if (a.getDegree() != 0) return null;
        // normalize last non-zero remainder
        x = x.multiply( new Polynomial<F2M12>(a.getHighestCoefficient().invert()) );
        return x;
    }

    private F2M12 evaluate(Polynomial<F2M12> F, int i) {
        // evaluates F[a^i]
        F2M12 Y = F2M12.FACTORY.zero();
        while (! F.isZero()) {
            Y = Y.add(F.getHighestCoefficient().multiply(a_(i*F.getDegree())));
            F = F.withoutHighestPower();
        }
        return Y;
    }





    /*
     *  FINDING AN IRREDUCIBLE POLYNOMIAL
     */

    /**
     * Gives a polynomial which guarantees that, when used as M with a GaloisAlgebra with the same generator a, P1 = ~P2 and P2 = ~P1
     */
    public Polynomial<F2M12> irreducibleFor(Phrase P1, Phrase P2) {
        // this is NOT a static method
        // to use, you need to create the Galois Algebra,
        //  ask for the appropriate irreducible,
        //  save it, then start over and create the Galois with it
        if (! Phrase.sameKey(P1, P2))
            throw new IllegalArgumentException("Cannot mix phrases of different key");
        if (! Phrase.sameTempo(P1, P2))
            throw new IllegalArgumentException("Cannot mix phrases of different tempo");
        if (P1.cpb != P2.cpb)
            throw new IllegalArgumentException(
                "Phrases have different chords/beat, so the results would not make sense."
                    + "\nExpand phrases to their lcm_cpb value before using this method."
            );

        Polynomial<F2M12> X = asPolynomial(P1).multiply(asPolynomial(P2));
        X = X.subtract(new Polynomial<F2M12>(F2M12.FACTORY.one()));

        // user may wish to factor X if possible, but that's their problem
        return X;
    }

    /**
     * Returns a random irreducible polynomial over GF(2^12) of degree t.
     * Warning: this function involves randomly generating a polynomial, then testing whether it is irreducible. It can take some time, and reports its progress in System.out
     */
    public static Polynomial<F2M12> randomIrreducible(int t) {
        Polynomial<F2M12> F = randomPolynomial(t);
        // System.out.println("*** Trying:\n\t"+F);
        int n = 0;
        while (! isirreducible(F)) { // may take time
            F = randomPolynomial(t);
            System.out.print((++n)+"\t");
            // System.out.println("*** Trying:\n\t"+F);
        }
        System.out.println("\nTook "+n+" attempts.");
        return F;
    }

    /**
     * Returns a random polynomial over GF(2^12) of degree t
     */
    private static Polynomial<F2M12> randomPolynomial(int t) {
        HashMap<Integer,F2M12> map = new HashMap<>();
        for (int i = 0; i < t; i ++) {
            map.put(i, F2M12.FACTORY.randomValue());
        }
        map.put(t, F2M12.FACTORY.one());
        return new Polynomial<>(map, F2M12.FACTORY);
    }

    /**
     * Returns true iff F is an irreducible polynomial over GF(2^12)
     */
    public static boolean isirreducible(Polynomial<F2M12> F) {
        // insofar as scalars are...scalar, p is irreducible . . . I guess
        if (F.getDegree() == 0) return true;
        // insofar as the leading coefficient can be factored out, p is not irreducible
        if (!F.getHighestCoefficient().isOne()) return false;

        for (int i = 1; i <= F.getDegree()/2; i ++) {
            HashMap<Integer,F2M12> map = new HashMap<>();
            map.put(1, F2M12.FACTORY.one());
            Polynomial<F2M12> x = new Polynomial<>(map, F2M12.FACTORY);

            // if the gcd isn't 1, then F isn't irreducible
            if (F.gcd(pow2to(x,12*i,F).subtract(x)).getDegree() > 0) return false;

        }
        return true;
    }

    private static Polynomial<F2M12> pow2to(Polynomial<F2M12> P, int e, Polynomial<F2M12> F) {
        // finds P^(2^e) mod F
        for (int i = 0; i < e; i ++) {
            P = P.multiply(P);
            P = P.longDivision(F).getRemainder();
        }
        return P;
    }

    private static Polynomial<F2M12> fmod(Polynomial<F2M12> a, Polynomial<F2M12> b) {
        int deg_a = a.getDegree();
        int deg_b = b.getDegree();
        // base case: a is smaller than b
        if (deg_a < deg_b) return a;

        // k is the number of times deg_b can be mutliplied by 2 and still be under deg_a
        int k = (int)(Math.log(deg_a/deg_b)/Math.log(2)); // aka log2(deg_a) - log2(deg_b)
        // p is that actual power of k
        int p = 1;
        for (int i = 1; i <= k; i ++) p *= 2;
        // d is the remaing difference in degrees after raising b ^ (2^k)
        int d = deg_a - deg_b*p;

        // product needed to eliminate leading coefficient from a
        F2M12 q = a.getHighestCoefficient().divide(b.getHighestCoefficient().pow(p));

        HashMap<Integer,F2M12> map = new HashMap<>();
        Polynomial<F2M12> bt = b;   // access each term in b by successive truncations
        for (int i = deg_b; i >= 0; i --) {
            if (bt.getDegree() < i) continue;   // b has no x^i term
            // since p is a power of 2 (characteristic of F2M12),
            // b^p has each coefficient bi^p (Frobenius automorphism)
            map.put(i*p + d, q.multiply(bt.getHighestCoefficient().pow(p)));
            bt = bt.withoutHighestPower();
        }
        Polynomial<F2M12> bp = new Polynomial<>(map, F2M12.FACTORY);
        // bp is a product of b, and thus a - bp the same modulus
        return fmod(a.subtract(bp), b);
    }



    /*
     *  MUSIC <=> MATH FUNCTIONS
     */

    private F2M12 asCoefficient(Chord C, int k) {
        boolean[] co = new boolean[12];

        for (int i = 0; i < C.length; i ++) {
            int f = Note.f(C.N(i).p, k);
            co[f] = co[f] || C.A(i) > 0;
        }

        return new F2M12(co);
    }

    private Chord asChord(F2M12 X, int k) {
        // first get number of sounded notes
        int wt = 0;
        for (int i = 0; i < 12; i ++) if (X.co(i)) wt ++;

        Note[] notes = new Note[wt];
        float[] amps = new float[wt];

        int j = 0; // track index of notes and amps
        for (int i = 0; i < 12; i ++) {
            if (X.co(i)) {
                notes[j] = new Note(default_o, Note.p(i,k));
                amps[j] = (float)1.0;
                j ++;
            }
        }

        return new Chord(notes, amps);
    }

    private Polynomial<F2M12> asPolynomial(Phrase P) {
        Vector<F2M12> C = new Vector<>(P.length, F2M12.FACTORY);

        for (int i = 0; i < P.length; i ++) {
            C.set(i+1, asCoefficient(P.C(i), P.key));
        }

        Vector<F2M12> V = Hi(C.length()).multiply(C);

        // convert to map for polynomial construction
        HashMap<Integer, F2M12> coef = new HashMap<>();
        for (int i = 0; i < V.length(); i ++) {
            coef.put(i, V.getEntry(i+1));
        }

        return new Polynomial<>(coef, F2M12.FACTORY);
    }

    private Phrase asPhrase(Polynomial<F2M12> F, int key, int bpm, int cpb) {
        Chord[] chords = new Chord[F.getDegree()+1];

        for (int i = 0; i < chords.length; i ++) {
            chords[i] = asChord(evaluate(F, i), key);
        }

        return new Phrase(chords, key, bpm, cpb);
    }

}
