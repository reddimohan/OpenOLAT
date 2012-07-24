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

package org.olat.group.ui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.util.StringHelper;
import org.olat.group.BusinessGroup;

/**
 * Implements a Business group creation dialog using FlexiForms.
 * 
 * @author twuersch
 * 
 */
public class BusinessGroupFormController extends FormBasicController {

	/**
	 * Text entry field for the name of this business group.
	 */
	private TextElement businessGroupName;

	/**
	 * Text entry field for the minimum number of members for this business group.
	 */
	private TextElement businessGroupMinimumMembers;

	/**
	 * Text entry field for the maximum number of members for this business group.
	 */
	private TextElement businessGroupMaximumMembers;

	/**
	 * Text entry field for the description for this business group.
	 */
	private RichTextElement businessGroupDescription;

	/**
	 * Decides whether minimum and maximum number of group members can be applied.
	 */
	private MultipleSelectionElement enableWaitingList;
	private MultipleSelectionElement enableAutoCloseRanks;

	/**
	 * The {@link BusinessGroup} object this form refers to.
	 */
	private BusinessGroup businessGroup;

	private boolean bulkMode = false;
	private boolean embbeded = false;

	private Set<String> validNames;

	/** The key for the waiting list checkbox. */
	private final String[] waitingListKeys = new String[] { "create.form.enableWaitinglist" };

	/** The value for the waiting list checkbox. */
	private final String[] waitingListValues = new String[] { translate("create.form.enableWaitinglist") };

	/** The key for the autoCloseRanks checkbox. */
	private final String[] autoCloseKeys = new String[] { "create.form.enableAutoCloseRanks" };

	/** The value for the autoCloseRanks checkbox. */
	private final String[] autoCloseValues = new String[] { translate("create.form.enableAutoCloseRanks") };
	
	/**
	 * Creates this controller.
	 * 
	 * @param ureq The user request.
	 * @param wControl The window control.
	 * @param businessGroup The group object which will be modified by this dialog.
	 * @param minMaxEnabled Decides whether to limit the number of people that can enrol to a group or not
	 */
	public BusinessGroupFormController(UserRequest ureq, WindowControl wControl, BusinessGroup businessGroup) {
		super(ureq, wControl, FormBasicController.LAYOUT_DEFAULT);
		this.businessGroup = businessGroup;
		initForm(ureq);
	}
	
	/**
	 * 
	 * @param ureq
	 * @param wControl
	 * @param businessGroup The group object which will be modified by this dialog.
	 * @param minMaxEnabled Decides whether to limit the number of people that can enrol to a group or not
	 * @param bulkMode when passing group names as CSV you have to set this to true and all groups will be created at once
	 */
	public BusinessGroupFormController(UserRequest ureq, WindowControl wControl, BusinessGroup businessGroup, boolean bulkMode) {
		super(ureq, wControl, FormBasicController.LAYOUT_DEFAULT);
		this.businessGroup = businessGroup;
		this.bulkMode = bulkMode;
		initForm(ureq); // depends on bulkMode flag
	}
	
