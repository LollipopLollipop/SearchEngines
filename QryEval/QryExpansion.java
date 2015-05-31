import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.regex.Pattern;


public class QryExpansion {
	private ScoreList initialDocRanking = null;
	private String field = null;
	private int fbMu = 0;
	private int fbTerms = 0;
	public QryExpansion(){
	}
	public QryExpansion(ScoreList initialScoreList){
		this.initialDocRanking = initialScoreList;
		this.field = "body";
	}
	public void setInitialScoreList(ScoreList initialScoreList){
		this.initialDocRanking = initialScoreList;
	}	
	public void setMu(int mu){
		this.fbMu = mu;
	}
	public void setTerms(int num){
		this.fbTerms = num;
	}
	public LinkedHashMap<String, Double> evaluate(RetrievalModel r) throws Exception{
		System.out.println("field of the query is " + this.field);
		return expandHelper(r);
	}
	private LinkedHashMap<String, Double> expandHelper(RetrievalModel r) throws Exception{
		//System.out.println(this.initialDocRanking.scores.size() + " documents processed.");
		//data structure to hold potential expansion terms with assoc docs
		HashMap<String, TermStats> termDocMap = new HashMap<String, TermStats>();
		HashMap<String, Double> termScoreMap = new HashMap<String, Double>();
		PairComparator pCmp = new PairComparator(termScoreMap);
		TreeMap<String, Double> sortedTermScoreMap = new TreeMap<String, Double>(pCmp);
		HashMap<Integer, Double> docsIndriScores = new HashMap<Integer, Double>();
		//mapping potential expanded terms with docids
		for(int i=0; i< this.initialDocRanking.scores.size(); i++){
			int internalDocid = this.initialDocRanking.getDocid(i);
			//double pId = ((QryopSl)initialQryOperator).getDefaultScore(r, internalDocid);
			double pId = this.initialDocRanking.getDocidScore(i);
			docsIndriScores.put(internalDocid,pId);
			//System.out.println(internalDocid);
			//System.out.println(field);
			TermVector tv = new TermVector(internalDocid, this.field);
			int termVectorSize = tv.stems.length;
			for(int j=1; j<termVectorSize; j++){
				String termString = tv.stemString(j);
				//System.out.println(termString);
				if(termString.indexOf(".")>=0||termString.indexOf(",")>=0)
					continue;//throw all terms with "."
				else{
					//collect stats for terms over retrieved top docs
					if(termDocMap.containsKey(termString)){
						termDocMap.get(termString).addDocTfPair(internalDocid, tv.stemFreq(j));
					}else{
						TermStats stats = new TermStats();
						stats.setCtf(tv.totalStemFreq(j));
						stats.addDocTfPair(internalDocid, tv.stemFreq(j));
						termDocMap.put(termString, stats);
					}					
				}
			}
		}
		//System.out.println("size of termDocMap: " + termDocMap.size());
		//System.out.println("size of docsIndriScores: " + docsIndriScores.size());
		//System.out.println(System.currentTimeMillis());
		for(String term : termDocMap.keySet()){
			TermStats stats= termDocMap.get(term);
			double ptc = (double)stats.getCtf()/(double)QryEval.totalTermFreq.get(this.field);
			
			double score = 0.0;
			HashMap<Integer, Integer> docTfMap = stats.getDocTfMap();
			for(int docid : docsIndriScores.keySet()){
				if(!docTfMap.containsKey(docid))
					docTfMap.put(docid, 0);
			}
			if(docTfMap.size()!=docsIndriScores.size()){
				System.out.println("doc map size mismatch...ERROR");
				return null;
			}
			for(int tmpDocid : docTfMap.keySet()){
				//the score Indri assigned to document d for initial query 
				//System.out.println("initial query is " + initialQryOperator.toString());
				double pId = docsIndriScores.get(tmpDocid);
				
				double ptd = ((double)docTfMap.get(tmpDocid) + 
						(double)(this.fbMu)*ptc)/((double)QryEval.s.getDocLength(field, tmpDocid)+(double)this.fbMu);
				
				//System.out.println("indri score for this doc " + pId);
				score+=(pId*ptd*Math.log(1/(ptc)));
			}
			
			termScoreMap.put(term, score);
		}
		//System.out.println(System.currentTimeMillis());
		sortedTermScoreMap.putAll(termScoreMap);
		
		//System.out.println("size of sortedScoreTermMap: " + sortedTermScoreMap.size());
		LinkedHashMap<String, Double> topTermScoreMap = new LinkedHashMap<String, Double>();
		int i=0;
		for(String term : sortedTermScoreMap.keySet()){
			if(i>=this.fbTerms)
				break;
			topTermScoreMap.put(term, sortedTermScoreMap.get(term));
			i++;
		}
		System.out.println("total " + topTermScoreMap.size() + " terms selected");
		return topTermScoreMap;
	}
	
}

