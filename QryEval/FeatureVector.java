
public class FeatureVector {
	private int queryID = -1;
	private String externalDocID = null;
	private int internalDocID = -1;
	private int relevanceDegree = -1;
	private double F1 = -1;
	private double F2 = -1;
	private double F3 = -1;
	private double F4 = 0;
	private double F5 = -1;
	private double F6 = -1;
	private double F7 = -1;
	private double F8 = -1;
	private double F9 = -1;
	private double F10 = -1;
	private double F11 = -1;
	private double F12 = -1;
	private double F13 = -1;
	private double F14 = -1;
	private double F15 = -1;
	private double F16 = -1;
	private double F17 = -1;
	private double F18 = -1;
	
	public FeatureVector(int q) {
		// TODO Auto-generated constructor stub
		this.queryID = q;
	}
	public int getQueryID(){
		return this.queryID;
	}
	public double getF1(){
		return this.F1;
	}
	public double getF2(){
		return this.F2;
	}
	public double getF3(){
		return this.F3;
	}
	public double getF4(){
		return this.F4;
	}
	public double getF5(){
		return this.F5;
	}
	public double getF6(){
		return this.F6;
	}
	public double getF7(){
		return this.F7;
	}
	public double getF8(){
		return this.F8;
	}
	public double getF9(){
		return this.F9;
	}
	public double getF10(){
		return this.F10;
	}
	public double getF11(){
		return this.F11;
	}
	public double getF12(){
		return this.F12;
	}
	public double getF13(){
		return this.F13;
	}
	public double getF14(){
		return this.F14;
	}
	public double getF15(){
		return this.F15;
	}
	public double getF16(){
		return this.F16;
	}
	public double getF17(){
		return this.F17;
	}
	public double getF18(){
		return this.F18;
	}
	
	
	public void setQueryID(int i){
		this.queryID = i;
	}
	public void setExternalDocID(String e){
		this.externalDocID = e;
	}
	public void setInternalDocID(int i){
		this.internalDocID = i;
	}
	public void setRelevanceDegree(int r){
		this.relevanceDegree = r;
	}
	public void setF1(double f){
		this.F1 = f;
	}
	public void setF2(double f){
		this.F2 = f;
	}
	public void setF3(double f){
		this.F3 = f;
	}
	public void setF4(double f){
		this.F4 = f;
	}
	public void setF5(double f){
		this.F5 = f;
	}
	public void setF6(double f){
		this.F6 = f;
	}
	public void setF7(double f){
		this.F7 = f;
	}
	public void setF8(double f){
		this.F8 = f;
	}
	public void setF9(double f){
		this.F9 = f;
	}
	public void setF10(double f){
		this.F10 = f;
	}
	public void setF11(double f){
		this.F11 = f;
	}
	public void setF12(double f){
		this.F12 = f;
	}
	public void setF13(double f){
		this.F13 = f;
	}
	public void setF14(double f){
		this.F14 = f;
	}
	public void setF15(double f){
		this.F15 = f;
	}
	public void setF16(double f){
		this.F16 = f;
	}
	public void setF17(double f){
		this.F17 = f;
	}
	public void setF18(double f){
		this.F18 = f;
	}
	public FeatureVector getCurMinFeatures(FeatureVector anotherFV){
		//System.out.println("get min called");
		if(anotherFV==null){
			//System.out.println("another FV is null");
			return this;
			
		}
		if(anotherFV.getQueryID()!=this.queryID){
			System.err.println("MINMAX COMP Query ID mismath...ERROR");
			System.exit(1);
		}
		FeatureVector minFV = new FeatureVector(this.queryID);
		//minFV.setQueryID(this.queryID);
		minFV.setF1(getMinHelper(anotherFV.F1, this.F1));
		minFV.setF2(getMinHelper(anotherFV.F2, this.F2));
		minFV.setF3(getMinHelper(anotherFV.F3, this.F3));
		minFV.setF4(Math.min(anotherFV.F4, this.F4));
		minFV.setF5(getMinHelper(anotherFV.F5, this.F5));
		minFV.setF6(getMinHelper(anotherFV.F6, this.F6));
		minFV.setF7(getMinHelper(anotherFV.F7, this.F7));
		minFV.setF8(getMinHelper(anotherFV.F8, this.F8));
		minFV.setF9(getMinHelper(anotherFV.F9, this.F9));
		minFV.setF10(getMinHelper(anotherFV.F10, this.F10));
		minFV.setF11(getMinHelper(anotherFV.F11, this.F11));
		minFV.setF12(getMinHelper(anotherFV.F12, this.F12));
		minFV.setF13(getMinHelper(anotherFV.F13, this.F13));
		minFV.setF14(getMinHelper(anotherFV.F14, this.F14));
		minFV.setF15(getMinHelper(anotherFV.F15, this.F15));
		minFV.setF16(getMinHelper(anotherFV.F16, this.F16));
		minFV.setF17(getMinHelper(anotherFV.F17, this.F17));
		minFV.setF18(getMinHelper(anotherFV.F18, this.F18));
		return minFV;
		
	}
	private double getMinHelper(double anotherValue, double curValue){
		if(anotherValue<0){
			if(curValue<0)
				return -1;
			else
				return curValue;
		}
		else{
			if(curValue<0)
				return anotherValue;
			else
				return Math.min(anotherValue, curValue);
		}
		
	}
	public FeatureVector getCurMaxFeatures(FeatureVector anotherFV){
		//System.out.println("get max called");
		if(anotherFV==null){
			//System.out.println("another FV is null");
		
			return this;
		}
		if(anotherFV.getQueryID()!=this.queryID){
			System.err.println("MINMAX COMP Query ID mismath...ERROR");
			System.exit(1);
		}
		FeatureVector maxFV = new FeatureVector(this.queryID);
		//maxFV.setQueryID(this.queryID);
		maxFV.setF1(getMaxHelper(anotherFV.F1, this.F1));
		maxFV.setF2(getMaxHelper(anotherFV.F2, this.F2));
		maxFV.setF3(getMaxHelper(anotherFV.F3, this.F3));
		maxFV.setF4(Math.max(anotherFV.F4, this.F4));
		maxFV.setF5(getMaxHelper(anotherFV.F5, this.F5));
		maxFV.setF6(getMaxHelper(anotherFV.F6, this.F6));
		maxFV.setF7(getMaxHelper(anotherFV.F7, this.F7));
		maxFV.setF8(getMaxHelper(anotherFV.F8, this.F8));
		maxFV.setF9(getMaxHelper(anotherFV.F9, this.F9));
		maxFV.setF10(getMaxHelper(anotherFV.F10, this.F10));
		maxFV.setF11(getMaxHelper(anotherFV.F11, this.F11));
		maxFV.setF12(getMaxHelper(anotherFV.F12, this.F12));
		maxFV.setF13(getMaxHelper(anotherFV.F13, this.F13));
		maxFV.setF14(getMaxHelper(anotherFV.F14, this.F14));
		maxFV.setF15(getMaxHelper(anotherFV.F15, this.F15));
		maxFV.setF16(getMaxHelper(anotherFV.F16, this.F16));
		maxFV.setF17(getMaxHelper(anotherFV.F17, this.F17));
		maxFV.setF18(getMaxHelper(anotherFV.F18, this.F18));
		return maxFV;
		
	}
	private double getMaxHelper(double anotherValue, double curValue){
		if(Math.max(anotherValue, curValue)<0)
			return -1;
		else
			return Math.max(anotherValue, curValue);
		
	}
	
