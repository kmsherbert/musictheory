package musictheory.algebra;

import java.util.ArrayList;

import org.jlinalg.LinSysSolver;
import org.jlinalg.Matrix;
import org.jlinalg.Vector;
import org.jlinalg.doublewrapper.DoubleWrapper;

import musictheory.music.Note;
import musictheory.music.Chord;
import musictheory.music.Phrase;

/**
 * Algebra implementing the Linear Algebra described in report.pdf
 *
 * This implementation is heavily dependent on JLinAlg (jlinalg.sourceforge.net)
 */
public class Linear implements Algebra {
    private static final int default_o = 4; // default octave
    private static final float default_eps = (float).001;
    private static final int default_pmax = 200;
    private static final float default_peps = (float).0001;

    private DoubleWrapper eps;
    private int pmax;
    private DoubleWrapper peps;

    /**
     * Define all numerical parameters
     * @param eps - ignore any note with amplitude (relative dynamic) less than eps
     * @param pmax - eigenvector power iteration will iterate up to pmax times before giving up (relevant for ~P only)
     * @param peps - ignore any eigenvalue less than peps (relevant for ~P only)
     */
    public Linear(float eps, int pmax, float peps) {
        super();
        this.eps = new DoubleWrapper(eps);
        this.pmax = pmax;
        this.peps = new DoubleWrapper(peps);
    }

    /**
     * Use default parameters
     * @param eps = .0001
     * @param pmax = 200
     * @param peps = .0001
     */
    public Linear() {
        this(default_eps, default_pmax, default_peps);
    }

    public Phrase sum(Phrase... Ps) {
        if (! Phrase.sameKey(Ps))
            throw new IllegalArgumentException("Cannot mix phrases of different key");
        if (! Phrase.sameTempo(Ps))
            throw new IllegalArgumentException("Cannot mix phrases of different tempo");
        if (! Phrase.sameDuration(Ps))
            throw new IllegalArgumentException("Cannot mix phrases of different duration");

        int cpb = Phrase.lcm_cpb(Ps);

        Matrix<DoubleWrapper> M = asMatrix(Ps[0].expand(cpb));
        for (int i = 1; i < Ps.length; i ++) {
            M.addReplace(asMatrix(Ps[i].expand(cpb)));
        }

        return asPhrase(M, Ps[0].key, Ps[0].bpm, cpb);
    }

    public Phrase magnify(float A, Phrase P) {
        Matrix<DoubleWrapper> M = asMatrix(P);
        M.multiplyReplace(new DoubleWrapper(A));
        return asPhrase(M, P.key, P.bpm, P.cpb);
    }

    public Phrase product(Phrase P1, Phrase P2) {
        if (! Phrase.sameKey(P1, P2))
            throw new IllegalArgumentException("Cannot mix phrases of different key");
        if (! Phrase.sameTempo(P1, P2))
            throw new IllegalArgumentException("Cannot mix phrases of different tempo");
        if (! Phrase.sameDuration(P1, P2))
            throw new IllegalArgumentException("Cannot mix phrases of different duration");

        int cpb = Phrase.lcm_cpb(P1, P2);

        Matrix<DoubleWrapper> M = asMatrix(P1.expand(cpb));
        M = M.multiply(asMatrix(P2.expand(cpb)).transpose());

        return asPhrase(M, P1.key, P1.bpm, cpb);
    }

    public Phrase inverse(Phrase P) {
        Matrix<DoubleWrapper> X = asMatrix(P);
        Matrix<DoubleWrapper> Xt = X.transpose();
        Matrix<DoubleWrapper> XXt = X.multiply(Xt);
        Matrix<DoubleWrapper> XtX = Xt.multiply(X);

        Matrix<DoubleWrapper> L = eigenMatrix(XXt);
        Matrix<DoubleWrapper> D = deltaMatrix(XXt, L);
        Matrix<DoubleWrapper> R = eigenMatrix(XtX);

        // each singular vector pair (l, r) is found independently, so
        //      the sign may be flipped from the correct decomposition
        // this is equivalent to having a negative singular value,
        //      so we'll correct by selecting the most accurate D
        //      from the combinations of negating each one
        int best_i = -1;
        double best_diff = Double.MAX_VALUE;
        for (int i = 0; i < Math.pow(2,D.getRows()); i ++) {
            Matrix<DoubleWrapper> SVD =
                    L.multiply(D_pattern(D, i)).multiply(R.transpose());
            double diff = diff(X, SVD);

            if (diff < best_diff) {
                best_i = i;
                best_diff = diff;
            }
        }
        // lock in with the ideal D
        D = D_pattern(D, best_i);

        Matrix<DoubleWrapper> It = L.multiply(D.inverse()).multiply(R.transpose());

        return asPhrase(It, P.key, P.bpm, P.cpb);
    }


