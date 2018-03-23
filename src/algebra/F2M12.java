package algebra;

import algebra.F2M12.F2M12Factory;
import java.util.Random;

import org.jlinalg.JLinAlgTypeProperties;
import org.jlinalg.IRingElement;
import org.jlinalg.IRingElementFactory;
import org.jlinalg.RingElement;
import org.jlinalg.RingElementFactory;
import org.jlinalg.FieldElement;
import org.jlinalg.Matrix;
import org.jlinalg.Vector;

/**
 * Implementation of GF(2^12) using JLinAlg interface.
 *
 * This implementation is heavily dependent on JLinAlg (jlinalg.sourceforge.net)
 *
 * You're welcome to enjoy the source code but this isn't the point of the project so it isn't documented...
 */
public class F2M12 extends FieldElement<F2M12> {
	private static boolean[] M = new boolean[13];
	static {
		M[0] = true;
		M[3] = true;
		M[12] = true;
	}

	private boolean[] co;

	public F2M12(boolean[] co) {
		if (co.length > 12) throw new IllegalArgumentException("Too many indices");
		this.co = new boolean[12];
		for (int i = 0; i < co.length; i ++) this.co[i] = co[i];
	}

	public F2M12(int... vector) {
		if (vector.length > 12) throw new IllegalArgumentException("Too many arguments");
		this.co = new boolean[12];
		for (int i = 0; i < vector.length; i ++) co[i] = vector[i] > 0;
	}

	public boolean co(int i) {
		if (i < 0 || i >= 12) throw new IllegalArgumentException("Index must be in [0,12)");
		return co[i];
	}


    public F2M12Factory getFactory() {
        return FACTORY;
    }

    public F2M12 abs() {
        return this;	// negatives mean nothing in p=2
    }

    public int compareTo(F2M12 o) {
		if (this.equals(o)) return 0;
		else return 1;	// unordered field. I'm not sure how the API should handle that
    }

    public F2M12 multiply(F2M12 other) {
		boolean[] product = new boolean[12];
		boolean[] cox = new boolean[12];
		for (int i = 0; i < 12; i ++) cox[i] = co[i];

		for (int i = 0; i < 12; i ++) {
			// add the term co*x^i if x^i is a term in other
			if (other.co[i]) {
				for (int j = 0; j < 12; j ++) product[j] = product[j] != cox[j];
			}

			// set cox *= x
			boolean gtp = cox[11];	// will we subtract by M = 1 + x^3 + x^12?
			for (int j = 11; j > 0; j --) cox[j] = cox[j-1];
			if (gtp) {
				cox[3] = !cox[3];
				cox[0] = true;
			} else cox[0] = false;
		}

		return new F2M12(product);
	}
	
	public F2M12 pow(int e) {
		F2M12 p = ONE;
		for (int i = 1; i <= e; i ++) p = p.multiply(this);
		return p;
	}

    public F2M12 negate() {
        return this;	// negatives mean nothing in p=2
	}
	
	public F2M12 invert() {
		if (this.equals(ZERO)) throw new IllegalArgumentException("Cannot divide by zero");

		boolean[] a = new boolean[12];
		for (int i = 0; i < 12; i ++) a[i] = co[i];

		F2M12 xp = ZERO;
		F2M12 x = ONE;

		divmod qr = new divmod(M, a);
		while (! qr.R.isZero()) {
			F2M12 temp = x;
			x = xp.subtract( qr.Q.multiply(x) );
			xp = temp;

			temp = qr.R;
			qr = new divmod(a, temp.co);
			a = temp.co;
		}

		// as this is a field, last non-zero remainder is always scalar
		// as coefficients are binary, no normalization is necessary

		return x;
	}

	private class divmod {
		F2M12 Q;
		F2M12 R;

