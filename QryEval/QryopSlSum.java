/**
 *  This class implements the SUM operator for the BM25 model.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;


public class QryopSlSum extends QryopSl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. 
   *  @param q A query argument (a query operator).
   */
	
	public QryopSlSum(Qryop... q) {
		//System.out.println("argument length is: " + q.length);
		for (int i = 0; i < q.length; i++){
			this.args.add(q[i]);
			//System.out.println(q[i].toString());
		}
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
	  
	  if (r instanceof BM25){
		  //System.out.println(((BM25) r).k_1 +" " + ((BM25) r).b +" " +((BM25) r).k_3);
		  return (evaluateBM25 (r, ((BM25) r).k_1, ((BM25) r).b, ((BM25) r).k_3));
	  }
	  else{
		  System.out.println("model mismatch....ERROR");
		  return null;
	  }
  }

  /**
   *  Evaluates the query operator for BM25,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateBM25 (RetrievalModel r, double k1, double b, double k3) throws IOException {

    //  Initialization
    allocArgPtrs (r);
    QryResult result = new QryResult ();
    int qrySize = this.argPtrs.size();
    // generate the query term vector 
    LinkedHashMap<ArgPtr, Integer> termCountMap = new LinkedHashMap<ArgPtr, Integer>();
    for(int i=0; i<qrySize; i++){
    	ArgPtr ptr = this.argPtrs.get(i);
    	if(termCountMap.containsKey(ptr)){
    		termCountMap.put(ptr, termCountMap.get(ptr)+1);
    	}
    	else{
    		termCountMap.put(ptr, 1);
    	}
    }
    //System.out.println("term count is " + termCountMap.size());
    int termCount = termCountMap.size();
    //get the union of document lists for all query terms
    LinkedHashMap<Integer, double[]> scoreMap = new LinkedHashMap<Integer, double[]>();
    int j=-1;
    for(ArgPtr ptr: termCountMap.keySet()){
    	j++;
    	for(int i=0; i<ptr.scoreList.scores.size(); i++){
    		//get internal id of current document being processed
    		int tmpDocId = ptr.scoreList.getDocid(i);
    		
    		double[] scoreArray = new double[termCount]; 
    		if(scoreMap.containsKey(tmpDocId)){
    			scoreArray = scoreMap.get(tmpDocId);
    		}
    		scoreArray[j]=ptr.scoreList.getDocidScore(i);
    		
    		scoreMap.put(tmpDocId, scoreArray);
    	}
    }
    
   
    //loop over the document list 
    //for each document, calculate the corresponding score
    for(Integer intDocId : scoreMap.keySet()){
    	
    	double sum = 0.0;
    	j = -1;
    	for(ArgPtr ptr: termCountMap.keySet()){
    		j++;
    		//query term frequency
			int curQTf = termCountMap.get(ptr);
			
    		double part3=((k3+1)*curQTf)/((k3+curQTf));
    		sum+=(scoreMap.get(intDocId)[j]*part3);
    	}
    	
    	result.docScores.add (intDocId, sum);
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

    return ("#SUM"+"( " + result + ")");
  }
}
