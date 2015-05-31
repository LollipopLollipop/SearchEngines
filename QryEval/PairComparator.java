import java.util.Comparator;
import java.util.Map;


public class PairComparator implements Comparator<String>{
	Map<String, Double> base;
	
    public PairComparator(Map<String, Double> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.    
    public int compare(String a, String b) {
        if (base.get(a) > base.get(b)) {
            return -1;
        } else if(base.get(a) < base.get(b)){
            return 1;
        } else if(a.compareTo(b)<0) {
        	return -1;
        } else if(a.compareTo(b)>0){
        	return 1;
        } else {
        	return 0;
        }
    }
    
	public PairComparator() {
		// TODO Auto-generated constructor stub
	}

}
