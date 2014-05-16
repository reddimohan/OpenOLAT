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
package org.olat.core.gui.components.form.flexible.impl.elements.table;

import java.util.List;

import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.DefaultComponentRenderer;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableFilter;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableSort;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormJSHelper;
import org.olat.core.gui.components.form.flexible.impl.NameValuePair;
import org.olat.core.gui.render.RenderResult;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.StringHelper;

/**
 * 
 * Initial date: 01.03.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public abstract class AbstractFlexiTableRenderer extends DefaultComponentRenderer {

	@Override
	public void render(Renderer renderer, StringOutput sb, Component source, URLBuilder ubu, Translator translator,
			RenderResult renderResult, String[] args) {
		
		FlexiTableComponent ftC = (FlexiTableComponent) source;
		FlexiTableElementImpl ftE = ftC.getFlexiTableElement();

		renderHeaderButtons(renderer, sb, ftE, ubu, translator, renderResult, args);
		
		sb.append("<div class=\"o_table_wrapper o_table_flexi")
		  .append(" o_table_edit", ftE.isEditMode());
		String css = ftE.getElementCssClass();
		if (css != null) {
			sb.append(" ").append(css);
		}
		switch(ftE.getRendererType()) {
			case custom: sb.append(" o_rendertype_custom"); break;
			case classic: sb.append(" o_rendertype_classic"); break;
			case dataTables: sb.append(" o_rendertype_dataTables"); break;
		}
		sb.append(" table-responsive\">");
		String id = ftC.getFormDispatchId();
		sb.append("<table id=\"").append(id).append("\" class=\"table table-condensed  table-striped table-hover table-responsive\">");
		
		//render headers
		renderHeaders(sb, ftC, translator);
		//render body
		sb.append("<tbody>");
		renderBody(renderer, sb, ftC, ubu, translator, renderResult);
		sb.append("</tbody></table>");
		
		renderFooterButtons(renderer, sb, ftC, ubu, translator, renderResult, args);
		sb.append("</div>");
		
		//source
		if (source.isEnabled()) {
			sb.append(FormJSHelper.getJSStartWithVarDeclaration(id));
			sb.append(FormJSHelper.getSetFlexiFormDirty(ftE.getRootForm(), id));
			sb.append(FormJSHelper.getJSEnd());
		}
	}
	
	protected void renderHeaderButtons(Renderer renderer, StringOutput sb, FlexiTableElementImpl ftE, URLBuilder ubu, Translator translator,
			RenderResult renderResult, String[] args) {

		Component searchCmp = ftE.getExtendedSearchComponent();
		if(searchCmp != null && !ftE.isExtendedSearchCallout() && ftE.isExtendedSearchExpanded()) {
			renderer.render(searchCmp, sb, args);
			sb.append("<div class='row clearfix'><div class='col-lg-6'></div>");
		} else {
			sb.append("<div class='row clearfix'><div class='col-lg-6'>");
			renderHeaderSearch(renderer, sb, ftE, ubu, translator, renderResult, args);
			sb.append("</div>");
		}

		sb.append("<div class='col-lg-2'></div><div class='col-lg-4'>");
		renderHeaderTools(renderer, sb, ftE, ubu, translator, renderResult,  args);
		sb.append("</div></div>");
	}
	
	protected void renderHeaderSearch(Renderer renderer, StringOutput sb, FlexiTableElementImpl ftE, URLBuilder ubu, Translator translator,
			RenderResult renderResult, String[] args) {

		if(ftE.isSearchEnabled()) {
			sb.append("<div class='o_table_search input-group'>");
			renderFormItem(renderer, sb, ftE.getSearchElement(), ubu, translator, renderResult, args);
			sb.append("<div class='input-group-btn'>");
			renderFormItem(renderer, sb, ftE.getSearchButton(), ubu, translator, renderResult, args);
			if(ftE.getExtendedSearchButton() != null) {
				renderFormItem(renderer, sb, ftE.getExtendedSearchButton(), ubu, translator, renderResult, args);
			}
			sb.append("</div></div>");
		} else if(ftE.getExtendedSearchButton() != null) {
			renderFormItem(renderer, sb, ftE.getExtendedSearchButton(), ubu, translator, renderResult, args);
		}
	}
	
	/**
	 * The method rendered the tools:  filter, sort, customize, export, switch type of renderer.
	 * @param renderer
	 * @param sb
	 * @param ftE
	 * @param ubu
	 * @param translator
	 * @param renderResult
	 * @param args
	 */
	protected void renderHeaderTools(Renderer renderer, StringOutput sb, FlexiTableElementImpl ftE, URLBuilder ubu, Translator translator,
			RenderResult renderResult, String[] args) {
		
		sb.append("<div class='pull-right o_table_tools'>");
		
		//filter
		if(ftE.isFilterEnabled()) {
			List<FlexiTableFilter> filters = ftE.getFilters();
			if(filters != null && filters.size() > 0) {
				renderFilterDropdown(sb, ftE, filters);
			}
		}
		
		//sort
		if(ftE.isSortEnabled()) {
			List<FlexiTableSort> sorts = ftE.getSorts();
			if(sorts != null && sorts.size() > 0) {
				renderSortDropdown(sb, ftE, sorts);
			}
		}
		
		if(ftE.getExportButton() != null && ftE.isExportEnabled()) {
			sb.append("<div class='btn-group'>");
			renderFormItem(renderer, sb, ftE.getExportButton(), ubu, translator, renderResult, args);
			sb.append("</div> ");
		}
		if(ftE.getCustomButton() != null && ftE.isCustomizeColumns()) {
			sb.append("<div class='btn-group'>");
			renderFormItem(renderer, sb, ftE.getCustomButton(), ubu, translator, renderResult, args);
			sb.append("</div> ");
		}
		
		//switch type of tables
		FlexiTableRendererType[] types = ftE.getAvailableRendererTypes();
		if(types.length > 1) {
			sb.append("<div class='btn-group'>");
			for(FlexiTableRendererType type:types) {
				renderHeaderSwitchType(type, renderer, sb, ftE, ubu, translator, renderResult, args);
			}
			sb.append("</div> ");
		}
		sb.append("</div>");
	}
	
	protected void renderFilterDropdown(StringOutput sb, FlexiTableElementImpl ftE, List<FlexiTableFilter> filters) {
		Form theForm = ftE.getRootForm();
		String dispatchId = ftE.getFormDispatchId();
		
		sb.append("<div class='btn-group'>")
		  .append("<button class='btn btn-default dropdown-toggle' data-toggle='dropdown'>")
		  .append("<i class='o_icon o_icon_filter o_icon-lg'>&nbsp;</i> <b class='caret'></b></button>")
		  .append("<ul class='dropdown-menu' role='menu'>");
		
		for(FlexiTableFilter filter:filters) {
			if(FlexiTableFilter.SPACER.equals(filter)) {
				sb.append("<li class='divider'></li>");
			} else {
				sb.append("<li><a href=\"javascript:")
				  .append(FormJSHelper.getXHRFnCallFor(theForm, dispatchId, 1, new NameValuePair("filter", filter.getFilter())))
				  .append("\">").append("<i class='o_icon o_icon_check o_icon-fw'>&nbsp;</i> ", filter.isSelected())
				  .append(filter.getLabel()).append("</a></li>");
			}
		}
		sb.append("</ul></div> ");
	}
	
	protected void renderSortDropdown(StringOutput sb, FlexiTableElementImpl ftE, List<FlexiTableSort> sorts) {
		Form theForm = ftE.getRootForm();
		String dispatchId = ftE.getFormDispatchId();
		
		sb.append("<div class='btn-group'>")
		  .append("<button class='btn btn-default dropdown-toggle' data-toggle='dropdown'>")
		  .append("<i class='o_icon o_icon_sort_menu o_icon-lg'>&nbsp;</i> <b class='caret'></b></button>")
		  .append("<ul class='dropdown-menu' role='menu'>");
		
		for(FlexiTableSort sort:sorts) {
			if(FlexiTableSort.SPACER.equals(sort)) {
				sb.append("<li class='divider'></li>");
			} else {
				sb.append("<li><a href=\"javascript:")
				  .append(FormJSHelper.getXHRFnCallFor(theForm, dispatchId, 1,
						  new NameValuePair("sort", sort.getSortKey().getKey()),
						  new NameValuePair("asc",  sort.getSortKey().isAsc() ? "desc" : "asc")))
				  .append("\">");
				if(sort.isSelected()) {
					if(sort.getSortKey().isAsc()) {
						sb.append("<i class='o_icon o_icon_sort_desc o_icon-fw'>&nbsp;</i> ");
					} else {
						sb.append("<i class='o_icon o_icon_sort_asc o_icon-fw'>&nbsp;</i> ");
					}
				}
				sb.append(sort.getLabel()).append("</a></li>");
			}
		}
		sb.append("</ul></div> ");
	}
	
	protected void renderHeaderSwitchType(FlexiTableRendererType type, Renderer renderer, StringOutput sb, FlexiTableElementImpl ftE, URLBuilder ubu, Translator translator,
			RenderResult renderResult, String[] args) {
		if(type != null) {
			switch(type) {
				case custom: {
					renderFormItem(renderer, sb, ftE.getCustomTypeButton(), ubu, translator, renderResult, args);
					break;
				}
				case classic: {
					renderFormItem(renderer, sb, ftE.getClassicTypeButton(), ubu, translator, renderResult, args);
					break;
				}
				case dataTables: {
					renderFormItem(renderer, sb, ftE.getDataTablesTypeButton(), ubu, translator, renderResult, args);
					break;
				}
			}
		}
	}
	
	protected void renderFormItem(Renderer renderer, StringOutput sb, FormItem item, URLBuilder ubu, Translator translator,
			RenderResult renderResult, String[] args) {
		if(item != null) {
			Component cmp = item.getComponent();
			cmp.getHTMLRendererSingleton().render(renderer, sb, cmp, ubu, translator, renderResult, args);
		}
	}
	
	protected void renderFooterButtons(Renderer renderer, StringOutput sb, FlexiTableComponent ftC, URLBuilder ubu, Translator translator,
			RenderResult renderResult, String[] args) {

		FlexiTableElementImpl ftE = ftC.getFlexiTableElement();
		if(ftE.isSelectAllEnable()) {
			String formName = ftE.getRootForm().getFormName();
			String dispatchId = ftE.getFormDispatchId();

			sb.append("<div class='o_table_footer'><div class='o_table_checkall input-sm'>");

			sb.append("<label class='checkbox-inline'><a id=\"")
			  .append(dispatchId).append("\" href=\"javascript:b_table_toggleCheck('").append(formName).append("', true);")
			  .append(FormJSHelper.getXHRFnCallFor(ftE.getRootForm(), dispatchId, 1, new NameValuePair("select", "checkall")))
			  .append("\"><input type='checkbox' checked='checked' disabled='disabled' /><span>").append(translator.translate("form.checkall"))
			  .append("</span></a></label>");

			sb.append("<label class='checkbox-inline'><a id=\"")
			  .append(dispatchId).append("\" href=\"javascript:b_table_toggleCheck('").append(formName).append("', false);")
			  .append(FormJSHelper.getXHRFnCallFor(ftE.getRootForm(), dispatchId, 1, new NameValuePair("select", "uncheckall")))
			  .append("\"><input type='checkbox' disabled='disabled' /><span>").append(translator.translate("form.uncheckall"))
			  .append("</span></a></label>");

			sb.append("</div></div>");
		}
		
		if(ftE.getRendererType() != FlexiTableRendererType.dataTables && ftE.getPageSize() > 0) {
			renderPagesLinks(sb, ftC);
		}
	}
	
	protected void renderHeaders(StringOutput target, FlexiTableComponent ftC, Translator translator) {
		FlexiTableElementImpl ftE = ftC.getFlexiTableElement();
		FlexiTableDataModel<?> dataModel = ftE.getTableDataModel();
		FlexiTableColumnModel columnModel = dataModel.getTableColumnModel();
		      
		target.append("<thead><tr>");

		int col = 0;
		if(ftE.isMultiSelect()) {
			String choice = translator.translate("table.header.choice");
			target.append("<th class='b_first_child'>").append(choice).append("</th>");
			col++;
		}
		
		int cols = columnModel.getColumnCount();
		for(int i=0; i<cols; i++) {
			FlexiColumnModel fcm = columnModel.getColumnModel(i);
			if(ftE.isColumnModelVisible(fcm)) {
				renderHeader(target, ftC, fcm, col++, cols, translator);
			}
		}
		
		target.append("</tr></thead>");
	}
	
	protected void renderHeader(StringOutput target, FlexiTableComponent ftC, FlexiColumnModel fcm, int colPos, int numOfCols, Translator translator) {
		String header;
		if(StringHelper.containsNonWhitespace(fcm.getHeaderLabel())) {
			header = fcm.getHeaderLabel();
		} else {
			header = translator.translate(fcm.getHeaderKey());
		}
		target.append("<th class=\"");
		// add css class for first and last column to support older browsers
		if (colPos == 0) target.append(" b_first_child");
		if (colPos == numOfCols-1) target.append(" b_last_child");
		target.append("\">").append(header);
		renderHeaderSort(target, ftC, fcm, colPos, translator);
		target.append("</th>");
	}
	
	protected abstract void renderHeaderSort(StringOutput target, FlexiTableComponent ftC, FlexiColumnModel fcm, int colPos, Translator translator);
	
	protected void renderBody(Renderer renderer, StringOutput target, FlexiTableComponent ftC,
			URLBuilder ubu, Translator translator, RenderResult renderResult) {
		
		String id = ftC.getFormDispatchId();
		FlexiTableElementImpl ftE = ftC.getFlexiTableElement();
		FlexiTableDataModel<?> dataModel = ftE.getTableDataModel();
		
		// the really selected rowid (from the tabledatamodel)
		int firstRow = ftE.getFirstRow();
		int maxRows = ftE.getMaxRows();
		int rows = dataModel.getRowCount();
		int lastRow = Math.min(rows, firstRow + maxRows);

		String rowIdPrefix = "row_" + id + "-";
		for (int i = firstRow; i < lastRow; i++) {
			if(dataModel.isRowLoaded(i)) {
				renderRow(renderer, target, ftC, rowIdPrefix,	i, rows, ubu, translator, renderResult);
			}
		}				
		// end of table table
	}
	
	protected void renderRow(Renderer renderer, StringOutput target, FlexiTableComponent ftC, String rowIdPrefix,
			int row, int rows, URLBuilder ubu, Translator translator, RenderResult renderResult) {

		FlexiTableElementImpl ftE = ftC.getFlexiTableElement();
		FlexiTableColumnModel columnsModel = ftE.getTableDataModel().getTableColumnModel();
		int numOfCols = columnsModel.getColumnCount();
		
		// use alternating css class
		String cssClass;
		if (row % 2 == 0) cssClass = "";
		else cssClass = "b_table_odd";
		// add css class for first and last column to support older browsers
		if (row == 0) cssClass += " b_first_child";
		if (row == rows-1) cssClass += " b_last_child";

		target.append("<tr id='").append(rowIdPrefix).append(row)
				  .append("' class=\"").append(cssClass).append("\">");
				
		int col = 0;
		if(ftE.isMultiSelect()) {
			target.append("<td class='b_first_child'>")
			      .append("<input type='checkbox' name='tb_ms' value='").append(rowIdPrefix).append(row).append("'");
			if(ftE.isAllSelectedIndex() || ftE.isMultiSelectedIndex(row)) {
				target.append(" checked='checked'");
			}   
			target.append("/></td>");
			col++;
		}
				
		for (int j = 0; j<numOfCols; j++) {
			FlexiColumnModel fcm = columnsModel.getColumnModel(j);
			if(ftE.isColumnModelVisible(fcm)) {
				renderCell(renderer, target, ftC, fcm, row, col++, numOfCols, ubu, translator, renderResult);
			}
		}
		target.append("</tr>");
	}

	protected void renderCell(Renderer renderer, StringOutput target, FlexiTableComponent ftC, FlexiColumnModel fcm,
			int row, int col, int numOfCols, URLBuilder ubu, Translator translator, RenderResult renderResult) {

		FlexiTableElementImpl ftE = ftC.getFlexiTableElement();
		FlexiTableDataModel<?> dataModel = ftE.getTableDataModel();

		int alignment = fcm.getAlignment();
		String cssClass = (alignment == FlexiColumnModel.ALIGNMENT_LEFT ? "text-left" : (alignment == FlexiColumnModel.ALIGNMENT_RIGHT ? "text-right" : "text-center"));
		// add css class for first and last column to support older browsers
		if (col == 0) cssClass += " b_first_child";
		if (col == numOfCols-1) cssClass += " b_last_child";				
		target.append("<td class=\"").append(cssClass).append("\">");
		if (col == 0) target.append("<a name=\"table\"></a>"); //add once for accessabillitykey

		int columnIndex = fcm.getColumnIndex();
		Object cellValue = columnIndex >= 0 ? 
				dataModel.getValueAt(row, columnIndex) : null;
		if (cellValue instanceof FormItem) {
			FormItem formItem = (FormItem)cellValue;
			formItem.setTranslator(translator);
			if(ftE.getRootForm() != formItem.getRootForm()) {
				formItem.setRootForm(ftE.getRootForm());
			}
			ftE.addFormItem(formItem);
			if(formItem.isVisible()) {
				formItem.getComponent().getHTMLRendererSingleton().render(renderer, target, formItem.getComponent(),
					ubu, translator, renderResult, null);
			}
		} else if(cellValue instanceof Component) {
			Component cmp = (Component)cellValue;
			cmp.setTranslator(translator);
			if(cmp.isVisible()) {
				cmp.getHTMLRendererSingleton().render(renderer, target, cmp,
					ubu, translator, renderResult, null);
			}
		} else {
			fcm.getCellRenderer().render(target, cellValue, row, ftC, ubu, translator);
		}
		target.append("</td>");
	}
	

	private void renderPagesLinks(StringOutput sb, FlexiTableComponent ftC) {
		FlexiTableElementImpl ftE = ftC.getFlexiTableElement();
		int pageSize = ftE.getPageSize();
		FlexiTableDataModel<?> dataModel = ftE.getTableDataModel();
		int rows = dataModel.getRowCount();
		
		if(pageSize > 0 && rows > pageSize) {
			sb.append("<ul class='pagination'>");

			int page = ftE.getPage();
			int maxPage = (int)Math.ceil(((double) rows / (double) pageSize));
	
			renderPageBackLink(sb, ftC, page);
			renderPageNumberLinks(sb, ftC, page, maxPage);
			renderPageNextLink(sb, ftC, page, maxPage);

			sb.append("</ul>");
		}
	}
	
	private void renderPageBackLink(StringOutput sb, FlexiTableComponent ftC, int page) {
		boolean disabled = (page <= 0);
		FlexiTableElementImpl ftE = ftC.getFlexiTableElement();
		Form theForm = ftE.getRootForm();
		sb.append("<li").append(" class='disabled'", disabled).append("><a href='");
		if(disabled) {
			sb.append("#");
		} else {
			sb.append("javascript:")
			  .append(FormJSHelper.getXHRFnCallFor(theForm, ftC.getFormDispatchId(), 1, new NameValuePair("page", Integer.toString(page - 1))));
		}
		sb.append("'>").append("&laquo;").append("</a></li>");
	}
	
	private void renderPageNextLink(StringOutput sb, FlexiTableComponent ftC, int page, int maxPage) {
		boolean disabled = (page >= maxPage);
		FlexiTableElementImpl ftE = ftC.getFlexiTableElement();
		Form theForm = ftE.getRootForm();
		sb.append("<li ").append(" class='disabled'", disabled).append("><a href='");
		if(disabled) {
			sb.append("#");
		} else {
			sb.append("javascript:")
			  .append(FormJSHelper.getXHRFnCallFor(theForm, ftC.getFormDispatchId(), 1, new NameValuePair("page", Integer.toString(page + 1)))); 
		}
		sb.append("'>").append("&raquo;").append("</li></a>");
	}
	
	private void renderPageNumberLinks(StringOutput sb, FlexiTableComponent ftC, int page, int maxPage) {
		if (maxPage < 12) {
			for (int i=0; i<maxPage; i++) {
				appendPagenNumberLink(sb, ftC, page, i);
			}
		} else {
			int powerOf10 = String.valueOf(maxPage).length() - 1;
			int maxStepSize = (int) Math.pow(10, powerOf10);
			int stepSize = (int) Math.pow(10, String.valueOf(page).length() - 1);
			boolean isStep = false;
			int useEveryStep = 3;
			int stepCnt = 0;
			boolean isNear = false;
			int nearleft = 5;
			int nearright = 5;
			if (page < nearleft) {
				nearleft = page;
				nearright += (nearright - nearleft);
			} else if (page > (maxPage - nearright)) {
				nearright = maxPage - page;
				nearleft += (nearleft - nearright);
			}
			for (int i = 1; i <= maxPage; i++) {
				// adapt stepsize if needed
				stepSize = adaptStepIfNeeded(page, maxStepSize, stepSize, i);
	
				isStep = ((i % stepSize) == 0);
				if (isStep) {
					stepCnt++;
					isStep = isStep && (stepCnt % useEveryStep == 0);
				}
				isNear = (i > (page - nearleft) && i < (page + nearright));
				if (i == 1 || i == maxPage || isStep || isNear) {
					appendPagenNumberLink(sb, ftC, page, i);
				}
			}
		}
	}
	
	private void appendPagenNumberLink(StringOutput sb, FlexiTableComponent ftC, int page, int i) {
		FlexiTableElementImpl ftE = ftC.getFlexiTableElement();
		Form theForm = ftE.getRootForm();
		sb.append("<li").append(" class='active'", (page == i)).append("><a href=\"javascript:")
		  .append(FormJSHelper.getXHRFnCallFor(theForm, ftC.getFormDispatchId(), 1, new NameValuePair("page", Integer.toString(i))))
		  .append("\">").append(i+1).append("</a></li>");
	}

	private int adaptStepIfNeeded(final int page, final int maxStepSize, final int stepSize, final int i) {
		int newStepSize = stepSize;
		if (i < page && stepSize > 1 && ((page - i) / stepSize == 0)) {
			newStepSize = stepSize / 10;
		} else if (i > page && stepSize < maxStepSize && ((i - page) / stepSize == 9)) {
			newStepSize = stepSize * 10;
		}
		return newStepSize;
	}
}