		divmod(boolean[] p, boolean[] a) {
			// first find degrees
			int deg_p = p.length - 1;
			while (deg_p >= 0 && !p[deg_p]) deg_p --;
			if (deg_p < 0) {	// p == 0
				Q = ZERO;
				R = ZERO;
				return;
			}

			int deg_a = a.length - 1;
			while (deg_a >= 0 && !a[deg_a]) deg_a --;
			if (deg_a < 0) throw new IllegalArgumentException("Cannot divide by zero");
			if (deg_a == 0) {	// a == 1
				if (deg_p == 12) Q = ONE;	// deg_p == 13 only when p == M
				else Q = new F2M12(p);
				R = ZERO;
				return;
			}

			if (deg_p < deg_a) {
				Q = ZERO;
				R = new F2M12(p);
				return;
			}

			int d = deg_p - deg_a;

			boolean[] new_p = new boolean[a.length];
			for (int i = 0; i < d; i ++) new_p[i] = p[i];
			for (int i = d; i < a.length; i ++) new_p[i] = p[i] != a[i-d];

			if (d == 0) {
				Q = ONE;
				R = new F2M12(new_p);
				return;
			}

			divmod recur = new divmod(new_p, a);	// yes I made a recursive constructor
			
			boolean[] q = new boolean[12];
			for (int i = 0; i < q.length; i ++) q[i] = recur.Q.co[i]; // copy quotient
			q[d] = true;							// include this iteration

			Q = new F2M12(q);
			R = recur.R;
		}
	}

    public F2M12 add(F2M12 other) {
		boolean[] sum = new boolean[12];
		for (int i = 0; i < 12; i ++) {
			sum[i] = co[i] != other.co[i];
		}
        return new F2M12(sum);
	}

	public boolean equals(Object o) {
		if (! (o instanceof F2M12)) return false;
		F2M12 other = (F2M12)o;
		for (int i = 0; i < co.length; i ++) {
			if (co[i] != other.co[i]) return false;
		}
		return true;
	}

	public String toString() {
		String ret = "";
		for (boolean b: co) ret += b ? "1" : "0";
		return ret;
	}

	private static final F2M12 ZERO = new F2M12();
	private static final F2M12 ONE = new F2M12(1);
	
	public static final F2M12Factory FACTORY = ZERO.new F2M12Factory();


    /**
	 * The factory for instances of class {@link DoubleWrapper#FACTORY}
	 */
	@JLinAlgTypeProperties(isExact = true, isDiscreet = true)
    public class F2M12Factory
			extends RingElementFactory<F2M12>
			implements IRingElementFactory<F2M12>
	{
		/**
		 * only used (once) by {@link F2M12} to instantiate
		 * {@link F2M12#FACTORY}
		 */
		private F2M12Factory() {
			super();
		}

		@Override
		public F2M12 get(Object o) {
			if (o instanceof boolean[]) {
				return new F2M12((boolean[])o);
			}
			if (o instanceof int[]) {
				return new F2M12((int[])o);
			}
			throw new IllegalArgumentException("Unsupported argument: "+o);
		}

		@Override
		public F2M12[][] getArray(int rows, int columns) {
			F2M12[][] array = new F2M12[rows][columns];
			for (int i = 0; i < rows; i ++) {
				for (int j = 0; j < columns; j ++) {
					array[i][j] = new F2M12();
				}
			}
			return array;
		}

		@Override
		public F2M12 m_one() {
			return ONE;
		}

		@Override
		public F2M12 one() {
			return ONE;
		}

		@Override
		public F2M12 zero() {
			return ZERO;
		}

		@Override
		public F2M12[] getArray(int size) {
			F2M12[] array = new F2M12[size];
			for (int i = 0; i < size; i ++) {
				array[i] = new F2M12();
			}
			return array;
		}

		@Override
		public F2M12 get(double d) {
			throw new UnsupportedOperationException();
		}

		@Override
		public F2M12 get(int i) {
			throw new UnsupportedOperationException();
		}

		@Override
		public F2M12 randomValue() {
			boolean[] co = new boolean[12];
			for (int i = 0; i < 12; i ++) {
				co[i] = (Math.random() < .5);
			}
			return new F2M12(co);
        }

		@SuppressWarnings("deprecation")
		@Override
		@Deprecated
		public F2M12 randomValue(Random random) {
			throw new UnsupportedOperationException();
		}

		@SuppressWarnings("deprecation")
		@Override
		@Deprecated
		public F2M12 gaussianRandomValue(
				@SuppressWarnings("unused") Random random) {
			throw new UnsupportedOperationException();
		}

		@Override
		public F2M12 get(long d) {
			throw new UnsupportedOperationException();
		}

		@SuppressWarnings("deprecation")
		@Override
		@Deprecated
		public F2M12 randomValue(
				@SuppressWarnings("unused") Random random, F2M12 min,
				F2M12 max) {
			throw new UnsupportedOperationException();
		}

		@Override
		public F2M12 gaussianRandomValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public F2M12 randomValue(F2M12 min, F2M12 max) {
			throw new UnsupportedOperationException();
		}
	}
}
