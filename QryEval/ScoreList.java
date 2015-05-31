/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 *
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.*;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;

public class ScoreList {

  //  A little utilty class to create a <docid, score> object.

  protected class ScoreListEntry {
    private int docid;
    private double score;

    private ScoreListEntry(int docid, double score) {
      this.docid = docid;
      this.score = score;
    }
  }
  
  

  List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

  /**
   *  Append a document score to a score list.
   *  @param docid An internal document id.
   *  @param score The document's score.
   *  @return void
   */
  public void add(int docid, double score) {
    scores.add(new ScoreListEntry(docid, score));
  }
  
  
  /**
   *  sort the current score list
   *  @param map internal id to external id
   *  @return void
   */
  public void sort(final HashMap<Integer, String> map) {
    Collections.sort(this.scores, new Comparator<ScoreListEntry>() {
		@Override
		public int compare(ScoreListEntry o1, ScoreListEntry o2) {
			//System.out.println("object");
			// TODO Auto-generated method stub
			if(((ScoreListEntry)o1).score>((ScoreListEntry)o2).score)
    			return -1;
    		else if(((ScoreListEntry)o1).score<((ScoreListEntry)o2).score)
    			return 1;
    		else 
    			return map.get(((ScoreListEntry)o1).docid).compareTo(map.get(((ScoreListEntry)o2).docid));
		}}); 
  }
  
  public void sort() {
	    Collections.sort(this.scores, new Comparator<ScoreListEntry>() {
			@Override
			public int compare(ScoreListEntry o1, ScoreListEntry o2) {
				//System.out.println("object");
				// TODO Auto-generated method stub
				if(((ScoreListEntry)o1).score>((ScoreListEntry)o2).score)
	    			return -1;
	    		else if(((ScoreListEntry)o1).score<((ScoreListEntry)o2).score)
	    			return 1;
				else{
					String externalId1 = null;
					try {
						externalId1 = QryEval.getExternalDocid(((ScoreListEntry)o1).docid);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String externalId2 = null;
					try {
						externalId2 = QryEval.getExternalDocid(((ScoreListEntry)o2).docid);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return externalId1.compareTo(externalId2);
				}
			}}); 
	  }

  /**
   *  Get the n'th document id.
   *  @param n The index of the requested document.
   *  @return The internal document id.
   */
  public int getDocid(int n) {
    return this.scores.get(n).docid;
  }

  /**
   *  Get the score of the n'th document.
   *  @param n The index of the requested document score.
   *  @return The document's score.
   */
  public double getDocidScore(int n) {
    return this.scores.get(n).score;
  }

}
