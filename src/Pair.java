
/**
 * @author Arno De Witte
 *
 * Simple class that allows to return 2 values
 *
 * @param <A>
 * @param <B>
 */
public class Pair<A, B> {

	private A a;
	private B b;
	
	public Pair(A a, B b){
		this.a = a;
		this.b = b;
	}

	public A getA() {
		return a;
	}

	public B getB() {
		return b;
	}
	
}
