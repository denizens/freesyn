package bnw.abm.intg.sync.wdwrapper;

import java.util.Random;
public class WDRandomHelper {
	
	private Random rand;

	public WDRandomHelper(int seed){
		rand = new Random(seed);
	}
	
	/**
	 * 
	 * @param from Start number (inclusive)
	 * @param to End Number (inclusive)
	 * @return
	 */
	public double nextDoubleFromTo(double from, double to){
		//rand.nextInt((max - min) + 1) + min;
		return from + rand.nextDouble()*(to-from);
	}
	
	/**
	 * 
	 * @param from Start number (inclusive)
	 * @param to End Number (inclusive)
	 * @return
	 */
	public int nextIntFromTo(int from, int to){
		return rand.nextInt((to - from) + 1) + from;
	}
	
	
}
