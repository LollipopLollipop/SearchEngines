import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.document.Document;

/**
 *  The BM25 model has no parameters.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */
public class LeToR extends RetrievalModel{

	/*private List<FeatureVector> fvList = new ArrayList<FeatureVector>();
	private FeatureVector curMinFeatures = new FeatureVector();
	private FeatureVector curMaxFeatures = new FeatureVector();*/
	private double k_1 = 0;
	private double b = 0;
	private double k_3 = 0;
	private double mu = 0;
	private double lambda = 0;
	private String trainingQueryFilePath;
	private String trainingQrelsFilePath;
	private String trainingFeatureVectorsFilePath;
	private int[] featureDisableList = new int[18];
	private String pageRankFilePath;
	private String testingFeatureVectorsFilePath;
	private String testingDocumentScoresPath;
	private String svmRankLearnPath;
	private String svmRankClassifyPath;
	private double FEAT_GEN = 0.0;
	private String modelOutputFile;
	private HashMap<String, Double> pageRankMap = new HashMap<String, Double>();
	private HashMap<String, Integer> docIdMap = new HashMap<String, Integer>();
	public LeToR(){
	}
	public void setBM25Parameters(double k_1, double b, double k_3){
		this.k_1 = k_1;
		this.b = b;
		this.k_3 = k_3;
	}
	public void setIndriParameters(double mu, double lambda){
		this.mu = mu;
		this.lambda = lambda;
	}
	public void setSVMParameters(String learnPath, double FEAT_GEN, String modelOutputFile, String classifyPath){
		this.svmRankLearnPath = learnPath;
		this.FEAT_GEN = FEAT_GEN;
		this.modelOutputFile = modelOutputFile;
		this.svmRankClassifyPath = classifyPath;
	}
	public double getK1(){
		return this.k_1;
	}
	public double getB(){
		return this.b;
	}
	public double getK3(){
		return this.k_3;
	}
	public void setTrainingParameters(String trainingQueryFile, String trainingQrelsFile, String trainingFeatureVectorsFile, 
			String pageRankFile, String featureDisable){
		this.trainingQueryFilePath = trainingQueryFile;
		this.trainingQrelsFilePath = trainingQrelsFile;
		this.trainingFeatureVectorsFilePath = trainingFeatureVectorsFile;
		this.pageRankFilePath = pageRankFile;
		//this.featureDisableList = featureDisable;
		if(featureDisable!=null){
			System.out.println(featureDisable);
			if(featureDisable.contains(",")){
				String[] disabledFeatures = featureDisable.split(",");
				for(int i = 0; i<disabledFeatures.length; i++){
					this.featureDisableList[Integer.parseInt(disabledFeatures[i])-1] = 1;
				}
			}
			else{
				String disabledFeature = featureDisable;
				this.featureDisableList[Integer.parseInt(disabledFeature)-1] = 1;
			}						
		}
	}
	public void setTestingParameters(String testingFeatureVectorsFile, String testingDocumentScores){
		this.testingFeatureVectorsFilePath = testingFeatureVectorsFile;
		this.testingDocumentScoresPath = testingDocumentScores;
	}
	private void parsePageRankFile(){
	//read the PageRank feature from a file and store in HashMap for fast search
	File pageRankFile = new File(this.pageRankFilePath);
	BufferedReader pageRankReader = null;
	try {
		pageRankReader = new BufferedReader(new FileReader(pageRankFile));		
		String pageRankLine = null;
		while((pageRankLine = pageRankReader.readLine())!=null){
			String[] pageRankElements = pageRankLine.split("\t");
			String externalDocID = pageRankElements[0];
			//System.out.println(externalDocID);
			try{
				double pageRankScore = Double.parseDouble(pageRankElements[1]);
				this.pageRankMap.put(externalDocID, pageRankScore);	
				int internalDocID = QryEval.getInternalDocid(externalDocID);
				this.docIdMap.put(externalDocID, internalDocID);
			}catch (Exception e){
				//discard the pagerank entry if the eid does not have corresponding iid 
				//e.printStackTrace();
				//System.out.println("internal id for " + externalDocID + " is not found...DISCARD");
				continue;
			}
				
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public void doReranking(BufferedWriter bw){		
		//testingReferenceFilePath init ranking
		//testingDocumentScoresPath score produced after svm
		
		//read doc score file to array for fast access
		File docScoreFile = new File(this.testingDocumentScoresPath);
		BufferedReader docScoreReader = null;
		List<Double> scoreList = new ArrayList<Double>();;
		try {
			docScoreReader = new BufferedReader(new FileReader(docScoreFile));
			String scoreLine = null;
			while((scoreLine=docScoreReader.readLine())!=null){
				scoreList.add(Double.parseDouble(scoreLine)); 
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			try {
				docScoreReader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//read the initial ranking file
		File initRankingFVFile = new File(this.testingFeatureVectorsFilePath);
		BufferedReader initRankingFVReader = null;
		try {
			initRankingFVReader = new BufferedReader(new FileReader(initRankingFVFile));	
			String initRankingFVLine = null;
			int prevQueryID = 0;
			int lineCount = 0;
			HashMap<String, Double> docScoreMap = new HashMap<String, Double>();			
			while((initRankingFVLine=initRankingFVReader.readLine())!=null){
				String[] initRankingFVLineElements = initRankingFVLine.split(" ");
				int curQueryID = Integer.parseInt(initRankingFVLineElements[1].split(":")[1]);
				if(prevQueryID==0)
					prevQueryID = curQueryID;
				else if((curQueryID!=prevQueryID)){
					//sort and write
					PairComparator pCmp = new PairComparator(docScoreMap);
					TreeMap<String, Double> sortedScoreDocMap = new TreeMap<String, Double>(pCmp);
					sortedScoreDocMap.putAll(docScoreMap);
					int i = 0;
					for(String eid : sortedScoreDocMap.keySet()){
						String content = prevQueryID + " Q0 " + 
	    					eid + " " + (i+1) + " "
	            			  + String.valueOf(sortedScoreDocMap.get(eid)) +" fubar\n";
						bw.write(content);
						i++;
					}
					docScoreMap = new HashMap<String, Double>();
					prevQueryID = curQueryID;
				}
				
				String externalDocID = initRankingFVLine.split("#")[1].trim();
				//double originalScore = Double.parseDouble(initRankingLineElements[4]);
				double newScore = scoreList.get(lineCount);
				docScoreMap.put(externalDocID, newScore);
				lineCount++;
				
			}
			PairComparator pCmp = new PairComparator(docScoreMap);
			TreeMap<String, Double> sortedScoreDocMap = new TreeMap<String, Double>(pCmp);
			sortedScoreDocMap.putAll(docScoreMap);
			int i = 0;
			for(String eid : sortedScoreDocMap.keySet()){
				String content = prevQueryID + " Q0 " + 
					eid + " " + (i+1) + " "
        			  + String.valueOf(sortedScoreDocMap.get(eid)) +" fubar\n";
				bw.write(content);
				i++;
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			
			try {
				initRankingFVReader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public void generateTrainingData(){
		File queryFile = null;
		File referenceFile = null;
	    BufferedReader queryReader = null;
	    //BufferedReader referenceReader = null;
	    BufferedWriter featureVectorsWriter = null;					
		parsePageRankFile();
		queryFile = new File(this.trainingQueryFilePath);
		referenceFile = new File(this.trainingQrelsFilePath);
	    try{
	    	featureVectorsWriter = new BufferedWriter(new FileWriter(new File(this.trainingFeatureVectorsFilePath)));	
	    	queryReader = new BufferedReader(new FileReader(queryFile));
	    	String queryLine = null;
	    	while((queryLine = queryReader.readLine())!=null){
	    		if (queryLine.trim().equals("") || !queryLine.trim().contains(":")) {
	                //System.out.println("do nothing for invalid query line");
	    			continue;
	            }
	    		// parse to get queryID and queryContent
	    		int queryID = Integer.parseInt(queryLine.split(":")[0].trim());
	    		String queryContent = queryLine.split(":")[1].trim();
	    		BufferedReader referenceReader = new BufferedReader(new FileReader(referenceFile));
	    		String referenceLine = null;	
	    		LinkedHashMap<String, Integer> docDegreeMap = new LinkedHashMap<String, Integer>();
	    		while((referenceLine = referenceReader.readLine())!=null){
	    			String[] referenceLineElements = referenceLine.split(" ");
	    			if(Integer.parseInt(referenceLineElements[0])!=queryID)
	    				continue;//only process relevance judgetment for this qry
	    			//FeatureVector fv = new FeatureVector(queryID);
	    			String externalDocId = referenceLineElements[2];
	    			int relevanceDegree = Integer.parseInt(referenceLineElements[3]);
	    			//System.out.println(externalDocId);
	    			//fv.setExternalDocID(referenceLineElements[2]);
	    			docDegreeMap.put(externalDocId, relevanceDegree);
	    		}	
	    		generateFVsForQuery(queryID, queryContent, docDegreeMap, featureVectorsWriter);	 
	    		referenceReader.close();
	    	}
	    }catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            if (queryReader != null) {
	            	queryReader.close();
	            }
	            /*if (referenceReader != null) {
	            	referenceReader.close();
	            }*/
	        } catch (IOException e) {
	        }
	        try {
	        	featureVectorsWriter.close();
	        } catch (IOException e) {
	        }
	    }
	}

	public void generateFVsForQuery(int queryID, String queryContent, LinkedHashMap<String, Integer> docDegreeMap, BufferedWriter featureVectorsWriter) throws IOException{		
		List<FeatureVector> fvList = new ArrayList<FeatureVector>();
		FeatureVector curMinFeatures = new FeatureVector(queryID);
		FeatureVector curMaxFeatures = new FeatureVector(queryID);
		String[] queryStems;
		queryStems = QryEval.tokenizeQuery(queryContent);
		List<String> queryStemsList = Arrays.asList(queryStems);
		int qrySize = queryStems.length;
		HashSet<String> queryStemsSet = new HashSet<String>(queryStemsList);
		for(String externalDocId : docDegreeMap.keySet()){
			FeatureVector fv = new FeatureVector(queryID);
			//System.out.println(externalDocId);
			fv.setExternalDocID(externalDocId);
			fv.setRelevanceDegree(docDegreeMap.get(externalDocId));
			int internalDocId = 0;
			if(this.docIdMap.containsKey(externalDocId)){
				internalDocId = this.docIdMap.get(externalDocId);
				fv.setF4(this.pageRankMap.get(externalDocId));
			}
			else{
				
				try {
					//when pagerank file does not contain this eid
					internalDocId = QryEval.getInternalDocid(externalDocId);
					this.docIdMap.put(externalDocId, internalDocId);
					fv.setF4(0);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					//when the index does hv this eid
					//e.printStackTrace();
					//set the fv for this term as not hving any other features
					fv.setAllDefault();
					if(this.pageRankMap.containsKey(externalDocId))
						fv.setF4(this.pageRankMap.get(externalDocId));
					fvList.add(fv);
		    		curMinFeatures = fv.getCurMinFeatures(curMinFeatures);
		    		curMaxFeatures = fv.getCurMaxFeatures(curMaxFeatures);
		    		continue;
				}
			}
			fv.setInternalDocID(internalDocId);	
		    //fetch the term vector for d
		    //calculate other features for <q, d>
			//To get a document's spam score and raw (unparsed) URL from the index:
			try {
				Document d = QryEval.READER.document(internalDocId);
				int spamscore = Integer.parseInt(d.get("score"));
    			fv.setF1(spamscore);
    			String rawUrl = d.get("rawUrl");
    			/*if(externalDocId.equals("clueweb09-en0000-01-21462")||
    					externalDocId.equals("clueweb09-en0000-02-00469")||
    					externalDocId.equals("clueweb09-en0000-02-25067")||
    					externalDocId.equals("clueweb09-en0000-02-25080")){
    				System.out.println(externalDocId);
    				System.out.println(rawUrl);
    				System.out.println(rawUrl.split("/").length);
    			}*/
    			int urlDepth = 0;
    			for(int i = 0; i<rawUrl.length(); i++){
    				if(rawUrl.charAt(i)=='/')
    					urlDepth++;
    			}
    			fv.setF2(urlDepth);
    			if(rawUrl.contains("wikipedia.org"))
    				fv.setF3(1);
    			else
    				fv.setF3(0);
			}catch(Exception e){
				//when QryEval.READER does not have this iid
				//set corresponding features as invalid
				e.printStackTrace();
				fv.setF1(-1);
				fv.setF2(-1);
				fv.setF3(-1);
			}	    				    			
			try {
				TermVector tv = new TermVector(internalDocId, "body");
				fv.setF5(featureBM25(queryStemsList, queryStemsSet, tv, internalDocId, "body"));
				double d = featureIndri(queryStemsSet, tv, internalDocId, "body", qrySize);
    			//System.out.println(externalDocId + " F6: " + d);
				fv.setF6(d);
    			fv.setF7(featureTermOverlap(queryStemsSet, tv, internalDocId, "body", qrySize));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//the document doesn't have a body field
				//set corresponding features as invalid
				//e.printStackTrace();
				fv.setF5(-1);
				fv.setF6(-1);
				fv.setF7(-1);
			}
			try {
				TermVector tv = new TermVector(internalDocId, "title");
    			fv.setF8(featureBM25(queryStemsList, queryStemsSet, tv, internalDocId, "title"));
    			fv.setF9(featureIndri(queryStemsSet, tv, internalDocId, "title", qrySize));
    			fv.setF10(featureTermOverlap(queryStemsSet, tv, internalDocId, "title", qrySize));
			}catch (Exception e) {
				// TODO Auto-generated catch block
				//the document doesn't have a title field
				//e.printStackTrace();
				fv.setF8(-1);
				fv.setF9(-1);
				fv.setF10(-1);
			}
			try {
				
				TermVector tv = new TermVector(internalDocId, "url");
    			fv.setF11(featureBM25(queryStemsList, queryStemsSet, tv, internalDocId, "url"));
    			fv.setF12(featureIndri(queryStemsSet, tv, internalDocId, "url", qrySize));
    			fv.setF13(featureTermOverlap(queryStemsSet, tv, internalDocId, "url", qrySize));
			} catch (Exception e) {
				//the document doesn't have an url field
				//e.printStackTrace();
				fv.setF11(-1);
				fv.setF12(-1);
				fv.setF13(-1);
			}
			try {
				TermVector tv = new TermVector(internalDocId, "inlink");
    			fv.setF14(featureBM25(queryStemsList, queryStemsSet, tv, internalDocId, "inlink"));
    			fv.setF15(featureIndri(queryStemsSet, tv, internalDocId, "inlink", qrySize));
    			fv.setF16(featureTermOverlap(queryStemsSet, tv, internalDocId, "inlink", qrySize));	 
			}catch (Exception e) {
				//the document doesn't have an inlink field
				//e.printStackTrace();
				fv.setF14(-1);
				fv.setF15(-1);
				fv.setF16(-1);
			}
			try {
				TermVector tv = new TermVector(internalDocId, "keywords");
    			fv.setF17(featureBM25(queryStemsList, queryStemsSet, tv, internalDocId, "inlink"));
    			fv.setF18(featureIndri(queryStemsSet, tv, internalDocId, "inlink", qrySize));	 
			}catch (Exception e) {
				//the document doesn't have an inlink field
				//e.printStackTrace();
				fv.setF17(-1);
				fv.setF18(-1);
			}
			fvList.add(fv);
    		curMinFeatures = fv.getCurMinFeatures(curMinFeatures);
    		curMaxFeatures = fv.getCurMaxFeatures(curMaxFeatures);  
    		//System.out.println("curMinFeatures F6: " + curMinFeatures.getF6());
    		//System.out.println("curMaxFeatures F6: " + curMaxFeatures.getF6());
		}
		for(FeatureVector fv : fvList){
			FeatureVector norm = fv.normalized(curMinFeatures, curMaxFeatures);
			try {
				/*if(!norm.toString().contains("#")){
					System.err.println("error in composing norm");
					System.err.println(fv.getExternalDocID());
					System.err.println(fv.getF6());
				}*/
				featureVectorsWriter.write(norm.toPrint(this.featureDisableList)+"\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//System.exit(0);
		
	}
	
	public void runSVMRank(boolean isLearn) throws Exception{
		Process cmdProc;
		if(isLearn){
			// runs svm_rank_learn from within Java to train the model
			// execPath is the location of the svm_rank_learn utility, 
			// which is specified by letor:svmRankLearnPath in the parameter file.
			// FEAT_GEN.c is the value of the letor:c parameter.
			cmdProc = Runtime.getRuntime().exec(
				new String[] {this.svmRankLearnPath, "-c", String.valueOf(this.FEAT_GEN),
						this.trainingFeatureVectorsFilePath, this.modelOutputFile });
		}
		else{
			// runs svm_rank_classify from within Java
	      	// execPath is the location of the svm_rank_classify utility, 
			cmdProc = Runtime.getRuntime().exec(
					new String[] { this.svmRankClassifyPath, this.testingFeatureVectorsFilePath, this.modelOutputFile, this.testingDocumentScoresPath});
		}

		// The stdout/stderr consuming code MUST be included.
        // It prevents the OS from running out of output buffer space and stalling.

		// consume stdout and print it out for debugging purposes
        BufferedReader stdoutReader = new BufferedReader(
            new InputStreamReader(cmdProc.getInputStream()));
        String line;
        while ((line = stdoutReader.readLine()) != null) {
          System.out.println(line);
        }
   
		
        // consume stderr and print it for debugging purposes
        BufferedReader stderrReader = new BufferedReader(
            new InputStreamReader(cmdProc.getErrorStream()));
        while ((line = stderrReader.readLine()) != null) {
          System.out.println(line);
        }
     
     
        // get the return value from the executable. 0 means success, non-zero 
        // indicates a problem
        int retValue = cmdProc.waitFor();
        if (retValue != 0) {
          throw new Exception("SVM Rank crashed.");
        }
			       
	}
    private double featureBM25(List<String> queryStemsList, HashSet<String> queryStemsSet, TermVector tv, int docid, String field){
    	double score = 0.0;
		try {
			//tv = new TermVector(docid, field);
			int distinctStemsCount = queryStemsSet.size();
			for (int i=0, j=0; i< tv.stemsLength()&&j<distinctStemsCount; i++){
				String stem = tv.stemString(i);
	    		if(queryStemsSet.contains(stem)){
	    			j++;
	    			//calc BM25 score for this stem
	    			int df = tv.stemDf(i);
	    			int tf = tv.stemFreq(i);
	    			double part1=Math.max(0, Math.log((QryEval.N-df+0.5)/(df+0.5)));
	    			int docLen = (int) QryEval.s.getDocLength(field, docid);
	    			double avgDocLen = QryEval.totalTermFreq.get(field) /
	    				      (double) QryEval.totalDocCount.get(field);
	    			double part2 = tf/(this.k_1*((docLen/avgDocLen)*this.b+(1-this.b))+tf);
	    			double qtf = Collections.frequency(queryStemsList, stem);
	    			double part3 = ((this.k_3+1)*qtf)/(this.k_3+qtf);
	    			score+=(part1*part2*part3);
	    		}
	    	}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			return score;
		}
    	
    }
	    
    private double featureIndri(HashSet<String> queryStemsSet, TermVector tv, int docid, String field, int qrySize){
    	List<Double> matchScores = new ArrayList<Double>();
    	//TermVector tv;
		try {
			//tv = new TermVector(docid, field);
			int distinctStemsCount = queryStemsSet.size();
			for (int i=0, j=0; i< tv.stemsLength()&&j<distinctStemsCount; i++){
				String stem = tv.stemString(i);
	    		if(queryStemsSet.contains(stem)){
	    			j++;
	    			int tf = tv.stemFreq(i);
	    			long collectionLen = QryEval.totalTermFreq.get(field);
	    			long docLen = QryEval.s.getDocLength(field, docid);
	    			long ctf = tv.totalStemFreq(i);
	    			double qC = ctf/(double)collectionLen; 
	    			double toMultiply =Math.pow((((1-this.lambda)*(((double)tf+this.mu*qC)/((double)docLen+this.mu)))+
	    					(this.lambda*qC)),1/(double)qrySize);
	    			//score*=toMultiply;
	    			matchScores.add(toMultiply);
	    		}
	    	}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			if(matchScores.size()==0)
				return 0.0;
			else{
				double totalScore = 1.0;
				for(int i=0; i<matchScores.size(); i++){
					totalScore*=matchScores.get(i);
				}
				return totalScore;
			}
		}
    }
	    
	    
    private double featureTermOverlap(HashSet<String> queryStemsSet, TermVector tv, int docid, String field, int qrySize){
    	int count = 0;
    	//TermVector tv;
		try {
			//tv = new TermVector(docid, field);
			int distinctStemsCount = queryStemsSet.size();
			for (int i=0; i< tv.stemsLength()&&count<distinctStemsCount; i++){
				String stem = tv.stemString(i);
				if(queryStemsSet.contains(stem)){
					count++;
	    		}
	    	}
			return ((double)count/(double)qrySize);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0.0;
		}
    }
	    
    
	/**
	   * Set a retrieval model parameter.
	   * @param parameterName
	   * @param parametervalue
	   * @return Always false because this retrieval model has no parameters.
	   */
	  public boolean setParameter (String parameterName, double value) {
	    System.err.println ("Error: Unknown parameter name for retrieval model " +
				"LeToR: " +
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
				"LeToR: " +
				parameterName);
	    return false;
	  }

	
}
