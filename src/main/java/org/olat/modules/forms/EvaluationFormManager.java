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
package org.olat.modules.forms;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.olat.basesecurity.IdentityRef;
import org.olat.core.commons.persistence.SortKey;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.modules.forms.model.jpa.EvaluationFormResponses;
import org.olat.modules.forms.model.xml.Form;
import org.olat.modules.forms.model.xml.Rubric;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryRef;

/**
 * 
 * Initial date: 12 déc. 2016<br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public interface EvaluationFormManager {

	public Form loadForm(RepositoryEntry formEntry);

	public EvaluationFormSurvey createSurvey(OLATResourceable ores, String subIdent, RepositoryEntry formEntry);
	
	public EvaluationFormSurvey createSurvey(OLATResourceable ores, String subIdent, RepositoryEntry formEntry,
			EvaluationFormSurvey previous);

	public EvaluationFormSurvey loadSurvey(OLATResourceable ores, String subIdent);

	public boolean isFormUpdateable(EvaluationFormSurvey survey);

	/**
	 * Update the form of a survey. Use {@link isFormUpdateable(survey)} to check if
	 * the form can be updated before using this method. If the form can not be
	 * updated anymore, the unchanged survey is returned.
	 *
	 * @param survey
	 * @param formEntry
	 * @return the survey
	 */
	public EvaluationFormSurvey updateSurveyForm(EvaluationFormSurvey survey, RepositoryEntry formEntry);

	/**
	 * Deletes all data of a survey but not the survey itself. It deletes all
	 * responses, sessions and participations.
	 *
	 * @param survey
	 */
	public void deleteAllData(EvaluationFormSurvey survey);

	/**
	 * Delete the survey and all data but the evaluation form.
	 *
	 * @param survey
	 */
	public void deleteSurvey(EvaluationFormSurvey survey);

	public EvaluationFormParticipation createParticipation(EvaluationFormSurvey survey);

	public EvaluationFormParticipation createParticipation(EvaluationFormSurvey survey, Identity executor);

	public EvaluationFormParticipation createParticipation(EvaluationFormSurvey survey, Identity executor, boolean anonymous);
	
	public EvaluationFormParticipation createParticipation(EvaluationFormSurvey survey,
			EvaluationFormParticipationIdentifier identifier);

	public EvaluationFormParticipation updateParticipation(EvaluationFormParticipation participation);
	
	public EvaluationFormParticipation loadParticipationByKey(EvaluationFormParticipationRef participationRef);

	public List<EvaluationFormParticipation> loadParticipations(EvaluationFormSurvey survey,
			EvaluationFormParticipationStatus status);

	public EvaluationFormParticipation loadParticipationByExecutor(EvaluationFormSurvey survey, IdentityRef executor);

	/**
	 * Load a participation by an identifier. If the identifier was inserted by
	 * {@link createParticipation(survey, executor)}, the identifier is only unique
	 * in combination with the survey. In all other cases the identifier is unique
	 * across all participations. If more than one participation with the same
	 * identifier (but other surveys) exists, this method returns null.
	 *
	 * @param identifier
	 * @return the participation or null
	 */
	public EvaluationFormParticipationRef loadParticipationByIdentifier(EvaluationFormParticipationIdentifier identifier);

	public EvaluationFormParticipation loadParticipationByIdentifier(EvaluationFormSurvey survey,
			EvaluationFormParticipationIdentifier identifier);
	
	/**
	 * Delete participations. It deletes the sessions and the responses as well if
	 * they are still connected with the participation. This is the case if the
	 * session is not anonymous or not finished yet.
	 *
	 * @param participationRefs
	 */
	public void deleteParticipations(List<? extends EvaluationFormParticipationRef> participationRefs);

	public EvaluationFormSession createSession(EvaluationFormParticipation participation);
	
	public EvaluationFormSession loadSessionByKey(EvaluationFormSessionRef sessionRef);

	public EvaluationFormSession loadSessionByParticipation(EvaluationFormParticipationRef participation);
	
	public List<EvaluationFormSession> loadSessionsByKey(List<? extends EvaluationFormSessionRef> sessionRefs,
			int firstResult, int maxResults, SortKey... orderBy);

	public List<EvaluationFormSession> loadSessionsBySurvey(EvaluationFormSurvey survey,
			EvaluationFormSessionStatus status);

	public EvaluationFormSession updateSession(EvaluationFormSession session, String email, String firstname, String lastname,
			String age, String gender, String orgUnit, String studySubject);

	/**
	 * Finish a session and the correspondent participation.
	 *
	 * @param sessionRef
	 * @return
	 */
	public EvaluationFormSession finishSession(EvaluationFormSessionRef sessionRef);
	
	/**
	 * Reopen a session and the correspondent participation. It is not possible to
	 * reopen a anonymous session.
	 *
	 * @param session
	 * @return
	 */
	public EvaluationFormSession reopenSession(EvaluationFormSession session);
	
	public long getCountOfSessions(EvaluationFormSurvey survey);

	public EvaluationFormResponse createStringResponse(String responseIdentifier, EvaluationFormSession session,
			String value);

	public EvaluationFormResponse createNumericalResponse(String responseIdentifier, EvaluationFormSession session,
			BigDecimal value);

	public EvaluationFormResponse createFileResponse(String responseIdentifier, EvaluationFormSession session,
			File file, String filename) throws IOException;

	public EvaluationFormResponse createNoResponse(String responseIdentifier, EvaluationFormSession session);

	public EvaluationFormResponse updateNumericalResponse(EvaluationFormResponse response, BigDecimal value);

	public EvaluationFormResponse updateStringResponse(EvaluationFormResponse response, String stringValue);

	public EvaluationFormResponse updateFileResponse(EvaluationFormResponse response, File file, String filename)
			throws IOException;

	public EvaluationFormResponse updateNoResponse(EvaluationFormResponse response);

	public EvaluationFormResponses loadResponsesBySessions(List<? extends EvaluationFormSessionRef> sessionRefs);

	public File loadResponseFile(EvaluationFormResponse response);

	public VFSLeaf loadResponseLeaf(EvaluationFormResponse response);

	public void deleteResponse(EvaluationFormResponse response);

	public void deleteResponses(List<EvaluationFormResponse> response);

	/**
	 * Is there some sessions using this repository entry.
	 * 
	 * @param formEntry
	 * @return
	 */
	public boolean isEvaluationFormActivelyUsed(RepositoryEntryRef formEntry);
	
	public EvaluationFormStatistic getSessionsStatistic(List<? extends EvaluationFormSessionRef> sessionRefs);
	
	public RubricStatistic getRubricStatistic(Rubric rubric, List<? extends EvaluationFormSessionRef> sessions);

}
