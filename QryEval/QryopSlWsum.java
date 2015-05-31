/**
 *  This class implements the WSUM operator for all retrieval models.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;


public class QryopSlWsum extends QryopSl {
	
	private ArrayList<Double> weights = new ArrayList<Double>();
	private double totalWeight = 0.0;
	private boolean expectingWeight = true;
  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
  public QryopSlWsum(Qryop... q) {
	  //System.out.println("argument length is: " + q.length);
    for (int i = 0; i < q.length; i++){
      this.args.add(q[i]);
      //System.out.println(q[i].toString());
    }
  }
  public QryopSlWsum() {
	  //System.out.println("new wsum");
	  this.weights = new ArrayList<Double>();
	  this.totalWeight = 0.0;
	  this.expectingWeight = true;
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
    this.expectingWeight = true;
  }
  
  public void addWeight (Double d) {
	this.weights.add(d);
	this.expectingWeight = false;
  }
  
  public void removeLastWeight () {
	  this.weights.remove(this.weights.size()-1);
	  this.expectingWeight = true;
	  //System.out.println("remove weight\n");
  }
  public boolean isExpectingWeight(){
	  return this.expectingWeight;
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
	//System.out.println("evaluate wsum");
    if (r instanceof Indri){
    	return evaluateHelper(r);
    }
    else {
    	System.out.println("model type mismatch...ERROR");
    	return null;
    }
    
  }
  
  /**
   *  Evaluates the query operator for Indri models,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateHelper (RetrievalModel r) throws IOException {
    //Initialization
    allocArgPtrs (r);
    sumWeights();
    //System.out.println("total weight is " + this.totalWeight);
    QryResult result = new QryResult ();
    int qrySize = this.argPtrs.size();
    
    if(qrySize!=this.weights.size())
    	System.out.println("Qry size mismatch...ERROR");
    //System.out.println("query size is " + qrySize);
    /*for(int j=0; j<qrySize; j++){
    	Qryop op = this.args.get(j);
    	//System.out.println(op.toString());
    }*/
    HashMap<Integer, HashSet<Integer>> docIdCollection = new HashMap<Integer, HashSet<Integer>>();
    // a reference map to store all docids for each query term
    for(int j=0; j<qrySize; j++){
    	ArgPtr ptr = this.argPtrs.get(j);
    	HashSet<Integer> docIdSet = new HashSet<Integer>();
    	for(int i=0; i<ptr.scoreList.scores.size(); i++){
    		//if(ptr.scoreList.getDocid(i)==311678)
    		//	System.out.println(j+"thth arg has 311678");
    		docIdSet.add(ptr.scoreList.getDocid(i));
    	}
    	docIdCollection.put(j, docIdSet);
    }
    //System.out.println("docIdCollection is " + docIdCollection.size());
    
    //HashMap stored p(q|d) calculated for all documents
    LinkedHashMap<Integer, double[]> qDMap = new LinkedHashMap<Integer, double[]>();
    //get the union of document lists for all query terms
    for(int j=0; j<qrySize; j++){
    	ArgPtr ptr = this.argPtrs.get(j);
    	//System.out.println("document list size for query " + j + ptr.scoreList.scores.size());
    	for(int i=0; i<ptr.scoreList.scores.size(); i++){
    		//get internal id of current document being processed
    		int tmpDocId = ptr.scoreList.getDocid(i); 
    		//if(tmpDocId==311678)
    		//	System.out.println(j+"th arg has 311678");
    		
    		double[] qDArray = new double[qrySize];
    		//System.out.println("test if processed");
    		if(qDMap.containsKey(tmpDocId)){
    			qDArray = qDMap.get(tmpDocId);
    			//if(tmpDocId==311678)
    			//	System.out.println("for " + j +"th arg, 311678 already calced....");
    		}
    		else{ 
    			//if(tmpDocId==311678){
    			//	System.out.println("for " + j + "th arg new doc processing");
    			//	System.out.println("qrySize is " + qrySize);
    			//}
    			for(int k=0; k<qrySize; k++){
    				//if(tmpDocId==311678)
    				//	System.out.println("k is " + k);
    				if(k==j)
    					continue;
    				//document tmpDocId is not included in term k's inverted list
    				if(!docIdCollection.get(k).contains(tmpDocId)){
    					//if(tmpDocId==311678)
    					//	System.out.println("311678 not included in " + k + "th arg");
    					qDArray[k] = ((QryopSl)this.args.get(k)).getDefaultScore(r,tmpDocId);
    					//if(tmpDocId==311678)
    					//	System.out.println("311678 default score for " + k + "th arg is " + qDArray[k]);
    				}
    				//if(tmpDocId==311678)
    				//	System.out.println("k is " + k);
    			}
    		}
    		qDArray[j]=ptr.scoreList.getDocidScore(i);
    		qDMap.put(tmpDocId, qDArray);
    		
    		
    	}
    }
    
    //loop over the document list 
    //for each document, calculate the corresponding score
    //System.out.println("qDMap size is " + qDMap.size());
    for(Integer intDocId : qDMap.keySet()){
    	
    	double score = 0.0;
    	//System.out.println(Arrays.toString(qDMap.get(intDocId)));
    	for(int j=0; j<qrySize; j++){
    		//System.out.println(qDMap.get(intDocId)[j]);
    		double toAdd=(qDMap.get(intDocId)[j])*(this.weights.get(j)/this.totalWeight);
    		//double toMultiply=(qDMap.get(intDocId)[j]);
    		//System.out.println(toMultiply);
    		//System.out.println("after power " + toMultiply);
    		score = score+toAdd;
    	}
    	
    	//if(intDocId==311678)
    	//	System.out.println("score for 311678 is " + score);
    	//System.out.println("final score is " + score);
    	result.docScores.add (intDocId, score);
    }
    freeArgPtrs ();
    return result;
  }
  public void sumWeights(){
	  this.totalWeight=0.0;
	  for(int i=0; i<this.weights.size(); i++){
		  this.totalWeight += this.weights.get(i);
		  //System.out.println("the " + i +"th weight is " + this.weights.get(i));
	  }
	  //System.out.println("the total weight is " + this.totalWeight);
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
	  //System.out.println("AND default score");
	  if (r instanceof Indri)
	  {
		  	double score = 0.0;
	    	double toAdd = 0.0;
	    	int qrySize = args.size();
	    	for(int i=0; i<qrySize; i++){
	    		toAdd = ((QryopSl)this.args.get(i)).getDefaultScore(r,docid);
				toAdd = toAdd* (this.weights.get(i)/this.totalWeight);
				score+=toAdd;
				
	    	}
	    	//System.out.println("AND default score is " + score);
	    	return score;
	  }
	  else
	  {
		  System.out.println("model type mismatch...ERROR");
		  return 0.0;
	  }
  }

  /*
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (int i=0; i<this.args.size(); i++)
      result += (this.weights.get(i).toString() + " " + this.args.get(i).toString() + " ");

    return ("#WSUM( " + result + ")");
  }
}
