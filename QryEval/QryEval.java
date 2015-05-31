/**
 *  QryEval illustrates the architecture for the portion of a search
 *  engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class QryEval {

  static String usage = "Usage:  java " + System.getProperty("sun.java.command")
      + " paramFile\n\n";

  //  The index file reader is accessible via a global variable. This
  //  isn't great programming style, but the alternative is for every
  //  query operator to store or pass this value, which creates its
  //  own headaches.

  public static IndexReader READER;
  // collection statistics 
  public static int N;
  public static DocLengthStore s; 
  public static HashMap<String, Long> totalTermFreq = new HashMap<String, Long>();
  public static HashMap<String, Integer> totalDocCount = new HashMap<String, Integer>();
  
  //  Create and configure an English analyzer that will be used for
  //  query parsing.

  public static EnglishAnalyzerConfigurable analyzer =
      new EnglishAnalyzerConfigurable (Version.LUCENE_43);
  static {
    analyzer.setLowercase(true);
    analyzer.setStopwordRemoval(true);
    analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
  }
  

  /**
   *  @param args The only argument is the path to the parameter file.
   *  @throws Exception
   */
  public static void main(String[] args) throws Exception {
	long startTime = System.currentTimeMillis();
    // must supply parameter file
    if (args.length < 1) {
      System.err.println(usage);
      System.exit(1);
    }

    // read in the parameter file; one parameter per line in format of key=value
    Map<String, String> params = new HashMap<String, String>();
    Scanner scan = new Scanner(new File(args[0]));
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split("=");
      params.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());
    scan.close();
    
    // parameters required for this example to run
    if (!params.containsKey("indexPath")) {
      System.err.println("Error: Parameters were missing.");
      System.exit(1);
    }

    // open the index
    READER = DirectoryReader.open(FSDirectory.open(new File(params.get("indexPath"))));

    if (READER == null) {
      System.err.println(usage);
      System.exit(1);
    }
    // used for doc length calc in BM25 and Indri
    s = new DocLengthStore(READER);
    // Number of documents in the corpus
    N = QryEval.READER.numDocs();
    //System.out.println ("numdocs=" + N);
    totalTermFreq.put("body", QryEval.READER.getSumTotalTermFreq("body"));
    totalTermFreq.put("url", QryEval.READER.getSumTotalTermFreq("url"));
    totalTermFreq.put("keywords", QryEval.READER.getSumTotalTermFreq("keywords"));
    totalTermFreq.put("title", QryEval.READER.getSumTotalTermFreq("title"));
    totalTermFreq.put("inlink", QryEval.READER.getSumTotalTermFreq("inlink"));
    totalDocCount.put("body", QryEval.READER.getDocCount("body"));
    totalDocCount.put("url", QryEval.READER.getDocCount("url"));
    totalDocCount.put("keywords", QryEval.READER.getDocCount("keywords"));
    totalDocCount.put("title", QryEval.READER.getDocCount("title"));
    totalDocCount.put("inlink", QryEval.READER.getDocCount("inlink"));
    
    //TermVector tv = new TermVector(241087, "body");//1 stands for the docid
    //System.out.println(tv.toString());
    //System.out.println("0th:" + tv.stemString(0)); // get the string for the 10th stem
    //System.out.println("1st:" + tv.stemString(1));
    
    
    // specify input/output file path
    String queryFilePath = params.get("queryFilePath");
    String trecEvalOutputPath = params.get("trecEvalOutputPath");
    // determine the retrieval model
    String retrievalAlgorithm = params.get("retrievalAlgorithm");
    // default model to unranked boolean
    RetrievalModel model=new RetrievalModelUnrankedBoolean();
    if(retrievalAlgorithm.equals("RankedBoolean"))
    	model = new RetrievalModelRankedBoolean();
    else if(retrievalAlgorithm.equals("BM25")){
    	//System.out.println("BM25");
    	// parse the model parameters 
        double k_1 = Double.parseDouble(params.get("BM25:k_1"));
        double b = Double.parseDouble(params.get("BM25:b"));
        double k_3 = Double.parseDouble(params.get("BM25:k_3"));
    	model = new BM25(k_1, b, k_3);
    }
    else if(retrievalAlgorithm.equals("Indri")){
    	//System.out.println("Indri");
    	double mu = Double.parseDouble(params.get("Indri:mu"));
        double lambda = Double.parseDouble(params.get("Indri:lambda"));
    	model = new Indri(mu, lambda);
    }
    else if(retrievalAlgorithm.equals("letor")){
    	//System.out.println("letor algorithm");
    	model = new LeToR();
    	//LeToR needs to train SVM model before classify for better doc ranking
    	// generate training data
    	String trainingQueryFile = params.get("letor:trainingQueryFile");
    	String trainingQrelsFile = params.get("letor:trainingQrelsFile");
    	String trainingFeatureVectorsFile = params.get("letor:trainingFeatureVectorsFile");
    	String pageRankFile = params.get("letor:pageRankFile");
    	String featureDisable = params.get("letor:featureDisable");
    	((LeToR) model).setTrainingParameters(trainingQueryFile, trainingQrelsFile, trainingFeatureVectorsFile, pageRankFile, featureDisable);
    	double k_1 = Double.parseDouble(params.get("BM25:k_1"));
        double b = Double.parseDouble(params.get("BM25:b"));
        double k_3 = Double.parseDouble(params.get("BM25:k_3"));
        double mu = Double.parseDouble(params.get("Indri:mu"));
        double lambda = Double.parseDouble(params.get("Indri:lambda"));
        ((LeToR) model).setBM25Parameters(k_1, b, k_3);
        ((LeToR) model).setIndriParameters(mu, lambda);
        //System.out.println("letor parameters read");
    	((LeToR) model).generateTrainingData();
    	//System.out.println("done training");
    	String learnPath = params.get("letor:svmRankLearnPath");
		double FEAT_GEN = Double.valueOf(params.get("letor:svmRankParamC"));
		String modelOutputFile = params.get("letor:svmRankModelFile");
		String classifyPath = params.get("letor:svmRankClassifyPath");
		((LeToR)model).setSVMParameters(learnPath, FEAT_GEN, modelOutputFile, classifyPath);
		//System.out.println("svm parameters read");
		//to train a model
		((LeToR)model).runSVMRank(true);
    	//System.exit(0);
		String testingFeatureVectorsFile = params.get("letor:testingFeatureVectorsFile");
    	String testingDocumentScores = params.get("letor:testingDocumentScores");
    	((LeToR)model).setTestingParameters(testingFeatureVectorsFile, testingDocumentScores);
    }
    
    
    // determine the parameters used in query expansion 
    boolean fb = false;
    if(params.containsKey("fb") && params.get("fb").equals("true"))
    	fb = true;
    //System.out.println(fb);
    int fbDocs = 0;
    if(params.containsKey("fbDocs"))
    	fbDocs = Integer.parseInt(params.get("fbDocs"));
    //System.out.println(fbDocs);
    int fbTerms = 0;
    if(params.containsKey("fbTerms"))
    	fbTerms = Integer.parseInt(params.get("fbTerms"));
    //System.out.println(fbTerms);
    int fbMu = 0;
    if(params.containsKey("fbMu"))
    	fbMu = Integer.parseInt(params.get("fbMu"));
    //System.out.println(fbMu);
    double fbOrigWeight = 0.0;
    if(params.containsKey("fbOrigWeight"))
    	fbOrigWeight = Double.parseDouble(params.get("fbOrigWeight"));
    //System.out.println(fbOrigWeight);
    String fbInitialRankingFile = null;
    if(params.containsKey("fbInitialRankingFile"))
    	fbInitialRankingFile = params.get("fbInitialRankingFile");
    String fbExpansionQueryFile = null;
    if(params.containsKey("fbExpansionQueryFile"))
    	fbExpansionQueryFile = params.get("fbExpansionQueryFile");
    
   
    
    // open query file for processing
    File queryFile = new File(queryFilePath);
    BufferedReader br = null;
    //trec output writer 
    BufferedWriter bw = new BufferedWriter(new FileWriter(new File(trecEvalOutputPath)));
    BufferedWriter featureVectorsWriter = null;
    if(model instanceof LeToR){
    	featureVectorsWriter = new BufferedWriter(new FileWriter(new File(params.get("letor:testingFeatureVectorsFile"))));
    
    }
    try{
    	br = new BufferedReader(new FileReader(queryFile));
    	String queryLine = null;
    	while((queryLine = br.readLine())!=null){
    		// do nothing if empty line or wrongly formatted line
    		if (queryLine.trim().equals("") || !queryLine.trim().contains(":")) {
                //System.out.println("do nothing for invalid line");
    			continue;
            }
    	    
    		// parse to get queryID and queryContent
    		String queryID = queryLine.split(":")[0].trim();
    		//System.out.println("QID: " + queryID);
    		String queryContent = queryLine.split(":")[1].trim();
    		
    		//if feedback mechanism is set to true, go over the query expansion process
    		if(fb){
    			//System.out.println("Q: " + queryContent);
    			//data structure to hold top fbDocs initial retrieved docs
    			ScoreList initialScoreList = new ScoreList();
    			Qryop initialQryOperator = parseQuery(queryContent, model);
    			String initialQry = initialQryOperator.toString();
    			QryExpansion expander = new QryExpansion();
				
    			if((fbInitialRankingFile!=null)){
    				System.out.println("read document ranking in trec_eval input format");
    				//read document ranking in trec_eval input format 
    				File initialRankingFile = new File(fbInitialRankingFile);
    				BufferedReader initialRankingFileReader = new BufferedReader(
    	    			new FileReader(initialRankingFile));   				
    				String initialRankingLine = null;
    				int docCount = 0;//only need to retrieve the top fbDocs documents
    				while((initialRankingLine = initialRankingFileReader.readLine())!=null && docCount < fbDocs){
    					String[] elements = initialRankingLine.split(" ");
    					if(!elements[0].equals(queryID))
    						continue;//do nothing if queryID mismatch
    					else{
    						int internalDocid = getInternalDocid(elements[2]);
    						double docScore = Double.parseDouble(elements[4]);
    						initialScoreList.add(internalDocid, docScore);
    						docCount++;
    					} 		
    				}
    				if(initialRankingFileReader!=null)
    					initialRankingFileReader.close();
    			}
    			else{
    				System.out.println("use the query to retrieve documents");
    				//use the query to retrieve documents
    				//System.out.println(qTree.toString());
    				QryResult result = initialQryOperator.evaluate(model);
    				//no need to map whole iid to eid if only want to retrieve top fbDocs documents
    				result.docScores.sort();
    				for(int i=0; (i<fbDocs)&&(i<result.docScores.scores.size()); i++){
    					initialScoreList.add(result.docScores.getDocid(i), result.docScores.getDocidScore(i));
    				}    	       
    			}
    			System.out.println("retrieved top " + initialScoreList.scores.size() + " docs...");
    			System.out.println(queryContent);
    			expander = new QryExpansion(initialScoreList);
    			expander.setMu(fbMu);
    			expander.setTerms(fbTerms);
    			String expandedQry = null;
    			try {
    				
    				// evaluate the parse query tree
    				LinkedHashMap<String, Double> selectedTermScores = expander.evaluate(model);
    				if (selectedTermScores.size() < 1) {
    					System.out.println("No terms found...");
    				}
    				else{
    					
    					expandedQry = "#wand ( ";
    					int i=0;
    					for (String term : selectedTermScores.keySet()) {
    						//System.out.println("the " + i + "th term: " + term);
    						String scoreString = String.format("%.4f", selectedTermScores.get(term));
    						expandedQry += (scoreString + " " + term + " ");
    						i++;
    					}
    					expandedQry+=")";
    					
    					String combinedQry = "#wand ( " + fbOrigWeight + " " + initialQry 
    	    					+ " " + (1-fbOrigWeight) + " " + expandedQry + " )";
    	    			System.out.println("combined qry is " + combinedQry);
    	    			queryContent = combinedQry;
    	    			
    	    			if(fbExpansionQueryFile!=null){
    	    				//expanded qry writer
    	    				BufferedWriter expansionTermsWriter = new BufferedWriter(
    	    						new FileWriter(new File(fbExpansionQueryFile)));
    	    				expansionTermsWriter.write(queryID+": ");
    	    				expansionTermsWriter.write(expandedQry+"\n");
    	    				expansionTermsWriter.close();
    	    			}
    				}
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		}
    		Qryop qTree = null;
    		if(model instanceof LeToR){
    			qTree = parseQuery(queryContent, new BM25(((LeToR) model).getK1(), 
    					((LeToR) model).getB(), ((LeToR) model).getK3()));
    		}
    		else{
    			qTree = parseQuery(queryContent,model);
    		}
    		try {
    			//System.out.println("query after parsing: " + qTree.toString());
    			// evaluate the parse query tree
    			QryResult result;
    			
    			if(model instanceof LeToR){
    				result = qTree.evaluate(new BM25(((LeToR) model).getK1(), 
        					((LeToR) model).getB(), ((LeToR) model).getK3()));
    			}
    			else
    				result = qTree.evaluate(model);
    			
    			//System.out.println("query after evaluation: " + qTree.toString());
    			// sort the result score list, first by score then by external docid
    			HashMap<Integer, String> docIdMap = new HashMap<Integer, String>();
    			mapInternalIdToExternal(result.docScores, docIdMap);
    	        result.docScores.sort(docIdMap);
    	        //Collections.sort(result.docScores.scores,Collections.reverseOrder(), new ScoreListEntryComparator());
    	    	//System.out.println("result size is " + result.docScores.scores.size());
    	        if (result.docScores.scores.size() < 1) {
    	        	bw.write(queryID+" Q0 dummy 1 0 run-1\n");  	        	
    	    	}
    	    	else {
    	    		if(!(model instanceof LeToR)){
    	    			for (int i = 0; i < result.docScores.scores.size() && i < 100; i++) {
    	    				String scoreString = String.format("%.12f", result.docScores.getDocidScore(i));
    	    				String content = queryID + " Q0 " + 
    	    					getExternalDocid (result.docScores.getDocid(i))
    	            			  + " " + (i+1) + " "
    	            			  + scoreString +" fubar\n";
    	    				bw.write(content);  	    						
        	    		}
    	    		}else{
    	    			LinkedHashMap<String, Integer> initDocDegreeMap = new LinkedHashMap<String, Integer>();
        	    		for (int i = 0; i < result.docScores.scores.size() && i < 100; i++) {			
        	    			initDocDegreeMap.put(getExternalDocid (result.docScores.getDocid(i)), 0);
        	    		}
        	    		if((model instanceof LeToR)){
        	    			((LeToR)model).generateFVsForQuery(Integer.parseInt(queryID), queryContent, initDocDegreeMap, featureVectorsWriter);
        	    		}
        	    	}
        	    		    	    		
    	    	}
    	    } catch (Exception e) {
    	    	e.printStackTrace();
    	    } 
    	}
    }catch (FileNotFoundException e) {
        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    } finally {
        try {
            if (br != null) {
                br.close();
            }
        } catch (IOException e) {
        }
        try {
        	if(model instanceof LeToR){
        		featureVectorsWriter.close();
        		//((LeToR)model).generateData(false);
        		//classify using the trained model
        		((LeToR)model).runSVMRank(false);
        		//((LeToR)model).setReRankingParameter(trecEvalOutputPath);
        		((LeToR)model).doReranking(bw);
        		bw.close();
        	}
        	else{
        		bw.close();
        	}
        } catch (IOException e) {
        }
    }

    // Later HW assignments will use more RAM, so you want to be aware
    // of how much memory your program uses.

    printMemoryUsage(false);
    long endTime   = System.currentTimeMillis();
    long totalTime = endTime - startTime;
    System.out.println(totalTime);

  }
  

  /**
   *  Write an error message and exit.  This can be done in other
   *  ways, but I wanted something that takes just one statement so
   *  that it is easy to insert checks without cluttering the code.
   *  @param message The error message to write before exiting.
   *  @return void
   */
  static void fatalError (String message) {
    System.err.println (message);
    System.exit(1);
  }

  /**
   *  Get the external document id for a document specified by an
   *  internal document id. If the internal id doesn't exists, returns null.
   *  
   * @param iid The internal document id of the document.
   * @throws IOException 
   */
  static String getExternalDocid (int iid) throws IOException {
	    Document d = QryEval.READER.document (iid);
	    String eid = d.get ("externalId");
	    return eid;
	  }
  /**
   *  Map the internal doc id of extracted docs to their external document id 
   *  for sorting
   *  
   * @param iid The internal document id of the document.
   * @throws IOException 
   */
  static void mapInternalIdToExternal (ScoreList list, HashMap<Integer, String> map) throws IOException {
	    for(int i=0; i<list.scores.size();i++){
	    	int internalId = list.getDocid(i);
	    	String externalId = getExternalDocid(internalId);
	    	//if(externalId.equals("clueweb09-en0006-39-34119"))
	    	//		System.out.println("internal id for clueweb09-en0006-39-34119 is " + internalId);
	    	map.put(internalId, externalId);
	    	
	    }
  }
  /**
   *  Finds the internal document id for a document specified by its
   *  external id, e.g. clueweb09-enwp00-88-09710.  If no such
   *  document exists, it throws an exception. 
   * 
   * @param externalId The external document id of a document.s
   * @return An internal doc id suitable for finding document vectors etc.
   * @throws Exception
   */
  static int getInternalDocid (String externalId) throws Exception {
    Query q = new TermQuery(new Term("externalId", externalId));
    
    IndexSearcher searcher = new IndexSearcher(QryEval.READER);
    TopScoreDocCollector collector = TopScoreDocCollector.create(1,false);
    searcher.search(q, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;
    
    if (hits.length < 1) {
      throw new Exception("External id not found.");
    } else {
      return hits[0].doc;
    }
  }

  /**
   * parseQuery converts a query string into a query tree.
   * 
   * @param qString
   *          A string containing a query.
   * @param qTree
   *          A query tree
   * @throws IOException
   */
  static Qryop parseQuery(String qString, RetrievalModel model) throws IOException {

    Qryop currentOp = null;
    // used for DFSs
    Stack<Qryop> stack = new Stack<Qryop>();

    // Add a default query operator to an unstructured query. This
    // is a tiny bit easier if unnecessary whitespace is removed.

    qString = qString.trim();
    qString = qString.toLowerCase();
    //System.out.println(qString);
    // default to different operators for different models
    if(model instanceof Indri){
    	if(((!qString.startsWith("#and")) && (!qString.startsWith("#wand")) && (!qString.startsWith("#wsum")))
    			||!qString.endsWith(")"))
    		qString = "#and(" + qString + ")";
    }
    else if (model instanceof BM25){
    	if(!qString.startsWith("#sum")||!qString.endsWith(")"))
    		qString = "#sum(" + qString + ")";
    }
    else{
    	if(!qString.startsWith("#")||!qString.endsWith(")"))
    		qString = "#or(" + qString + ")";
    }
    System.out.println(qString);
   

    // Tokenize the query.
    StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
    String token = null;
    
    // Each pass of the loop processes one token. To improve
    // efficiency and clarity, the query operator on the top of the
    // stack is also stored in currentOp.

    while (tokens.hasMoreTokens()) {
      
      token = tokens.nextToken();
      //System.out.println(token);
      if (token.matches("[ ,(\t\n\r]")) {
        continue;
      } 
      if (token.equals("#and")) {
    	  currentOp = new QryopSlAnd();
    	  stack.push(currentOp);
      }else if (token.equals("#wsum")) {
    	  currentOp = new QryopSlWsum();
    	  stack.push(currentOp);
      }else if (token.equals("#wand")) {
    	  currentOp = new QryopSlWand();
    	  stack.push(currentOp);
      }else if (token.equals("#or")) {
          currentOp = new QryopSlOr();
          stack.push(currentOp);
      } else if(token.matches("(#near/)(\\d+)")){
    	  //System.out.println("near matched");
    	  currentOp = new QryopIlNear(Integer.parseInt(token.substring(6,token.length())));
    	  stack.push(currentOp);
      } else if (token.equals("#syn")) {
    	  currentOp = new QryopIlSyn();
    	  stack.push(currentOp);
      } else if (token.equals("#sum")) {
    	  currentOp = new QryopSlSum();
    	  stack.push(currentOp);
      }else if (token.matches("(#window/)(\\d+)")){
    	  currentOp = new QryopIlWindow(Integer.parseInt(token.substring(8,token.length())));
    	  stack.push(currentOp);
      }
      else if (token.equals(")")) { // Finish current query operator.
        // If the current query operator is not an argument to
        // another query operator (i.e., the stack is empty when it
        // is removed), we're done (assuming correct syntax - see
        // below). Otherwise, add the current operator as an
        // argument to the higher-level operator, and shift
        // processing back to the higher-level operator.

        stack.pop();

        if (stack.empty())
          break;

        Qryop arg = currentOp;
        currentOp = stack.peek();
        currentOp.add(arg);
      } else {
    	  
    	  //System.out.println(token);
    	  //System.out.println("token count" + tokenCount);
    	  // NOTE: You should do lexical processing of the token before
    	  // creating the query term, and you should check to see whether
    	  // the token specifies a particular field (e.g., apple.title).
    	  
    	  /*if(currentOp instanceof QryopSlWand || currentOp instanceof QryopSlWsum){
    		  tokenCount++;
    		  System.out.println(tokenCount+"|"+token);
    	  }*/
    	  
    	  if(currentOp instanceof QryopSlWand && ((QryopSlWand) currentOp).isExpectingWeight()){
    		  //System.out.println("weight: " + token);
    		  if(isNumeric(token)){
    			  ((QryopSlWand)currentOp).addWeight(Double.parseDouble(token));
    			  //System.out.println("weight:"+token);
    		  }
    		  else{
    			  System.out.println("invalid weight...ERROR" + token);
    			  return null;
    		  }
    		  
    		  
    	  }
    	  else if (currentOp instanceof QryopSlWsum && ((QryopSlWsum) currentOp).isExpectingWeight()){
    		  //System.out.println("weight: " + token);
    		  if(isNumeric(token)){
    			  ((QryopSlWsum)currentOp).addWeight(Double.parseDouble(token));
    			  //System.out.println("weight:"+token);
    			  
    		  }
    		  else{
    			  System.out.println("invalid weight...ERROR" + token);
    			  return null;
    		  }
    		  
    		  
    	  }
    	  else{
    		int indexOfDot = token.indexOf(".");
    	  
    	  	int tokenLen = token.length();
  			if(indexOfDot<0){
  				//System.out.println("no dot");
  				//If a query term has no explicit field (see below), default to 'body'; and
  				String[] pToken = tokenizeQuery(token);
  				//do nothing is the token becomes empty after stemming
  				if (pToken.length>0)
  					currentOp.add(new QryopIlTerm(pToken[0]));
  				else if(currentOp instanceof QryopSlWand){
  					((QryopSlWand)currentOp).removeLastWeight();				
  				}
  				else if(currentOp instanceof QryopSlWsum){
  					((QryopSlWsum)currentOp).removeLastWeight();
  					
  				}
  				
  			}
  			else if(indexOfDot==0 || indexOfDot==(tokenLen-1)){
  				System.out.println("incorrect query syntax " + qString + " ...ERROR");
  				return null;
  			}
  			else{
  				String term = token.substring(0, indexOfDot);
  				String field = token.substring(indexOfDot+1, tokenLen);
  				String[] pToken = tokenizeQuery(term);
  				if (pToken.length>0){
  					//System.out.println(currentOp.toString());
  					currentOp.add(new QryopIlTerm(pToken[0], field));
  					//System.out.println(currentOp.toString());
  				}
  				else if(currentOp instanceof QryopSlWand){
  					((QryopSlWand)currentOp).removeLastWeight();
  				}
  				else if(currentOp instanceof QryopSlWsum){
  					((QryopSlWsum)currentOp).removeLastWeight();
  				}
  			}
    	  }
    	  
      }
    }

    // A broken structured query can leave unprocessed tokens on the
    // stack, so check for that.

    if (tokens.hasMoreTokens()) {
      System.out.println("incorrect query syntax " + qString + " ...ERROR" );
      return null;
    }

    return currentOp;
  }
  
  public static boolean isNumeric(String str)  
  {  
    try  
    {  
      double d = Double.parseDouble(str);  
    }  
    catch(NumberFormatException nfe)  
    {  
      return false;  
    }  
    return true;  
  }

  

  /**
   *  Print a message indicating the amount of memory used.  The
   *  caller can indicate whether garbage collection should be
   *  performed, which slows the program but reduces memory usage.
   *  @param gc If true, run the garbage collector before reporting.
   *  @return void
   */
  public static void printMemoryUsage (boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc) {
      runtime.gc();
    }

    System.out.println ("Memory used:  " +
			((runtime.totalMemory() - runtime.freeMemory()) /
			 (1024L * 1024L)) + " MB");
  }
  
  /**
   * Print the query results. 
   * 
   * THIS IS NOT THE CORRECT OUTPUT FORMAT.  YOU MUST CHANGE THIS
   * METHOD SO THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK
   * PAGE, WHICH IS:
   * 
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param queryName Original query.
   * @param result Result object generated by {@link Qryop#evaluate()}.
   * @throws IOException 
   */
  static void printResults(String queryName, QryResult result) throws IOException {

    System.out.println(queryName + ":  ");
    //if(result == null)
    //	System.out.println("result is null");
    
    if (result.docScores.scores.size() < 1) {
      System.out.println("\tNo results.");
    } else {
      for (int i = 0; i < result.docScores.scores.size(); i++) {
        System.out.println("\t" + i + ":  "
			   + getExternalDocid (result.docScores.getDocid(i))
			   + ", "
			   + result.docScores.getDocidScore(i));
      }
    }
  }
  

  /**
   *  Given a query string, returns the terms one at a time with stopwords
   *  removed and the terms stemmed using the Krovetz stemmer. 
   * 
   *  Use this method to process raw query terms. 
   * 
   *  @param query String containing query
   *  @return Array of query tokens
   *  @throws IOException
   */
  static String[] tokenizeQuery(String query) throws IOException {

    TokenStreamComponents comp = analyzer.createComponents("dummy", new StringReader(query));
    TokenStream tokenStream = comp.getTokenStream();

    CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();

    List<String> tokens = new ArrayList<String>();
    while (tokenStream.incrementToken()) {
      String term = charTermAttribute.toString();
      tokens.add(term);
    }
    return tokens.toArray(new String[tokens.size()]);
  }
}
