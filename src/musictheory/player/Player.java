/**
 * Player.java: provides functions to interface Javas SoundAPI with Phrases and their .phases() arrays
 */
package musictheory.player;

import musictheory.music.Phrase;
import musictheory.music.Chord;
import musictheory.music.Note;

import java.util.ArrayList;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * Player - static class, just a vector for its functions
 */
public class Player {
    /** the standardized sample rate */
    public static float R = 44100; // samples / sec

    /**
     * play - converts phrases into raw bytes and asks the operating system to sound them
     *
     * @param Ps - any positive number of phrases. They'll all be played simultaneously.
     */
    public static void play(Phrase... Ps) {
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

        play(phases);
    }

    /**
     * converts a double[] of amplitudes into raw bytes and asks the operating system to sound them
     * @param phases - discretized values of pressure amplitude to reproduce in your sound card
     *
     * In retrospect, "phases" is probably not the right word...
     */
    public static void play(double[] phases) {
        double max = 0;
        // find peak phase, to normalize to max int
        for (int i = 0; i < phases.length; i ++) {
            if (phases[i] > max) max = phases[i];
        }

        int[] fn = new int[phases.length];
        for (int i = 0; i < phases.length; i ++) {
            fn[i] = (int)(phases[i] / max * Integer.MAX_VALUE);
        }

        play(fn);
    }

    /**
     * converts an int[] of amplitudes into raw bytes and asks the operating system to sound them
     * @param fn - usually, a pressure amplitude function normalized to Integer.MAX_VALUE
     */
    public static void play(int[] fn) {
        byte[] raw = new byte[4*fn.length];
        for (int i = 0; i < fn.length; i ++) {
            raw[4*i] = (byte)(fn[i] >> 24);
            raw[4*i+1] = (byte)(fn[i] >> 16);
            raw[4*i+2] = (byte)(fn[i] >> 8);
            raw[4*i+3] = (byte)(fn[i]);
        }

        play(raw);
    }

    /**
     * asks the operating system to sound an array of raw bytes
     * @param raw - a byte array of integers (4 bytes / number) in Big Endian order
     */
    public static void play(byte[] raw) {
        AudioFormat format = new AudioFormat(R, 32, 1, true, true);
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(format, raw, 0, raw.length);
            clip.loop(0);

            try {
                Thread.sleep((long)(raw.length/4/R*1000));
            } catch (InterruptedException e) {
                System.err.println("Sleep interrupted");
            }
        } catch (LineUnavailableException e) {
            System.err.println("System could not provide clip line.");
        }
    }
}
