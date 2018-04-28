package musictheory.music;

/**
 * Models a "note" as a pitch (ex. "c") and an octave (ex. "4")
 */
public class Note {
    /**
     * human readable list of pitch, in order.
     * Does not include alternate names (ie. C# is in, Db is out)
     * C C# D Eb F F# G G# A Bb B
     */
    public static final String[] notes = {
        "C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B"
    };
    /** angular frequency of middle A pure tone */
    public static final float wA = (float)(2*Math.PI*440); // rad/sec
    /** "n"-value of middle A (see documentation for Note.n */
    public static final int nA = 57;

    /** numerical pitch (ie index) of human-readable pitch, starting from C */
    public static int p(String pitch) {
        switch(pitch) {
            case "C": return 0;
            case "C#":
            case "Db": return 1;
            case "D": return 2;
            case "D#":
            case "Eb": return 3;
            case "E": return 4;
            case "F": return 5;
            case "F#":
            case "Gb": return 6;
            case "G": return 7;
            case "G#":
            case "Ab": return 8;
            case "A": return 9;
            case "A#":
            case "Bb": return 10;
            case "B": return 11;
            default: throw new IllegalArgumentException("Invalid pitch: "+pitch);
        }
    }

    /**
     * Get numerical pitch (p) of f-th note in the circle of fifths from pitch k
     */
    public static int p(int f, int k) {
        return (7*f + k) % 12;
    }

    /**
     * Get index of the pitch p in the circle of fifths from pitch k
     */
    public static int f(int p, int k) {
        return 7*(p - k + 12) % 12;  // extra '12' ensures positiveness
    }

    /**
     * maps pitch and octave to a single number:
     * if octave is o and pitch is given numerically by p, n = 12*o + p
     */
    public final int n;
    /**
     * octave number. Middle C is defined as C4, and the number cycles at each C (so C4b = B3)
     */
    public final int o;
    /**
     * numerical pitch, ie. the index in a chromatic list of pitches starting with C
     */
    public final int p;
    /**
     * the angular frequency of a pure tone of this note
     */
    public final float w;

    /**
     * Construct a note from a human-readable pitch/octave (ex. "C4", "F#5", etc...)
     *
     * If not octave is provided, it defaults to octave 4
     */
    public Note(String str) {
        int i = 0; // index of first digit in str
        while (i < str.length() && !Character.isDigit(str.charAt(i))) i ++;

        // next two lines are liable to crash if str is badly formatted
        this.p = p(str.substring(0,i));

        if (i == str.length()) {
            this.o = 4; // default octave when not specified
        } else {
            this.o = Integer.parseInt(str.substring(i));
        }

        this.n = 12*o + p;
        this.w = (float)(Math.pow(2, (n - nA)/12.0) * wA);
    }

    /**
     * Construct a note directly from its n-value
     */
    public Note(int n) {
        this.n = n;
        this.o = n / 12;
        this.p = Math.abs(n % 12);	// Java allows negative %, we don't want that
        this.w = (float)(Math.pow(2, (n - nA)/12.0) * wA);
    }

    /**
     * Construct a note explicitly giving octave (o) and numerical pitch (p)
     */
    public Note(int o, int p) {
        this.n = 12*o + p;
        this.o = o;
        this.p = p;
        this.w = (float)(Math.pow(2, (n - nA)/12.0) * wA);
    }

    /** gives human readable pitch/octave (ex. "C4", "F#5", etc...) */
    public String toString() {
        return notes[p] + o;
    }
}
