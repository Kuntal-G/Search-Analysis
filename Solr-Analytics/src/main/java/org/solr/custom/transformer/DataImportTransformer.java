package org.solr.custom.transformer;

import java.util.Map;

/**
 * Custom Transformer for using during the DataimportHandler.
 * This simple transformer will simply take the address,city,state,zip-code,country-name with space as separator ,
 * and generate a new field location during indexing.
 * 
 * @author kuntal
 *
 */

public class DataImportTransformer {
	
	
	
	public Object transformRow(Map<String, Object> row) 
	{
	
		

		    /*
			 * To inject the location field.
			 */
		    StringBuffer locationSB = new StringBuffer();
	        
	        if(row.get("streetAddress")!= null){
				locationSB.append(row.get("streetAddress").toString());
				locationSB.append( " "); 
			}
	        if(row.get("address2")!= null){
				locationSB.append(row.get("address2").toString());
				locationSB.append( " "); 
			}	       
	        
	        if(row.get("city")!= null){
				locationSB.append(row.get("city").toString());
				locationSB.append( " "); 
			}
	        if(row.get("zip")!= null){
				locationSB.append(row.get("zip").toString());
				locationSB.append( " "); 
			}
	        if(row.get("countryName")!= null){
				locationSB.append(row.get("countryName").toString());
				locationSB.append( " "); 
			}
	        row.put("location", locationSB.toString().trim());
	       
		return row;
	}
}