/**
 *  This class implements the NEAR operator for all retrieval models.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopIlNear extends QryopIl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopNear (n, arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
	private int proximity = 0;
	public QryopIlNear(int n, Qryop... q) {
	  //System.out.println("argument length is: " + q.length);
		for (int i = 0; i < q.length; i++){
			this.args.add(q[i]);
      //System.out.println(q[i].toString());
		}
		this.proximity = n;
	}
	public QryopIlNear(int n) {
		//System.out.println("near initialize once");
		//System.out.println("n value is " + n);
		this.proximity = n;
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
	  //System.out.println("near");
	  //The SYN and NEAR/n query operator implementations are identical 
	  //for all retrieval models (ranked Boolean, BM25, and Indri).
	  //System.out.println("big evaluate once");
	  if(this.args.size()<=0)
		  return null;
    return (evaluateHelper(r));
  }

  /**
   *  Evaluates the query operator for boolean retrieval models,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateHelper (RetrievalModel r) throws IOException {
	  //System.out.println("evaluate once");
    //Initialization
	//inv_list is initialized but not score_list
    allocArgPtrs (r);
    //System.out.println(this.argPtrs.size());
    QryResult result = new QryResult ();
    //only one term in the NEAR query
    if(this.argPtrs.size()<=1){
    	result.invertedList = this.argPtrs.get(0).invList;
    	return result;
    }
    // fields of NEAR terms are assumed to be the same
    // need to check later!!!!!!!!!!!!!!!!*******************!!!!!!!!!!!!!!!!!
    result.invertedList.field = new String (this.argPtrs.get(0).invList.field);
    //System.out.println("field in near is " + result.invertedList.field);
    //  return a document if ALL of the query arguments occur in the document, 
    //  IN ORDER, with no more than n-1 terms separating two adjacent terms
    //System.out.println("before checking documents");
    //big logic error!!!!!!!!!!!!!!!!**********************!!!!!!!!!!!!!!!!
    //make good use of a hash table
    HashMap<Integer, Vector<Integer>> documentMap = new HashMap<Integer, Vector<Integer>>();
    ArgPtr ptr0 = this.argPtrs.get(0);
	//ptr0.invList.print();
	ArgPtr ptr1 = this.argPtrs.get(1);
	//ptr1.invList.print();
	//nextDoc starts from 0
	for ( ; ptr0.nextDoc < ptr0.invList.postings.size(); ptr0.nextDoc ++) {
		int ptr0Docid = ptr0.invList.getDocid (ptr0.nextDoc);
	    Vector<Integer> ptr0Positions = ptr0.invList.getPositions(ptr0.nextDoc);
	    //Vector<Integer> positions = new Vector<Integer>();
	    while (ptr1.nextDoc<ptr1.invList.postings.size()) {
	    	if (ptr1.invList.getDocid (ptr1.nextDoc) > ptr0Docid)
	    		break;	// The ptr0docid can't match.
	    	else if (ptr1.invList.getDocid (ptr1.nextDoc) < ptr0Docid)
	    		ptr1.nextDoc ++;			// Not yet at the right doc.
	    	else{
	    		//System.out.println(ptr0Docid);
	    		Vector<Integer> positions=checkPositions(ptr0Positions, ptr1.invList.getPositions(ptr1.nextDoc));
	    		if ((positions.size())>0){
	    			//System.out.println("doc id 0: " + ptr0Docid);
	    			//System.out.println("doc id 1: " +ptr1.invList.getDocid (ptr1.nextDoc));   	    		
	    			Collections.sort (positions);
	    		    documentMap.put(ptr0Docid, positions);
	    		    //System.out.println(positions.toString());
	    		}// ptrj matches ptr0Docid
	    		ptr1.nextDoc ++;
    			break;
	    	}
	    }
	}
	//System.out.println("size of hashmap after 1&2: " + documentMap.size());
    for(int i=2; i<this.argPtrs.size(); i++){
    	//System.out.println(i);
    	HashMap<Integer, Vector<Integer>> tmpDocumentMap = new HashMap<Integer, Vector<Integer>>();
    	ArgPtr ptr = this.argPtrs.get(i);
    	for ( ; ptr.nextDoc < ptr.invList.postings.size(); ptr.nextDoc ++) {
    		int ptrDocid = ptr.invList.getDocid (ptr.nextDoc);
    		//only check for positions of documents previously filtered out 
    		if(documentMap.containsKey(ptrDocid)){
    			//System.out.println("contains");
    			Vector<Integer> positions = checkPositions(
    					documentMap.get(ptrDocid), ptr.invList.getPositions(ptr.nextDoc));
    			
    			//update hashmap value for this document
    			if ((positions.size())>0){
	    			//System.out.println("doc id 0: " + ptr0Docid);
	    			//System.out.println("doc id 1: " +ptr1.invList.getDocid (ptr1.nextDoc));   	    		
	    			Collections.sort (positions);
	    		    //documentMap.put(ptrDocid, positions);
	    		    tmpDocumentMap.put(ptrDocid, positions);
	    		    //System.out.println(positions.toString());
	    		}
    			//remove entry for this document in the hashmap if no fit found
    			/*else{
    				documentMap.remove(ptrDocid);
    			}*/
    		}
    	}
    	documentMap = tmpDocumentMap;
		//System.out.println("interim: " + tmpDocumentMap.size());
    		
    }
    //System.out.println("size of hashmap after 3: " + documentMap.size());
    Map<Integer, Vector<Integer>> documentSortedMap = new TreeMap<Integer, Vector<Integer>>();
    for(Integer docIdKey : documentMap.keySet()){
    	documentSortedMap.put(docIdKey, documentMap.get(docIdKey));
    }
    for(Integer docIdKey : documentSortedMap.keySet()){
    	result.invertedList.appendPosting(docIdKey, documentSortedMap.get(docIdKey));
    }
    //System.out.println("size of result " + result.invertedList.postings.size());
    freeArgPtrs();
    //System.out.println("inside evaluate size: " +result.invertedList.postings.size());
    return result;
  }
  
  /* Helper function to check if terms positions in one document meet the proximity requirements
   * Return the number of times matched 
   */
  private Vector<Integer> checkPositions(Vector<Integer> term0Pos, Vector<Integer> term1Pos){
	  //System.out.println("enter check position function");
	  Vector<Integer> positions = new Vector<Integer>();
	  int numMatched = 0;
	  int j = 0;
	  //Collections.sort(term1Pos);
	  //Collections.sort(term2Pos);
	  for(int i=0; i<term0Pos.size(); i++){
		  int t0 = term0Pos.get(i);
		  while(j<term1Pos.size()){
			  int t1 = term1Pos.get(j);
			  if (t1>t0 && t1<=(t0+this.proximity)){
				  positions.add(t1);
				  numMatched++;
				  j++;
				  break;
			  }
			  else if(t1<=t0){
				  j++;
			  }
			  else if(t1>t0+this.proximity){
				  break;
			  }
		  }
	  }
	  if(positions.size()!=numMatched)
		  System.out.println("error in algorithm");
	  return positions;
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

    return ("#NEAR/"+this.proximity+"( " + result + ")");
  }
}
