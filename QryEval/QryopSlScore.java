/**
 *  This class implements the SCORE operator for all retrieval models.
 *  The single argument to a score operator is a query operator that
 *  produces an inverted list.  The SCORE operator uses this
 *  information to produce a score list that contains document ids and
 *  scores.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlScore extends QryopSl {
	
	private QryResult result = null;

  /**
   *  Construct a new SCORE operator.  The SCORE operator accepts just
   *  one argument.
   *  @param q The query operator argument.
   *  @return @link{QryopSlScore}
   */
  public QryopSlScore(Qryop q) {
    this.args.add(q);
    
  }
  

  /**
   *  Construct a new SCORE operator.  Allow a SCORE operator to be
   *  created with no arguments.  This simplifies the design of some
   *  query parsing architectures.
   *  @return @link{QryopSlScore}
   */
  public QryopSlScore() {
  }

  /**
   *  Appends an argument to the list of query operator arguments.  This
   *  simplifies the design of some query parsing architectures.
   *  @param q The query argument to append.
   */
  public void add (Qryop a) {
    this.args.add(a);
  }

  /**
   *  Evaluate the query operator.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluate(RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean)
    	return (evaluateBoolean (r, true));
    else if (r instanceof RetrievalModelRankedBoolean)
    	return (evaluateBoolean(r, false));
    else if (r instanceof BM25)
    	return (evaluateBM25(r));
    else {
    	//System.out.println("score evaluate Indri");
    	return evaluateIndri(r);
    }
  }

 /**
   *  Evaluate the query operator for boolean retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateBoolean(RetrievalModel r, boolean unRanked) throws IOException {

    // Evaluate the query argument.

    result = args.get(0).evaluate(r);

    // Each pass of the loop computes a score for one document. Note:
    // If the evaluate operation above returned a score list (which is
    // very possible), this loop gets skipped.

    for (int i = 0; i < result.invertedList.df; i++) {

      // DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY. 
      // Unranked Boolean. All matching documents get a score of 1.0.
    	if(unRanked)
    		result.docScores.add(result.invertedList.postings.get(i).docid,
			   (float) 1.0);
    	else
    		result.docScores.add(result.invertedList.postings.get(i).docid,
    				   (float) result.invertedList.postings.get(i).tf);
    }

    // The SCORE operator should not return a populated inverted list.
    // If there is one, replace it with an empty inverted list.

    if (result.invertedList.df > 0)
	result.invertedList = new InvList();

    return result;
  }
  
  /**
   *  Evaluate the query operator for BM25 model.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateBM25(RetrievalModel r) throws IOException {
	  //System.out.println("score evaluate BM25");
    // Evaluate the query argument.
	// Eval of IITerm
    result = args.get(0).evaluate(r);
    double k1 = ((BM25)r).k_1;
    double b = ((BM25)r).b;
    
    // Each pass of the loop computes a score for one document. Note:
    // If the evaluate operation above returned a score list (which is
    // very possible), this loop gets skipped.
    int df = result.invertedList.df;
    String field = result.invertedList.field;
	double avgDocLen = QryEval.totalTermFreq.get(field) /
		      (double) QryEval.totalDocCount.get(field);
	//System.out.println("df is " + df);
	//initialize doc scores if null
    if(result.docScores.scores.size()<=0){
    	for (int i = 0; i < result.invertedList.postings.size(); i++) {
    		int tmpDocId = result.invertedList.getDocid(i);
    		int docLen = (int) QryEval.s.getDocLength(field, tmpDocId);
    		int tf = result.invertedList.getTf(i);
    		//System.out.println("tf is " + tf);
    		double part1=Math.max(0, Math.log((QryEval.N-df+0.5)/(df+0.5)));
    		double part2=tf/(k1*((docLen/avgDocLen)*b+(1-b))+tf);
    		double score =(part1*part2);
    		result.docScores.add(tmpDocId,score);
    	}
    }
    
    return result;
  }
  
  
  /**
   *  Evaluate the query operator for Indri model.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateIndri(RetrievalModel r) throws IOException {

    // Evaluate the query argument.
	// Eval of IITerm
    result = args.get(0).evaluate(r);
    
    // Each pass of the loop computes a score for one document. Note:
    // If the evaluate operation above returned a score list (which is
    // very possible), this loop gets skipped.
    int ctf = result.invertedList.ctf;
    String field = result.invertedList.field;
    //System.out.println(field);
	long collectionLen = QryEval.totalTermFreq.get(field);
	//System.out.println(collectionLen);
	//initialize doc scores if null
    if(result.docScores.scores.size()<=0){
    	for (int i = 0; i < result.invertedList.postings.size(); i++) {
    		int tmpDocId = result.invertedList.getDocid(i);
    		result.docScores.add(tmpDocId,
    			getIndriScore(r,tmpDocId,i,ctf,collectionLen,result));
    	}
    }
    
    return result;
  }
  public double getExpansionScore(RetrievalModel r, int docid, int fbMu) throws IOException{
	  //System.out.println("1");
	  result = args.get(0).evaluate(r);
	  
	  int ctf = result.invertedList.ctf;
	  String field = result.invertedList.field;
	  
	  long collectionLen = QryEval.totalTermFreq.get(field);
	  int docLen = (int) QryEval.s.getDocLength(field, docid);
	  
	  int tf = result.invertedList.docTfMap.get(docid);
	  
	  double ptc = ctf/(double)collectionLen; 
	  double ptd = (tf+fbMu*ptc)/(docLen+fbMu);
	  return ptd*Math.log(1/ptc);
  }
  /*
   *  Calculate the Indri query likelihood score for a document that matches
   *  the query argument.  
   *  @param r A retrieval model that controls how the operator behaves.
   *  @param docid The internal id of the document
   *  @return The calculated score.
   */
  public double getIndriScore (RetrievalModel r, long docid,int i, int ctf, long collectionLen, QryResult result) throws IOException {

    if (r instanceof Indri)
    {
    	//System.out.println("score calc");
    	//do the query likelihood with 2 stage smoothing
    	
    	//QryResult result = args.get(0).evaluate(r);
    	//int ctf = result.invertedList.ctf;
    	//long collectionLen = QryEval.READER.getSumTotalTermFreq(result.invertedList.field);
    	int docLen = (int) QryEval.s.getDocLength(result.invertedList.field, (int)docid);
    	
    	if(result.invertedList.getDocid(i)==docid){
			return qryLikeliHood(result.invertedList.getTf(i), ctf, collectionLen, 
					docLen, ((Indri)r).lambda, ((Indri)r).mu);
    	}
    	else{
    		System.out.println("current doc id could be found at the list...ERROR");
    		return qryLikeliHood(0, ctf, collectionLen, docLen, ((Indri)r).lambda, ((Indri)r).mu);
    	}
    }
    else{
    	System.out.println("you can not reach here...ERROR");
    	return 0.0;
    }
    
  }

  /*
   *  Calculate the default score for a document that does not match
   *  the query argument.  This score is 0 for many retrieval models,
   *  but not all retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @param docid The internal id of the document that needs a default score.
   *  @return The default score.
   */
  public double getDefaultScore (RetrievalModel r, long docid) throws IOException {
	  //System.out.println("Score default score");
	  //System.out.println(invList.postings.size());
	  if(result==null)
		  result = args.get(0).evaluate(r);
	if (r instanceof RetrievalModelUnrankedBoolean)
      return (0.0);
    else if (r instanceof RetrievalModelRankedBoolean)
        return (0.0);
    else if (r instanceof BM25)
        return (0.0);
    else{
    	//System.out.println(invListCollection.size());
    	//do the query likelihood with 2 stage smoothing
    	//System.out.println(args.get(0).toString());
    	//QryResult result = args.get(0).evaluate(r);
    	//System.out.println(invListCollection.get(args.get(0)));
    	//System.exit(0);
    	int ctf = result.invertedList.ctf;
    	//System.out.println("ctf is " + ctf);
    	String field = result.invertedList.field;
    	//System.out.println("field is "+field);
    	long collectionLen = QryEval.totalTermFreq.get(field);
    	//System.out.println("collection length is "+collectionLen);
    	int docLen = (int) QryEval.s.getDocLength(field, (int)docid);
    	//System.out.println("doc len is " +docLen);
    	//System.out.println("lambda is " + ((Indri)r).lambda);
    	//System.out.println("mu is " + ((Indri)r).mu);
    	return qryLikeliHood(0, ctf, collectionLen, docLen, ((Indri)r).lambda, ((Indri)r).mu);
    }
  }
  
  public double qryLikeliHood(int tf, int ctf, long collectionLen, int docLen, double lambda, double mu){
	  double qC = ctf/(double)collectionLen;
	  double res = ((1-lambda)*((tf+mu*qC)/(docLen+mu)))+
			  lambda*qC;
	  
	  //System.out.println("res is " +res);
	  return res;
  }

  /**
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (Iterator<Qryop> i = this.args.iterator(); i.hasNext(); )
      result += (i.next().toString() + " ");

    return ("#SCORE( " + result + ")");
  }
}