	public FeatureVector normalized(FeatureVector minFV, FeatureVector maxFV){
		
		if((minFV.getQueryID()!=maxFV.getQueryID()) || (minFV.getQueryID()!=this.queryID)){
			System.err.println("Query ID mismatch...ERROR");
			System.exit(1);
		}
		FeatureVector norm =  new FeatureVector(this.queryID);
		norm.setF1(normalizationHelper(this.F1, minFV.getF1(), maxFV.getF1()));
		norm.setF2(normalizationHelper(this.F2, minFV.getF2(), maxFV.getF2()));
		norm.setF3(this.F3);
		//only F4 can have valid negative feature value
		if(maxFV.getF4()>minFV.getF4())
			norm.setF4((this.F4-minFV.getF4())/(maxFV.getF4()-minFV.getF4()));
		else if(maxFV.getF4()==minFV.getF4())
			norm.setF4(0);
		else{
			System.err.println("Max Min missetting...ERROR");
			System.exit(1);
		}
		norm.setF5(normalizationHelper(this.F5, minFV.getF5(), maxFV.getF5()));
		norm.setF6(normalizationHelper(this.F6, minFV.getF6(), maxFV.getF6()));
		norm.setF7(normalizationHelper(this.F7, minFV.getF7(), maxFV.getF7()));
		norm.setF8(normalizationHelper(this.F8, minFV.getF8(), maxFV.getF8()));
		norm.setF9(normalizationHelper(this.F9, minFV.getF9(), maxFV.getF9()));
		norm.setF10(normalizationHelper(this.F10, minFV.getF10(), maxFV.getF10()));
		norm.setF11(normalizationHelper(this.F11, minFV.getF11(), maxFV.getF11()));
		norm.setF12(normalizationHelper(this.F12, minFV.getF12(), maxFV.getF12()));
		norm.setF13(normalizationHelper(this.F13, minFV.getF13(), maxFV.getF13()));
		norm.setF14(normalizationHelper(this.F14, minFV.getF14(), maxFV.getF14()));
		norm.setF15(normalizationHelper(this.F15, minFV.getF15(), maxFV.getF15()));
		norm.setF16(normalizationHelper(this.F16, minFV.getF16(), maxFV.getF16()));
		norm.setF17(normalizationHelper(this.F17, minFV.getF17(), maxFV.getF17()));
		norm.setF18(normalizationHelper(this.F18, minFV.getF18(), maxFV.getF18()));
		norm.setQueryID(this.queryID);
		norm.setExternalDocID(this.externalDocID);
		norm.setInternalDocID(this.internalDocID);
		norm.setRelevanceDegree(this.relevanceDegree);
		return norm;
	}
	