    private Matrix<DoubleWrapper> clean(Matrix<DoubleWrapper> M) {
        Matrix<DoubleWrapper> X = M.copy();
        for (int i = 0; i < M.getRows(); i ++) {
            for (int j = 0; j < M.getCols(); j ++) {
                double v = M.get(i+1,j+1).getValue();
                v = Math.round(v*10)/10.0;
                X.set(i+1,j+1, DoubleWrapper.FACTORY.get(v));
            }
        }
        return X;
    }

    private boolean equal(Matrix<DoubleWrapper> M1, Matrix<DoubleWrapper> M2) {
        Matrix<DoubleWrapper> D = M1.subtract(M2);
        D = D.arrayMultiply(D);
        D = D.gt(eps);

        Matrix<DoubleWrapper> Z = new Matrix<>(
            D.getRows(), D.getCols(), DoubleWrapper.FACTORY
        );
        Z.setAll(DoubleWrapper.FACTORY.zero());

        return D.equals(Z);
    }

    private Matrix<DoubleWrapper> D_pattern(Matrix<DoubleWrapper> D, int k) {
        boolean[] repr = binary(k, D.getRows());

        Matrix<DoubleWrapper> M = D.copy();
        for (int i = 0; i < repr.length; i ++) {
            if (repr[i]) {
                DoubleWrapper d = M.get(i+1, i+1);
                M.set(i+1,i+1, d.multiply(DoubleWrapper.FACTORY.m_one()));
            }
        }

        return M;
    }

    private boolean[] binary(int i, int n) {
        boolean[] repr = new boolean[n];
        for (int j = repr.length-1; j >= 0; j --) {
            repr[j] = (i & 1) > 0;  // check if i is odd
            i /= 2;
        }
        return repr;
    }

    private double diff(Matrix<DoubleWrapper> M1, Matrix<DoubleWrapper> M2) {
        Matrix<DoubleWrapper> M = M1.subtract(M2);
        double sum = 0;
        for (int i = 0; i < M.getRows(); i ++) {
            for (int j = 0; j < M.getCols(); j ++) {
                sum += Math.abs(M.get(i+1,j+1).getValue());
            }
        }
        return sum;
    }







    private Vector<DoubleWrapper> powerIteration(Matrix<DoubleWrapper> M, int i, int r) {
        int n = M.getRows();
        Vector<DoubleWrapper> x0 = new Vector<>(n, DoubleWrapper.FACTORY);
        // uniform initialization
        x0.setAll(DoubleWrapper.FACTORY.one());
        x0.divideReplace(x0.L2Norm());

        return powerIteration(M, x0, i,r);
    }

    private Vector<DoubleWrapper> powerIteration(
        Matrix<DoubleWrapper> M, Vector<DoubleWrapper> x0, int i, int rank
    ) {
        Vector<DoubleWrapper> x = x0;
        Vector<DoubleWrapper> xp;

        int cnt = 0;
        DoubleWrapper dist = peps;
        while (dist.ge(peps)) {
            xp = M.multiply(x);             // propagate
            xp.divideReplace(xp.L2Norm());  // normalize
            dist = xp.distance(x);          // check error

            x = xp;                         // iterate
            if (++cnt > pmax) break;        // insure against orthogonal x0
        }

        DoubleWrapper eig = eigenvalue(M,x);

        // check if we failed to converge, or converged on the wrong vector
        if (cnt > pmax || eig.lt(eps)) {
            // throw new RuntimeException("Increase your pmax or decrease your peps!" +"\n"+
            //     "Convergence "+dist+" after "+pmax+" iterations.");
            System.err.println("Passed "+pmax+" iterations:");
            System.err.println("Singular vector "+i+"/"+rank);
//            System.err.println("\tx0:\t"+x0);
            System.err.println("\tD:\t"+dist);
            System.err.println("\tl:\t"+eig);
            System.err.println("\tRestarting with new x0:");
            Vector<DoubleWrapper> r = randomOrthonormal(x0);
//            System.out.println(r);
            return powerIteration(M, r, i,rank);
        }
        // System.out.println("Power iteration took "+cnt+" iterations.");

        // System.out.println("Iteration successful: "+eig);
        return x;
    }

