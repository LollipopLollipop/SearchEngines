import java.util.HashMap;


public class TermStats {
	private long ctf = 0;
	private HashMap<Integer, Integer> docTfMap = new HashMap<Integer, Integer>();
	public TermStats() {
		ctf = 0;
		docTfMap = new HashMap<Integer, Integer>();
	}
	public void addDocTfPair(int docid, int tf){
		this.docTfMap.put(docid, tf);
	}

	public void setCtf(long ctf){
		this.ctf = ctf;
	}
	public long getCtf(){
		return this.ctf;
	}
	
	public int getDocListSize(){
		return docTfMap.size();
	}
	
	public HashMap<Integer, Integer> getDocTfMap(){
		return docTfMap;
	}
}
