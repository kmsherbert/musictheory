/**
 * A carbon copy of Player, lazily thrown together to generate .wav files instead of system sounds
 *
 * Really, this should not be a distinct class, and a lot of this work should be folded into private helper methods...
 */
package player;

import music.Phrase;
import music.Chord;
import music.Note;

import java.util.ArrayList;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFileFormat;

public class Writer {
    /**
     * the standard sample rate
     * This should REALLY not be its own variable...
     */
    public static float R = 44100; // samples / sec

    /**
     * write a bunch of phrases (played simultaneously) to the specified file, in WAV format
     * @param out - a validly opened file with write permissions
     * @param Ps - any positive number of phrases, to be "played" simultaneously
     */
    public static void write(File out, Phrase... Ps) throws IOException {
        if (! Phrase.sameKey(Ps))
            throw new IllegalArgumentException("Cannot mix phrases of different key");
        if (! Phrase.sameTempo(Ps))
            throw new IllegalArgumentException("Cannot mix phrases of different tempo");
        if (! Phrase.sameDuration(Ps))
            throw new IllegalArgumentException("Cannot mix phrases of different duration");

        ArrayList<double[]> ps = new ArrayList<>(Ps.length);
        for (int i = 0; i < Ps.length; i ++) {
            ps.add(Ps[i].phases(R));
        }

        double[] phases = new double[ps.get(0).length];
        // combine phases
        for (int i = 0; i < phases.length; i ++) {
            phases[i] = 0; // technically unnecessary in Java
            for (int j = 0; j < Ps.length; j ++) {
                phases[i] += ps.get(j)[i];
            }
        }

        write(out, phases);
    }

    /**
     * write a pressure wave array to the specified file, in WAV format
     * @param out - a validly opened file with write permissions
     * @param phases - misnomer: the sequence of amplitudes for the sound card to reproduce
     */
    public static void write(File out, double[] phases) throws IOException {
        double max = 0;
        // find peak phase, to normalize to max int
        for (int i = 0; i < phases.length; i ++) {
            if (phases[i] > max) max = phases[i];
        }

        int[] fn = new int[phases.length];
        for (int i = 0; i < phases.length; i ++) {
            fn[i] = (int)(phases[i] / max * Integer.MAX_VALUE);
        }

        write(out, fn);
    }

    /**
     * write a pressure wave array, normalized to Integer.MAX_VALUE, to the specified file, in WAV format
     * @param out - a validly opened file with write permissions
     * @param fn - usually, a normalized sequence of amplitudes for the sound card to reproduce
     */
    public static void write(File out, int[] fn) throws IOException {
        byte[] raw = new byte[4*fn.length];
        for (int i = 0; i < fn.length; i ++) {
            raw[4*i] = (byte)(fn[i] >> 24);
            raw[4*i+1] = (byte)(fn[i] >> 16);
            raw[4*i+2] = (byte)(fn[i] >> 8);
            raw[4*i+3] = (byte)(fn[i]);
        }

        write(out, raw);
    }

    /**
     * write a raw byte array to the specified file, in WAV format
     * @param out - a validly opened file with write permissions
     * @param raw - 4 bytes/number, Big Endian order
     */
    public static void write(File out, byte[] raw) throws IOException {
        AudioFormat format = new AudioFormat(R, 32, 1, true, true);
        AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(raw), format, raw.length/4);

        AudioSystem.write(in, AudioFileFormat.Type.WAVE, out);
    }
}
