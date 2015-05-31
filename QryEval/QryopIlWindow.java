/**
 *  This class implements the WINDOW operator for all retrieval models.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopIlWindow extends QryopIl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopNear (n, arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
	private int proximity = 0;
	public QryopIlWindow(int n, Qryop... q) {
		//System.out.println("argument length is: " + q.length);
		for (int i = 0; i < q.length; i++){
			this.args.add(q[i]);
			//System.out.println(q[i].toString());
		}
		this.proximity = n;
		//System.out.println("priximity is " + this.proximity);
	}
	public QryopIlWindow(int n) {
		//System.out.println("initialize once");
		//System.out.println("n value is " + n);
		this.proximity = n;
		//System.out.println("priximity is " + this.proximity);
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
	  //#Window/n supports both BM25 and Indri models
    //System.out.println("window");
	  if(this.args.size()<=0)
		  return null;
	  if(r instanceof BM25)
		  return evaluateHelper(r);
		  //return evaluateWindow(r);
	  else if(r instanceof Indri)
		  return evaluateHelper(r);
	  else{
		  System.out.println("model type mismatch....ERROR");
		  return null;
	  }
  }
  
 

  /**
   *  Evaluates the query operator for BM25/Indri retrieval models,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateHelper (RetrievalModel r) throws IOException {
	  
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
	  
	  result.invertedList.field = new String (this.argPtrs.get(0).invList.field);
	  //System.out.println("field in window is " + result.invertedList.field);
	  //make good use of a hash table
	  //internal doc index for each shared docid
	  HashMap<Integer, ArrayList<Integer>> documentMap = new HashMap<Integer, ArrayList<Integer>>();
	  //initialize documentMap with first arg
	  ArgPtr ptr0 = this.argPtrs.get(0);
	  for ( ; ptr0.nextDoc < ptr0.invList.postings.size(); ptr0.nextDoc ++) {
		  ArrayList<Integer> docInxList = new ArrayList<Integer>();
		  docInxList.add(ptr0.nextDoc);
		  documentMap.put(ptr0.invList.getDocid (ptr0.nextDoc), docInxList);
	  }
	  //update documentMap to include only docid containing all terms 
	  for(int i=1; i<this.argPtrs.size(); i++){
		  
		  HashMap<Integer, ArrayList<Integer>> tmpDocumentMap = new HashMap<Integer, ArrayList<Integer>>();
		  ArgPtr ptr = this.argPtrs.get(i);
		  for ( ; ptr.nextDoc < ptr.invList.postings.size(); ptr.nextDoc ++) {
			  int ptrDocid = ptr.invList.getDocid (ptr.nextDoc);
			  //only check for positions of documents previously filtered out 
			  if(documentMap.containsKey(ptrDocid)){
				  ArrayList<Integer> tmpDocInxList = documentMap.get(ptrDocid);
				  tmpDocInxList.add(ptr.nextDoc);
				  tmpDocumentMap.put(ptrDocid, tmpDocInxList);
			  }
		  }
		  documentMap = tmpDocumentMap;
	  }
	  
	  Map<Integer, Vector<Integer>> documentSortedMap = new TreeMap<Integer, Vector<Integer>>();
	  for(Integer docIdKey : documentMap.keySet()){
		  ArrayList<Integer> docIndexList = documentMap.get(docIdKey);
		  //collections of term positions for docIdKey 
		  ArrayList<Vector<Integer>> positions = new ArrayList<Vector<Integer>>();
		  if(docIndexList.size()!=this.argPtrs.size()){
			  System.out.println("arg size mismatch...ERROR");
			  return null;
		  }
		  else{
			  for(int i=0; i<docIndexList.size(); i++){
				  positions.add(this.argPtrs.get(i).invList.getPositions(docIndexList.get(i)));
			  }
		  }
		  //check if positions collections fulfill window range requirement
		  Vector<Integer> windowPositions = checkIfWithinWindow(positions);
		  if(windowPositions.size()>0)
			  documentSortedMap.put(docIdKey, windowPositions);
	  }
	  
	  for(Integer docIdKey : documentSortedMap.keySet()){
		  result.invertedList.appendPosting(docIdKey, documentSortedMap.get(docIdKey));
	  }
	  //System.out.println("size of result " + result.invertedList.postings.size());
	  freeArgPtrs();
	  
	  return result;
  	}
  
  
  public Vector<Integer> checkIfWithinWindow(ArrayList<Vector<Integer>> positions){
	  
	  Vector<Integer> windowPositions = new Vector<Integer>();
	  int size = positions.size();
	  //idx for each positions vector 
	  int[] posIdxArr = new int[size];
	  //terminate checking when one vector idx hits end
	  boolean flag = true;
	  while(flag){
		  HashMap<Integer, Integer> posDocMap = new HashMap<Integer, Integer>();
		  ArrayList<Integer> posToCheck = new ArrayList<Integer>();
	  
		  for(int i=0; i<size; i++){
			  //System.out.println("posIdxArr for " + i + " is " + posIdxArr[i]);
			  //System.out.println("positions size for " + i + " is " + positions.get(i).size());
			  if(posIdxArr[i]>=positions.get(i).size()){
				  //System.out.println("break?");
				  flag = false;
				  break;
			  }
			  int curPos = positions.get(i).get(posIdxArr[i]);
			  posDocMap.put(curPos, i);
			  posToCheck.add(curPos);
			  //System.out.println("posToCheck size " + posToCheck.size());
		  }
		  if(posToCheck.size()!=size){
			  //System.out.println("size mismatch...ERROR" + posToCheck.size() + "|" + size);
			  //return null;
			  break;
		  }
		  //range check only need to be done on min and max pos
		  Collections.sort(posToCheck);
		  if((posToCheck.get(posToCheck.size()-1)-posToCheck.get(0))<this.proximity){
			  windowPositions.add(posToCheck.get(posToCheck.size()-1));
			  //update all position index
			  for(int j=0; j<size; j++){
				  posIdxArr[j]++;
			  }
		  }
		  else{
			  //only update idx for term with min pos
			  int argId = posDocMap.get(posToCheck.get(0));
			  posIdxArr[argId]++;
		  }
	  }
	  
	  return windowPositions;
	  
  }
  

  /*
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (int i=0; i<this.args.size(); i++)
      result += this.args.get(i).toString() + " ";

    return ("#WINDOW/"+this.proximity+"( " + result + ")");
  }
}