	private double normalizationHelper(double curFeature, double minFeature, double maxFeature){
		if(maxFeature<0 && minFeature<0){
			//features of all matching docs are invalid
			if(curFeature<0)
				return -1;
			else{
				System.err.println("this can not occur...ERROR");
				return 0.0;
			}
			
		}
		else if(minFeature<0){
			System.err.println("this also can not occur...ERROR");
			return -1;
		}
		else{
			//feature value for cur doc is invalid...discard
			if(curFeature<0)
				return -1;		
			if(maxFeature>minFeature)
				return ((curFeature-minFeature)/(maxFeature-minFeature));
			else if (maxFeature==minFeature){
				if(maxFeature!=curFeature){
					System.err.println("Min Max missetting...ERROR");
					return -1;
				}
				else{
					return 0.0;
				}
			}
			else{
				System.err.println("Min>Max...ERROR");
				return -1;
			}
		}
	}
	
	public String toPrint(int[] mask){
		String s = null;
		s = this.relevanceDegree + " qid:" + this.queryID + " ";
		//only output features not masked and is valid
		if(mask[0]!=1 && this.F1>=0)
			s += "1:"+this.F1+" ";
		if(mask[1]!=1 && this.F2>=0)
			s += "2:"+this.F2+" ";
		if(mask[2]!=1 && this.F3>=0)
			s += "3:"+this.F3+" ";
		if(mask[3]!=1 && this.F4>=0)
			s += "4:"+this.F4+" ";
		if(mask[4]!=1 && this.F5>=0)
			s += "5:"+this.F5+" ";
		if(mask[5]!=1 && this.F6>=0)
			s += "6:"+this.F6+" ";
		if(mask[6]!=1 && this.F7>=0)
			s += "7:"+this.F7+" ";
		if(mask[7]!=1 && this.F8>=0)
			s += "8:"+this.F8+" ";
		if(mask[8]!=1 && this.F9>=0)
			s += "9:"+this.F9+" ";
		if(mask[9]!=1 && this.F10>=0)
			s += "10:"+this.F10+" ";
		if(mask[10]!=1 && this.F11>=0)
			s += "11:"+this.F11+" ";
		if(mask[11]!=1 && this.F12>=0)
			s += "12:"+this.F12+" ";
		if(mask[12]!=1 && this.F13>=0)
			s += "13:"+this.F13+" ";
		if(mask[13]!=1 && this.F14>=0)
			s += "14:"+this.F14+" ";
		if(mask[14]!=1 && this.F15>=0)
			s += "15:"+this.F15+" ";
		if(mask[15]!=1 && this.F16>=0)
			s += "16:"+this.F16+" ";
		if(mask[16]!=1 && this.F17>=0)
			s += "17:"+this.F17+" ";
		if(mask[17]!=1 && this.F18>=0)
			s += "18:"+this.F18+" ";
		s += "# " + this.externalDocID;
		return s;
	}
	public String getExternalDocID(){
		return this.externalDocID;
	}
	public void setAllDefault(){
		this.internalDocID = -1;
		this.F1 = -1;
		this.F2 = -1;
		this.F3 = -1;
		this.F4 = 0;
		this.F5 = -1;
		this.F6 = -1;
		this.F7 = -1;
		this.F8 = -1;
		this.F9 = -1;
		this.F10 = -1;
		this.F11 = -1;
		this.F12 = -1;
		this.F13 = -1;
		this.F14 = -1;
		this.F15 = -1;
		this.F16 = -1;
		this.F17 = -1;
		this.F18 = -1;
	}
}