    private DoubleWrapper eigenvalue(Matrix<DoubleWrapper> M, Vector<DoubleWrapper> E) {
        return E.multiply( M.multiply(E) );
    }

    private Matrix<DoubleWrapper> eigenMatrix(Matrix<DoubleWrapper> M) {
        // Vector<DoubleWrapper> V = new Vector<>(M.eig(), DoubleWrapper.FACTORY);
        // // rank is # of eigenvalues greater than "zero"
        // int r = (int)(V.gt(eps).sum().getValue());
        int r = M.rank();

        Matrix<DoubleWrapper> X = new Matrix<>(M.getRows(), r, DoubleWrapper.FACTORY);

        for (int i = 0; i < r; i ++) {
            Vector<DoubleWrapper> E = powerIteration(M, i,r);
            DoubleWrapper eig = eigenvalue(M,E);

            X.setCol(i+1, E);
            M = M.subtract( E.transposeAndMultiply(E).multiply(eig) );
        }

        return X;
    }

    private Matrix<DoubleWrapper> deltaMatrix(
        Matrix<DoubleWrapper> M, Matrix<DoubleWrapper> E
    ) {
        int r = E.getCols();

        Double[][] zeros = new Double[r][r];
        for (int i = 0; i < r; i ++) for (int j = 0; j < r; j ++) zeros[i][j] = 0.0;
        Matrix<DoubleWrapper> D = new Matrix<>(zeros, DoubleWrapper.FACTORY);

        for (int i = 0; i < r; i ++) {
            DoubleWrapper eig = eigenvalue(M, E.getCol(i+1));
            D.set(i+1, i+1, eig.sqrt());
        }

        return D;
    }

    private Vector<DoubleWrapper> randomOrthonormal(Vector<DoubleWrapper> V) {
        int n = V.length();
        Vector<DoubleWrapper> R = new Vector<>(n, DoubleWrapper.FACTORY);
        for (int i = 0; i < n-1; i ++) R.set(i+1, DoubleWrapper.FACTORY.randomValue());
        R.set(n, DoubleWrapper.FACTORY.zero());

        R.set(n, R.multiply(V).divide(V.getEntry(n)).negate() );
        // technically we should iterate through until we find a non-zero element of V

        R.divideReplace(R.L2Norm()); // normalize

        return R;
    }

















    private Vector<DoubleWrapper> asVector(Chord C, int k) {
        Double[] amps = new Double[12];
        for (int i = 0; i < amps.length; i ++) amps[i] = 0.0;

        for (int i = 0; i < C.length; i ++) {
            amps[Note.f(C.N(i).p, k)] += C.A(i);
        }

        return new Vector<DoubleWrapper>(amps, DoubleWrapper.FACTORY);
    }

    private Chord asChord(Vector<DoubleWrapper> V, int k) {
        // find weight of vector: # of elements greater than "zero"
        int wt = (int)(V.gt(eps).sum().getValue());

        // also account for valid negative numbers!
        Vector<DoubleWrapper> mV = V.multiply(DoubleWrapper.FACTORY.m_one());
        wt += (int)(mV.gt(eps).sum().getValue());

        Note[] notes = new Note[wt];
        float[] amps = new float[wt];

        int j = 0; // track index of notes and amps
        for (int i = 0; i < V.length(); i ++) {
            if (V.getEntry(i+1).gt(eps) || mV.getEntry(i+1).gt(eps)) {
                notes[j] = new Note(default_o, Note.p(i,k));
                amps[j] = (float)V.getEntry(i+1).getValue();
                j ++;
            }
        }

        return new Chord(notes, amps);
    }


    private Matrix<DoubleWrapper> asMatrix(Phrase P) {
        Vector<DoubleWrapper>[] chords = new Vector[P.length];
        for (int i = 0; i < chords.length; i ++) {
            chords[i] = asVector(P.C(i), P.key);
        }

        Matrix<DoubleWrapper> M = new Matrix<>(chords);
        return M.transpose();
    }

    private Phrase asPhrase(Matrix<DoubleWrapper> M, int key, int bpm, int cpb) {
        Chord[] chords = new Chord[M.getCols()];

        for (int i = 0; i < chords.length; i ++) {
            chords[i] = asChord(M.getCol(i+1), key);
        }

        Phrase P = new Phrase(chords, key, bpm, cpb);
        return P.compress();
    }

}
