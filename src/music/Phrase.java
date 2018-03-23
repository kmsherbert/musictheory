package music;

import java.io.PrintStream;

import java.util.Scanner;
import java.util.ArrayList;

/**
 * Models a phrase as a sequence of Chords, with various accompanying metadata (see public field documentation)
 */
public class Phrase {
    private Chord[] chords;
    /**
     * numerical pitch of the key of this phrase.
     * We assume all key changes happen between phrases...
     */
    public final int key; // pitch of key signature, were it major
    /**
     * the tempo for this phrase, in beats per minute.
     * We make no attempt to model tempo changes...
     */
    public final int bpm; // beats/minute
    /**
     * The number of "chords" in each beat.
     *
     * Each chord is taken to have the same duration, so each conceptual chord
     * will likely be broken into many "chords", depending on the
     * shortest-duration chord in the phrase
     */
    public final int cpb; // chords/beat

    /**
     * the number of chords in the phrase
     *
     * Get the number of beats by length/cpb
     */
    public final int length; // chords
    /**
     * the duration of each chord, in seconds
     */
    public final float spc; // sec/chord
    /**
     * the duration of the entire phrase, in seconds
     */
    public final float duration; // sec

    /**
     * Define your phrase from a sequence of chords, explicitly defining key, bpm, and cpb
     */
    public Phrase(Chord[] chords, int key, int bpm, int cpb) {
        this.chords = chords;
        this.key = key; // key signature
        this.bpm = bpm; // beats/minute
        this.cpb = cpb; // chords/beat

        this.length = chords.length; // chords
        this.spc = (float)(60.0 / bpm / cpb); // sec/chord
        this.duration = spc * length; // sec
    }

    /**
     * Define your phrase from a Scanner reading through a specially formatted string:
     */
    public Phrase(Scanner in) {
        // find key and bpm from header
        this.key = Note.p(in.nextLine());
        this.bpm = Integer.parseInt(in.nextLine());

        // load rest of file, and find cpb
        int cpb = 1;
        ArrayList<Integer> nums = new ArrayList<>();
        ArrayList<Integer> dens = new ArrayList<>();
        ArrayList<Chord> crds = new ArrayList<>();
        while(in.hasNextLine()) {
            String line = in.nextLine();

            if (line.startsWith("#")) continue; // skip comments

            String[] mapping = line.split("\\s+", 2);
            String beat = mapping[0];
            String chord = "";
            if (mapping.length == 2) chord = mapping[1];

            int num; // numerator
            int den = 1; // denominator
            // '/' is optional - denominator defaults to 1
            int c = beat.indexOf("/"); // index of '/'
            if (c > -1) {
                num = Integer.parseInt(beat.substring(0,c));
                den = Integer.parseInt(beat.substring(c+1));
            } else {
                num = Integer.parseInt(beat);
                // den is already set
            }

            nums.add(num);
            dens.add(den);
            crds.add(new Chord(chord));

            // cpb is the least common multiple of all denominators
            int i = 1;
            while ( (cpb*i) % den > 0) i ++;
            cpb *= i;
        }
        this.cpb = cpb;

        // find length of chords
        int length = 0;
        ArrayList<Integer> lens = new ArrayList<>();
        for (int i = 0; i < nums.size(); i ++) {
            int l = nums.get(i) * cpb / dens.get(i);
            length += l;
            lens.add(l);
        }
        this.length = length;
        this.spc = (float)(60.0 / bpm / cpb); // sec/chord
        this.duration = spc * length; // sec
        this.chords = new Chord[length];

        // fill chords
        int c = 0; // index of chords array
        for (int i = 0; i < nums.size(); i ++) {
            for (int j = 0; j < lens.get(i); j ++) {
                chords[c++] = crds.get(i);
            }
        }
    }

    /**
     * Write this Phrase to the specially formatted String described in the constructor Phrase(String)
     */
    public void write(PrintStream out) {
        // print header
        out.println(Note.notes[key]);
        out.println(bpm);

        for (int i = 0; i < length; i ++) {
            out.print("1/"+cpb+"\t");
            out.println(C(i).toString());
        }
    }

    /**
     * Fetch the i-th Chord of this Phrase
     */
    public Chord C(int i) {
        return chords[i];
    }

