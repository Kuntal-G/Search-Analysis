package org.apache.lucene.test;

import org.apache.lucene.util.AttributeImpl;

public class GenderAttributeImpl extends AttributeImpl implements GenderAttribute {
	
	private Gender gender = Gender.Undefined;
	
	public void setGender(Gender gender) {
		this.gender = gender;
	}
	
	public Gender getGender() {
		return gender;
	};
	
	@Override
	public void clear() {
		gender = Gender.Undefined;
	}
	
	@Override
	public void copyTo(AttributeImpl target) {
		((GenderAttribute) target).setGender(gender);
	}
}
