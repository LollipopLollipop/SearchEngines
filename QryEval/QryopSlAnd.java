/**
 *  This class implements the AND operator for all retrieval models.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;


public class QryopSlAnd extends QryopSl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
  public QryopSlAnd(Qryop... q) {
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
	  //System.out.println("and");
	  if(this.args.size()<=0)
		  return null;
	  
    if (r instanceof RetrievalModelUnrankedBoolean){
    	//System.out.println("unranked");
    	return (evaluateBoolean (r, true));
    }
    else if (r instanceof RetrievalModelRankedBoolean){
    	//System.out.println("ranked");
    	QryResult res = evaluateBoolean (r, false);
    	//System.out.println("result score list size is: " + res.docScores.scores.size());
    	return res;
    }
    else if (r instanceof BM25){
    	QryResult res = evaluateBoolean (r, false);
    	return res;
    }
    else {
    	//System.out.println("evaluate Indri");
    	QryResult res = evaluateIndri(r);
    	return res;
    }
    //return null;
  }
  
  /**
   *  Evaluates the query operator for Indri models,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  //fails when there are >=3 terms!!!!!!!!!!!!!***************!!!!!!!!!!!!!
  public QryResult evaluateIndri (RetrievalModel r) throws IOException {
    //Initialization
    allocArgPtrs (r);
    QryResult result = new QryResult ();
    int qrySize = this.argPtrs.size();
    //System.out.println("query size is " + qrySize);
    /*for(int j=0; j<qrySize; j++){
    	Qryop op = this.args.get(j);
    	//System.out.println(op.toString());
    }*/
    HashMap<Integer, HashSet<Integer>> docIdCollection = new HashMap<Integer, HashSet<Integer>>();
    // a reference map to store all docids for each query term
    for(int j=0; j<qrySize; j++){
    	ArgPtr ptr = this.argPtrs.get(j);
    	//System.out.println("score list size for " + j + "th arg is " + ptr.scoreList.scores.size());
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
    //System.out.println("QDMap size = " + qDMap.size());
    //loop over the document list 
    //for each document, calculate the corresponding score
    //System.out.println("qDMap size is " + qDMap.size());
    for(Integer intDocId : qDMap.keySet()){
    	
    	double score = 1.0;
    	//System.out.println(Arrays.toString(qDMap.get(intDocId)));
    	for(int j=0; j<qrySize; j++){
    		//System.out.println(qDMap.get(intDocId)[j]);
    		double toMultiply=Math.pow((qDMap.get(intDocId)[j]),1/((double)qrySize));
    		//double toMultiply=(qDMap.get(intDocId)[j]);
    		//System.out.println(toMultiply);
    		//System.out.println("after power " + toMultiply);
    		score = score*toMultiply;
    	}
    	
    	//if(intDocId==311678)
    	//	System.out.println("score for 311678 is " + score);
    	//System.out.println("final score is " + score);
    	result.docScores.add (intDocId, score);
    }
    freeArgPtrs ();
    return result;
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
	  //System.out.println("called once");
    allocArgPtrs (r);
    QryResult result = new QryResult ();

    //  Sort the arguments so that the shortest lists are first.  This
    //  improves the efficiency of exact-match AND without changing
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

    //  Exact-match AND requires that ALL scoreLists contain a
    //  document id.  Use the first (shortest) list to control the
    //  search for matches.

    //  Named loops are a little ugly.  However, they make it easy
    //  to terminate an outer loop from within an inner loop.
    //  Otherwise it is necessary to use flags, which is also ugly.

    ArgPtr ptr0 = this.argPtrs.get(0);

    EVALUATEDOCUMENTS:
    for ( ; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc ++) {

      int ptr0Docid = ptr0.scoreList.getDocid (ptr0.nextDoc);
      double docScore = ptr0.scoreList.getDocidScore (ptr0.nextDoc);

      //  Do the other query arguments have the ptr0Docid?

      for (int j=1; j<this.argPtrs.size(); j++) {

	ArgPtr ptrj = this.argPtrs.get(j);

	while (true) {
	  if (ptrj.nextDoc >= ptrj.scoreList.scores.size())
	    break EVALUATEDOCUMENTS;		// No more docs can match
	  else
	    if (ptrj.scoreList.getDocid (ptrj.nextDoc) > ptr0Docid)
	      continue EVALUATEDOCUMENTS;	// The ptr0docid can't match.
	  else
	    if (ptrj.scoreList.getDocid (ptrj.nextDoc) < ptr0Docid)
	      ptrj.nextDoc ++;			// Not yet at the right doc.
	  else{
		  if(ptrj.scoreList.getDocidScore (ptrj.nextDoc) < docScore)
			  docScore = ptrj.scoreList.getDocidScore (ptrj.nextDoc);
		  break;
	  }// ptrj matches ptr0Docid
	}
      }

      //  The ptr0Docid matched all query arguments, so save it.
      //if(unRanked)
      //result.docScores.add (ptr0Docid, 1.0);
      //else
    	  result.docScores.add (ptr0Docid, docScore);
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
	  //System.out.println("AND default score");
	  if (r instanceof RetrievalModelUnrankedBoolean)
	      return (0.0);
	    else if (r instanceof RetrievalModelRankedBoolean)
	        return (0.0);
	    else if (r instanceof BM25)
	        return (0.0);
	    else{
	    	//System.out.println("get default indri score");
	    	double score = 1.0;
	    	double toMultiply = 1.0;
	    	int qrySize = args.size();
	    	for(int i=0; i<qrySize; i++){
	    		if(this.args.get(i) instanceof QryopSl)
	    			toMultiply = ((QryopSl)this.args.get(i)).getDefaultScore(r,docid);
	    		else
	    			toMultiply = (new QryopSlScore(this.args.get(i))).getDefaultScore(r, docid);
				
	    		toMultiply = Math.pow(toMultiply, (1/(double)qrySize));
				score*=toMultiply;
				
	    	}
	    	//System.out.println("AND default score is " + score);
	    	return score;
	    }
  }

  /*
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (int i=0; i<this.args.size(); i++)
      result += this.args.get(i).toString() + " ";

    return ("#AND( " + result + ")");
  }
}
