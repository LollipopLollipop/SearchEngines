/**
 *  The Indri model has no parameters.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */
public class Indri extends RetrievalModel{

	public double mu;
	public double lambda;
	public Indri(double mu, double lambda){
		this.mu = mu;
		this.lambda = lambda;
		
	}
	/**
	   * Set a retrieval model parameter.
	   * @param parameterName
	   * @param parametervalue
	   * @return Always false because this retrieval model has no parameters.
	   */
	  public boolean setParameter (String parameterName, double value) {
	    System.err.println ("Error: Unknown parameter name for retrieval model " +
				"Indri: " +
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
				"Indri: " +
				parameterName);
	    return false;
	  }

	
}
