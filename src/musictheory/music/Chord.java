package musictheory.music;

import java.util.Map;
import java.util.Arrays;

/**
 * Models a chord as a list of notes in the chord, accompanied by the relative dynamics of each note
 */
public class Chord {
    private Note[] notes;
    private float[] amps; // [0,1]
    /** the number of notes occurring in the Chord */
    public final int length;

    /**
     * Construct your Chord from a specially formatted string:
     * each note is separated by whitespace, and appears as XXXX:YY
     * XXXX is any valid string argument for the Note(String) constructor (see Note class for documentation)
     * YY is a parsable float value representing the relative dynamic
     *
     * The :YY may be omitted, in which case the relative dynamic defaults to 1.0
     */
    public Chord(String str) {
        String[] map = str.split("\\s+");

        if (str.isEmpty()) {
            this.length = 0;
        } else {
            this.length = map.length;
        }

        this.notes = new Note[length];
        this.amps = new float[length];

        // if str was empty, we're already finished
        for (int i = 0; i < length; i ++) {
            String note = map[i];
            float amp = (float)1.0;
            // ':' is optional - amplitude defaults to 1
            int c = map[i].indexOf(":"); // index of ':'
            if (c > -1) {
                amp = Float.parseFloat(note.substring(c+1));
                note = note.substring(0,c);
            }

            notes[i] = new Note(note);
            amps[i] = amp;
        }
    }

    /**
     * Construct your Chord from a Map whose keys are "n"-values (see Note class) and whose values are the relative dynamic
     */
    public Chord(Map<Integer,Float> map) {
        this.length = map.size();
        this.notes = new Note[length];
        this.amps = new float[length];

        int i = 0;
        for (int n: map.keySet()) {
            notes[i] = new Note(n);
            amps[i] = map.get(n);
            i += 1;
        }
    }

    /**
     * Construct your chord from parallel arrays of Note objects (notes) and their corresponding relative dynamics (amps)
     */
    public Chord(Note[] notes, float[] amps) {
        this.length = notes.length;
        this.notes = Arrays.copyOf(notes, length);
        this.amps = Arrays.copyOf(amps, length);
    }

    /**
     * Fetch the i-th note in this Chord
     *
     * Notes are NOT ordered; this is only really useful for iterating through all Notes (w/ Chord.length)
     */
    public Note N(int i) {
        return notes[i];
    }

    /**
     * Fetch the relative dynamic of the i-th note in this Chord
     */
    public float A(int i) {
        return amps[i];
    }

    /**
     * Produces the specially formatted String described in the constructor Chord(String)
     */
    public String toString() {
        String ret = "";
        for (int i = 0; i < length; i ++) {
            ret += notes[i].toString() + ":" + amps[i];
            if (i + 1 < length) ret += "\t";
        }
        return ret;
    }

    /**
     * Chords are "equal" if they consist of the same notes, with the EXACT same relative dynamics,
     * in the SAME INTERNAL ORDER. Note this last condition means conceptually identical chords will not
     * necessarily be "equal" here.
     */
    public boolean equals(Object o) {
        if (! (o instanceof Chord)) return false;
        Chord other = (Chord)o;
        if (length != other.length) return false;

        for (int i = 0; i < length; i ++) {
            if (notes[i].n != other.notes[i].n) return false;
            if (amps[i] != other.amps[i]) return false;
        }

        return true;
    }

    /**
     * Returns a new Chord with the relative dynamic 1.0, or 0.0 if it is currently less than eps
     */
    public Chord normalize(float eps) {
        Note[] notes = new Note[length];
        float[] amps = new float[length];
        for (int i = 0; i < length; i ++) {
            notes[i] = this.notes[i];
            if (Math.abs(this.amps[i]) < eps) amps[i] = (float)0.0;
            else amps[i] = (float)1.0;
        }
        return new Chord(notes, amps);
    }

    /**
     * Transpose a chord up by k half steps
     *
     * For example, the chord (C-E-G) shifted with k=2 becomes (D-F#-A)
     */
    public Chord shift(int k) {
        Note[] notes = new Note[length];
        float[] amps = new float[length];
        for (int i = 0; i < length; i ++) {
            notes[i] = new Note(this.notes[i].n + k);
            amps[i] = this.amps[i];
        }
        return new Chord(notes, amps);
    }
}