	public BusinessGroupFormController(UserRequest ureq, WindowControl wControl, BusinessGroup businessGroup, Form rootForm) {
		super(ureq, wControl, FormBasicController.LAYOUT_DEFAULT, null, rootForm);
		this.businessGroup = businessGroup;
		bulkMode = false;
		embbeded = true;
		initForm(ureq); // depends on bulkMode flag
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		// Create the business group name input text element
		if (bulkMode) {
			businessGroupName = uifactory.addTextElement("create.form.title.bgnames", "create.form.title.bgnames", 10 * BusinessGroup.MAX_GROUP_NAME_LENGTH, "", formLayout);
			businessGroupName.setExampleKey("create.form.message.example.group", null);
		} else {
			businessGroupName = uifactory.addTextElement("create.form.title.bgname", "create.form.title.bgname", BusinessGroup.MAX_GROUP_NAME_LENGTH, "", formLayout);
			businessGroupName.setNotLongerThanCheck(BusinessGroup.MAX_GROUP_NAME_LENGTH, "create.form.error.nameTooLong");
			businessGroupName.setRegexMatchCheck(BusinessGroup.VALID_GROUPNAME_REGEXP, "create.form.error.illegalName");
		}
		businessGroupName.setMandatory(true);

		// Create the business group description input rich text element
		businessGroupDescription = uifactory.addRichTextElementForStringDataMinimalistic("create.form.title.description",
				"create.form.title.description", "", 10, -1, false, formLayout, ureq.getUserSession(), getWindowControl());

		if(businessGroup != null && !bulkMode) {
			BusinessControlFactory bcf = BusinessControlFactory.getInstance();
			List<ContextEntry> entries = bcf.createCEListFromString("[BusinessGroup:" + businessGroup.getKey() + "]");
			String url = BusinessControlFactory.getInstance().getAsURIString(entries, true);
			uifactory.addStaticTextElement("create.form.businesspath", url, formLayout);
		}
		
		uifactory.addSpacerElement("myspacer", formLayout, true);

		// Minimum members input
		businessGroupMinimumMembers = uifactory.addTextElement("create.form.title.min", "create.form.title.min", 5, "", formLayout);
		businessGroupMinimumMembers.setDisplaySize(6);
		businessGroupMinimumMembers.setVisible(false); // currently the minimum feature is not enabled

		// Maximum members input
		businessGroupMaximumMembers = uifactory.addTextElement("create.form.title.max", "create.form.title.max", 5, "", formLayout);
		businessGroupMaximumMembers.setDisplaySize(6);

		// Checkboxes
		enableWaitingList = uifactory.addCheckboxesHorizontal("create.form.enableWaitinglist", null, formLayout, waitingListKeys,
				waitingListValues, null);
		enableAutoCloseRanks = uifactory.addCheckboxesHorizontal("create.form.enableAutoCloseRanks", null, formLayout, autoCloseKeys,
				autoCloseValues, null);

		// Enable only if specification of min and max members is possible
		businessGroupMinimumMembers.setVisible(false); // currently the minimum feature is not enabled
		businessGroupMaximumMembers.setVisible(true);
		enableWaitingList.setVisible(true);
		enableAutoCloseRanks.setVisible(true);

		if ((businessGroup != null) && (!bulkMode)) {
			businessGroupName.setValue(businessGroup.getName());
			businessGroupDescription.setValue(businessGroup.getDescription());
			Integer minimumMembers = businessGroup.getMinParticipants();
			Integer maximumMembers = businessGroup.getMaxParticipants();
			businessGroupMinimumMembers.setValue(minimumMembers == null || minimumMembers.intValue() <= 0 ? "" : minimumMembers.toString());
			businessGroupMaximumMembers.setValue(maximumMembers == null || maximumMembers.intValue() <= 0 ? "" : maximumMembers.toString());
			if (businessGroup.getWaitingListEnabled() != null) {
				enableWaitingList.select("create.form.enableWaitinglist", businessGroup.getWaitingListEnabled());
			}
			if (businessGroup.getAutoCloseRanksEnabled() != null) {
				enableAutoCloseRanks.select("create.form.enableAutoCloseRanks", businessGroup.getAutoCloseRanksEnabled());
			}
		}
		
		if(!embbeded) {
			// Create submit and cancel buttons
			final FormLayoutContainer buttonLayout = FormLayoutContainer.createButtonLayout("buttonLayout", getTranslator());
			formLayout.add(buttonLayout);
			uifactory.addFormSubmitButton("finish", buttonLayout);
			uifactory.addFormCancelButton("cancel", buttonLayout, ureq, getWindowControl());
		}
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#validateFormLogic(org.olat.core.gui.UserRequest)
	 */
	@Override
	public boolean validateFormLogic(UserRequest ureq) {
		// 1) Check valid group names
		if (!StringHelper.containsNonWhitespace(businessGroupName.getValue())) {
			businessGroupName.setErrorKey("form.legende.mandatory", new String[] {});
			return false;
		}
		
		if (bulkMode) {
			// check all names to be valid and check that at least one is entered
			// e.g. find "," | " , " | ",,," errors => no group entered
			String selectionAsCsvStr = businessGroupName.getValue();
			String[] activeSelection = selectionAsCsvStr != null ? selectionAsCsvStr.split(",") : new String[] {};
			validNames = new HashSet<String>();
			Set<String> wrongNames = new HashSet<String>();
			boolean nameTooLong = false;
			for (int i = 0; i < activeSelection.length; i++) {
				String currentName = activeSelection[i].trim();
				if (currentName.getBytes().length > BusinessGroup.MAX_GROUP_NAME_LENGTH) {
					nameTooLong = true;					
				} else if ((currentName).matches(BusinessGroup.VALID_GROUPNAME_REGEXP)) {							
					validNames.add(currentName);
				} else {
					wrongNames.add(currentName);
				}
			}
			if (validNames.size() == 0 && wrongNames.size() == 0 && !nameTooLong) {
				// no valid name and no invalid names, this is no names
				businessGroupName.setErrorKey("create.form.error.illegalName", new String[] {});
				return false;
			} else if(nameTooLong) {
				businessGroupName.setErrorKey("create.form.error.nameTooLong", new String[] { BusinessGroup.MAX_GROUP_NAME_LENGTH + ""});
				return false;
			}	else if (wrongNames.size() == 1) {
				// one invalid name
				businessGroupName.setErrorKey("create.form.error.illegalName", new String[] {});
				return false;
			} else if (wrongNames.size() > 1) {
				// two or more invalid names
				String[] args = new String[] { StringHelper.formatAsCSVString(wrongNames) };
				businessGroupName.setErrorKey("create.form.error.illegalNames", args);
				return false;
			}
		} else {
			if (businessGroupName.hasError()) return false; // auto-validations from form, return false, because of that clearError()-calls everywhere...
		}
		// all group name tests passed
		businessGroupName.clearError();

		// 2) Check valid description
		if (businessGroupDescription.getValue().length() > 4000) {
			businessGroupDescription.setErrorKey("input.toolong", new String[] {});
			return false;
		}
		businessGroupDescription.clearError();

		// 3) Check auto close settings
		boolean disableWaitingListOk = true;
		if ((businessGroup != null) && (businessGroup.getWaitingGroup() != null)) {
			int waitingPartipiciantSize = BaseSecurityManager.getInstance().countIdentitiesOfSecurityGroup(businessGroup.getWaitingGroup());
			if ((businessGroup.getWaitingListEnabled()).booleanValue() && !isWaitingListEnabled() && (waitingPartipiciantSize > 0)) {
				enableAutoCloseRanks.setErrorKey("form.error.disableNonEmptyWaitingList", new String[] {});
				disableWaitingListOk = false;
				setEnableWaitingList(true);
				return false;
			}
		}
		enableAutoCloseRanks.clearError();
		
		if (disableWaitingListOk) {
			// 4) Check min / max settings
			String maxValue = null;
			if (StringHelper.containsNonWhitespace(businessGroupMaximumMembers.getValue())) {
				maxValue = businessGroupMaximumMembers.getValue();
			}
			String minValue = null;
			if (StringHelper.containsNonWhitespace(businessGroupMinimumMembers.getValue())){
				minValue = businessGroupMinimumMembers.getValue();
			}
			if (isWaitingListEnabled() && (maxValue == null || minValue == "")) {
				enableWaitingList.setErrorKey("create.form.error.enableWaitinglist", new String[] {});
				return false;
			}
			enableWaitingList.clearError();

			// 5) Check auto close - waiting list dependency
			if (isAutoCloseRanksEnabled() && !isWaitingListEnabled()) {
				enableAutoCloseRanks.setErrorKey("create.form.error.enableAutoCloseRanks", new String[] {});
				return false;
			}
			enableAutoCloseRanks.clearError();

			// 6) Check min/max validity
			if (! businessGroupMaximumMembers.getValue().matches("^\\p{Space}*(\\p{Digit}*)\\p{Space}*$") ) {
				businessGroupMaximumMembers.setErrorKey("create.form.error.numberOrNull", new String[] {});
				return false;
			}
			if (!businessGroupMinimumMembers.getValue().matches("^\\p{Space}*(\\p{Digit}*)\\p{Space}*$")) {
				businessGroupMaximumMembers.setErrorKey("create.form.error.numberOrNull", new String[] {});
				return false;
			}
			businessGroupMaximumMembers.clearError();
		}
	  // group name duplication test passed
		businessGroupName.clearError();
		
		// all checks passed
		return true;
	}

	/**
	 * @param name
	 */
	public void setGroupName(String name) {
		businessGroupName.setValue(name);
	}

	/**
	 * @return
	 */
	public String getGroupDescription() {
		return businessGroupDescription.getValue();
	}

	/**
	 * @return
	 */
	public Set<String> getGroupNames() {
		return validNames;
	}

	/**
	 * @return
	 */
	public String getGroupName() {
		return businessGroupName.getValue();
	}

	/**
	 * @return
	 */
	public Integer getGroupMax() {
		String result = businessGroupMaximumMembers.getValue();
		if (StringHelper.containsNonWhitespace(result)) {
			result = result.replaceAll(" ", "");
			return Integer.parseInt(result);
		} else {
			return null;
		}
	}

	/**
	 * @return
	 */
	public Integer getGroupMin() {
		String result = businessGroupMinimumMembers.getValue();
		if (StringHelper.containsNonWhitespace(result)) {
			result = result.replaceAll(" ", "");
			return Integer.parseInt(result);
		} else {
			return null;
		}
	}

	/**
	 * @return
	 */
	public boolean isAutoCloseRanksEnabled() {
		return enableAutoCloseRanks.getSelectedKeys().size() != 0;
	}

	/**
	 * @param enableAutoCloseRanks
	 */
	public void setEnableAutoCloseRanks(Boolean enableAutoCloseRanks) {
		if(enableAutoCloseRanks != null) {
			this.enableAutoCloseRanks.select("create.form.enableAutoCloseRanks", enableAutoCloseRanks.booleanValue());
		}
	}

	/**
	 * @return
	 */
	public boolean isWaitingListEnabled() {
		return enableWaitingList.getSelectedKeys().size() != 0;
	}

	/**
	 * @param enableWaitingList
	 */
	public void setEnableWaitingList(Boolean enableWaitingList) {
		if(enableWaitingList != null) {
			this.enableWaitingList.select("create.form.enableWaitinglist", enableWaitingList.booleanValue());
		}
	}

	/**
	 * @return
	 */
	public BusinessGroup getBusinessGroup() {
		return businessGroup;
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#doDispose()
	 */
	@Override
	protected void doDispose() {
	// Nothing to dispose
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formCancelled(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formNOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formNOK(UserRequest ureq) {
		fireEvent(ureq, Event.FAILED_EVENT);
	}
}
