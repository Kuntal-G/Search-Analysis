package org.apache.lucene.test;

import org.apache.lucene.util.Attribute;

public interface GenderAttribute extends Attribute {
	
	public static enum Gender {Male, Female, Undefined};
	
	public void setGender(Gender gender);
	
	public Gender getGender();
}