    /**
     * Convert this phrase into a pressure-wave function
     * Note the misnomer: this function would better be called "amplitudes"
     * @param r - the sample rate, ie the number of samples per second of time
     */
    public double[] phases(float r) {
        int Npc = (int)(r * spc); // samples / chord
        int cap = Npc / 100; // which sample in a pulse to start decay
        // int cap = 35;

        double[] phases = new double[Npc * length];
        for (int i = 0; i < length; i ++) { // i iterates over chords
            Chord C = chords[i];

            for (int j = 0; j < Npc; j ++) { // j iterates over samples
                float t = j / r; // sec

                double phase = 0;
                for (int k = 0; k < C.length; k ++) { // k iterates over notes
                    Note N = C.N(k);
                    phase += C.A(k) * Math.sin(N.w * t);
                    // linearly modulate start and end of pulse
                    if (j < cap) phase *= (1.0*j)/cap;
                    if (Npc-j < cap) phase *= (Npc - 1.0*j)/cap;
                }
                phases[Npc*i+j] = phase;
            }
        }
        
        return phases;
    }

    /**
     * Returns a new Phrase, which is the transposition of this Phrase into another key, with each Chord shifted as necessary
     */
    public Phrase transpose(int key) {
        int d = key - this.key;
        Chord[] chords = new Chord[length];
        for (int i = 0; i < length; i ++) {
            chords[i] = this.chords[i].shift(d);
        }
        return new Phrase(chords, key, bpm, cpb);
    }

    /**
     * Returns a new Phrase, conceptually identical to this one, which replicates each Chord as necessary to set the chords per beat to cpb
     */
    public Phrase expand(int cpb) {
        if (cpb == this.cpb) return this;   // no expansion necessary

        if (cpb % this.cpb != 0) throw new IllegalArgumentException(
            "Could not expand: must be multiple of cpb="+this.cpb
        );

        int r = cpb/this.cpb;

        Chord[] chords = new Chord[length * r];
        for (int i = 0; i < chords.length; i ++) {
            chords[i] = this.chords[i/r];
        }

        return new Phrase(chords, key, bpm, cpb);
    }

    /**
     * Returns a new Phrase, conceptually identical to this one, which minimizes the number of times each Chord is replicated.
     *
     * Essentially, you can use compress() to undo a previous expand()
     */
    public Phrase compress() {
        if (length == 0) return this;   // no compression possible

        for (int r = cpb; r > 1; r --) {
            if (compressable(r)) {
                Chord[] chords = new Chord[length / r];
                for (int i = 0; i < chords.length; i ++) {
                    chords[i] = this.chords[i*r];
                }
                return new Phrase(chords, key, bpm, cpb/r);
            }
        }
        
        return this;    // no compression possible
    }

    private boolean compressable(int r) {
        // r must be a factor of both length and cpb
        if (length % r != 0) return false;
        if (cpb % r != 0) return false;
        // chords must be arrangeable in groups of r
        for (int i = 0; i < length; i += r) {
            for (int j = 1; j < r; j ++) {
                if (! chords[i].equals(chords[i+j])) return false;
            }
        }
        return true;
    }

    /**
     * Returns true iff all phrases passed in have the same key
     * Always pass at least one phrase
     */
    public static boolean sameKey(Phrase... Ps) {
        int key = Ps[0].key;
        for (Phrase P: Ps) if (key != P.key) return false;
        return true;
    }

    /**
     * Returns true iff all phrases passed in have the same bpm
     * Always pass at least one phrase
     */
    public static boolean sameTempo(Phrase... Ps) {
        int bpm = Ps[0].bpm;
        for (Phrase P: Ps) if (bpm != P.bpm) return false;
        return true;
    }

    /**
     * Returns true iff all phrases passed in have the same duration
     * Always pass at least one phrase
     */
    public static boolean sameDuration(Phrase... Ps) {
        float duration = Ps[0].duration;
        for (Phrase P: Ps) if (duration != P.duration) return false;
        return true;
    }

    /**
     * Returns the least common multiple of all passed Phrase's cpb values
     * Always pass at least one phrase
     */
    public static int lcm_cpb(Phrase... Ps) {
        int cpb = 1;

        for (int i = 0; i < Ps.length; i ++) {
            int k = 1;
            while ( (cpb*k) % Ps[i].cpb > 0) k ++;
            cpb *= k;
        }

        return cpb;
    }
}
