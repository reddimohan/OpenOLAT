/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.group.model;

import org.olat.group.BusinessGroupShort;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class BusinessGroupReference {

	private Long key;
	private String name;
	
	private Long originalKey;
	private String originalName;
	
	public BusinessGroupReference() {
		//
	}
	
	public BusinessGroupReference(BusinessGroupShort group) {
		this.key = group.getKey();
		this.name = group.getName();
		this.originalKey = group.getKey();
		this.originalName = group.getName();
	}
	
	public BusinessGroupReference(BusinessGroupShort group, Long originalKey, String originalName) {
		this.key = group.getKey();
		this.name = group.getName();
		this.originalKey = originalKey;
		this.originalName = originalName;
	}
	
	public Long getKey() {
		return key;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public Long getOriginalKey() {
		return originalKey;
	}

	public void setOriginalKey(Long originalKey) {
		this.originalKey = originalKey;
	}

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}
}
