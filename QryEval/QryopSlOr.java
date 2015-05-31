/**
 *  This class implements the OR operator for all retrieval models.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlOr extends QryopSl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopOr (arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
  public QryopSlOr(Qryop... q) {
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
    //System.out.println("parameter length " + q.length);
  }

  /**
   *  Appends an argument to the list of query operator arguments.  This
   *  simplifies the design of some query parsing architectures.
   *  @param {q} q The query argument (query operator) to append.
   *  @return void
   *  @throws IOException
   */
  public void add (Qryop a) {
    this.args.add(a);
  }

  /**
   *  Evaluates the query operator, including any child operators and
   *  returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluate(RetrievalModel r) throws IOException {
	  if(this.args.size()<=0)
		  return null;
	  
    if (r instanceof RetrievalModelUnrankedBoolean)
    	return (evaluateBoolean (r, true));
    else if(r instanceof RetrievalModelRankedBoolean){
    	//System.out.println("ranked");
    	return (evaluateBoolean (r, false));
    }
    else
    	return (evaluateBoolean (r, false));
    	
    
  }

  /**
   *  Evaluates the query operator for boolean retrieval models,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateBoolean (RetrievalModel r, boolean unRanked) throws IOException {

    //  Initialization
	  //System.out.println("enter once");
    allocArgPtrs (r);
    //System.out.println("after alloc");
    QryResult result = new QryResult ();

    //  Sort the arguments so that the shortest lists are first.  This
    //  improves the efficiency of exact-match OR without changing
    //  the result.

    for (int i=0; i<(this.argPtrs.size()-1); i++) {
      for (int j=i+1; j<this.argPtrs.size(); j++) {
	if (this.argPtrs.get(i).scoreList.scores.size() >
	    this.argPtrs.get(j).scoreList.scores.size()) {
	    ScoreList tmpScoreList = this.argPtrs.get(i).scoreList;
	    this.argPtrs.get(i).scoreList = this.argPtrs.get(j).scoreList;
	    this.argPtrs.get(j).scoreList = tmpScoreList;
	}
      }
    }

    //  Exact-match OR requires that the document id appears in AT LEAST ONE 
    // 	scoreList. Use the first (shortest) list to control the
    //  search for matches.
    //  Named loops are a little ugly.  However, they make it easy
    //  to terminate an outer loop from within an inner loop.
    //  Otherwise it is necessary to use flags, which is also ugly.
    //  Read all doc ids associated with OR query terms,
    //  efficiently eliminate the duplicate doc id problem
    HashMap<Integer, Double> docMap = new HashMap<Integer, Double>();
    for (int j=0; j<this.argPtrs.size(); j++){
    	ArgPtr ptr = this.argPtrs.get(j);
    	for ( ; ptr.nextDoc < ptr.scoreList.scores.size(); ptr.nextDoc ++) {
    		int ptrDocid = ptr.scoreList.getDocid(ptr.nextDoc);
    		double ptrDocScore = ptr.scoreList.getDocidScore(ptr.nextDoc);
    		if(docMap.containsKey(ptrDocid)){
    			if(ptrDocScore > docMap.get(ptrDocid))
    				docMap.put(ptrDocid, ptrDocScore);
    		}
    		else{
    			docMap.put(ptrDocid, ptrDocScore);
    		}
    	}
    }
    //  Iterate over the hashmap to store <doc id, doc score> k-v pair to result
    for(Integer key: docMap.keySet()){
    	result.docScores.add (key, docMap.get(key));
    }
    freeArgPtrs ();

    return result;
  }

  /*
   *  Calculate the default score for the specified document if it
   *  does not match the query operator.  This score is 0 for many
   *  retrieval models, but not all retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @param docid The internal id of the document that needs a default score.
   *  @return The default score.
   */
  public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean)
      return (0.0);

    return 0.0;
  }

  /*
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (int i=0; i<this.args.size(); i++)
      result += this.args.get(i).toString() + " ";

    return ("#OR( " + result + ")");
  }
}
