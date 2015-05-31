/**
 *  The BM25 model has no parameters.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */
public class BM25 extends RetrievalModel{

	public double k_1;
	public double b;
	public double k_3;
	public BM25(double k_1, double b, double k_3){
		this.k_1 = k_1;
		this.b = b;
		this.k_3 = k_3;
	}
	
	/**
	   * Set a retrieval model parameter.
	   * @param parameterName
	   * @param parametervalue
	   * @return Always false because this retrieval model has no parameters.
	   */
	  public boolean setParameter (String parameterName, double value) {
	    System.err.println ("Error: Unknown parameter name for retrieval model " +
				"BM25: " +
				parameterName);
	    return false;
	  }

	  /**
	   * Set a retrieval model parameter.
	   * @param parameterName
	   * @param parametervalue
	   * @return Always false because this retrieval model has no parameters.
	   */
	  public boolean setParameter (String parameterName, String value) {
	    System.err.println ("Error: Unknown parameter name for retrieval model " +
				"BM25: " +
				parameterName);
	    return false;
	  }

	
}
