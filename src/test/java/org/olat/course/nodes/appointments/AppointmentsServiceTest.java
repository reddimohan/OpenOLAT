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
package org.olat.course.nodes.appointments;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.olat.test.JunitTestHelper.random;

import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.course.nodes.appointments.ParticipationResult.Status;
import org.olat.repository.RepositoryEntry;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 8 Jun 2020<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class AppointmentsServiceTest extends OlatTestCase {
	
	@Autowired
	private DB dbInstance;
	
	@Autowired
	private AppointmentsService sut;
	
	@Test
	public void createParticipationShouldCreateParticiption() {
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		Appointment appointment = createRandomAppointment();
		dbInstance.commitAndCloseSession();
		
		ParticipationResult result = sut.createParticipation(appointment, participant, true, false);
		
		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(result.getStatus()).isEqualTo(Status.ok);
		softly.assertThat(result.getParticipations()).isNotNull().isNotEmpty();
		softly.assertThat(result.getParticipations().get(0).getIdentity()).isEqualTo(participant);
		softly.assertAll();
	}

	@Test
	public void createParticipationShouldNotCreateParticipationIfAppointmentDeleted() {
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		Appointment appointment = createRandomAppointment();
		dbInstance.commitAndCloseSession();
		sut.deleteAppointment(appointment);
		dbInstance.commitAndCloseSession();
		
		ParticipationResult result = sut.createParticipation(appointment, participant, true, false);
		
		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(result.getStatus()).isEqualTo(Status.appointmentDeleted);
		softly.assertThat(result.getParticipations()).isNull();
		softly.assertAll();
	}
	
	@Test
	public void createParticipationShouldNotCreateParticipationIfConfirmed() {
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		Appointment appointment = createRandomAppointment();
		dbInstance.commitAndCloseSession();
		sut.confirmAppointment(appointment);
		dbInstance.commitAndCloseSession();
		
		ParticipationResult result = sut.createParticipation(appointment, participant, true, false);
		
		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(result.getStatus()).isEqualTo(Status.appointmentConfirmed);
		softly.assertThat(result.getParticipations()).isNull();
		softly.assertAll();
	}
	
	@Test
	public void createParticipationShouldNotCreateParticipationIfNoFreePlaces() {
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		Appointment appointment = createRandomAppointment();
		appointment.setMaxParticipations(2);
		sut.saveAppointment(appointment);
		dbInstance.commitAndCloseSession();
		Identity participantA = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		sut.createParticipation(appointment, participantA, true, false);
		Identity participantB = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		sut.createParticipation(appointment, participantB, true, false);
		dbInstance.commitAndCloseSession();
		
		ParticipationResult result = sut.createParticipation(appointment, participant, true, false);
		
		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(result.getStatus()).isEqualTo(Status.appointmentFull);
		softly.assertThat(result.getParticipations()).isNull();
		softly.assertAll();
	}

	@Test
	public void createParticipationShouldAutoconfirm() {
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		Appointment appointment = createRandomAppointment();
		dbInstance.commitAndCloseSession();
		
		sut.createParticipation(appointment, participant, true, true);
		dbInstance.commitAndCloseSession();
		
		AppointmentSearchParams params = new AppointmentSearchParams();
		params.setAppointment(appointment);
		List<Appointment> appointments = sut.getAppointments(params);
		assertThat(appointments).hasSize(1)
				.element(0).extracting(Appointment::getStatus).isEqualTo(Appointment.Status.confirmed);
	}

	@Test
	public void createParticipationShouldDoSingleParticipation() {
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		Topic topic = createRandomTopic();
		Appointment appointment1 = createRandomAppointment(topic);
		Appointment appointment2 = createRandomAppointment(topic);
		dbInstance.commitAndCloseSession();
		
		sut.createParticipation(appointment1, participant, false, false);
		dbInstance.commitAndCloseSession();
		sut.createParticipation(appointment2, participant, false, false);
		dbInstance.commitAndCloseSession();
		
		ParticipationSearchParams params = new ParticipationSearchParams();
		params.setTopic(topic);
		params.setIdentity(participant);
		List<Participation> participations = sut.getParticipations(params);
		assertThat(participations).hasSize(1)
				.element(0).extracting(Participation::getAppointment).isEqualTo(appointment2);
	}

	@Test
	public void createParticipationShouldDoMultiParticipation() {
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		Topic topic = createRandomTopic();
		Appointment appointment1 = createRandomAppointment(topic);
		Appointment appointment2 = createRandomAppointment(topic);
		dbInstance.commitAndCloseSession();
		
		sut.createParticipation(appointment1, participant, true, true);
		dbInstance.commitAndCloseSession();
		sut.createParticipation(appointment2, participant, true, true);
		dbInstance.commitAndCloseSession();
		
		ParticipationSearchParams params = new ParticipationSearchParams();
		params.setTopic(topic);
		params.setIdentity(participant);
		List<Participation> participations = sut.getParticipations(params);
		assertThat(participations).hasSize(2);
	}
	
	@Test
	public void rebookParticipationShouldCreateParticiption() {
		Identity participant1 = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		Identity participant2 = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		Identity participant3 = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		Appointment currentAppointment = createRandomAppointment();
		Appointment rebookAppointment = createRandomAppointment();
		ParticipationResult participationResult1 = sut.createParticipation(currentAppointment, participant1, true, false);
		Participation participation1 = participationResult1.getParticipations().get(0);
		ParticipationResult participationResult2 = sut.createParticipation(currentAppointment, participant2, true, false);
		Participation participation2 = participationResult2.getParticipations().get(0);
		ParticipationResult participationResult3 = sut.createParticipation(currentAppointment, participant3, true, false);
		Participation participation3 = participationResult3.getParticipations().get(0);
		dbInstance.commitAndCloseSession();
		
		ParticipationResult rebooked = sut.rebookParticipations(rebookAppointment, asList(participation1, participation2), false);
		dbInstance.commitAndCloseSession();
		
		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(rebooked.getStatus()).isEqualTo(Status.ok);
		softly.assertThat(rebooked.getParticipations()).hasSize(2);
		softly.assertThat(rebooked.getParticipations()).extracting(Participation::getIdentity)
				.containsExactlyInAnyOrder(participant1, participant2);
		
		ParticipationSearchParams params = new ParticipationSearchParams();
		params.setAppointment(currentAppointment);
		List<Participation> currentParticipations = sut.getParticipations(params);
		softly.assertThat(currentParticipations).hasSize(1);
		softly.assertThat(currentParticipations.get(0)).isEqualTo(participation3);
		
		softly.assertAll();
	}

	@Test
	public void rebookParticipationShouldNotCreateParticipationIfAppointmentDeleted() {
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		Appointment currentAppointment = createRandomAppointment();
		Appointment rebookAppointment = createRandomAppointment();
		ParticipationResult participationResult = sut.createParticipation(currentAppointment, participant, true, false);
		Participation participation = participationResult.getParticipations().get(0);
		dbInstance.commitAndCloseSession();
		sut.deleteAppointment(rebookAppointment);
		dbInstance.commitAndCloseSession();
		
		ParticipationResult rebooked = sut.rebookParticipations(rebookAppointment, singletonList(participation), false);
		dbInstance.commitAndCloseSession();
		
		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(rebooked.getStatus()).isEqualTo(Status.appointmentDeleted);
		softly.assertThat(rebooked.getParticipations()).isNull();
		
		ParticipationSearchParams params = new ParticipationSearchParams();
		params.setAppointment(currentAppointment);
		params.setIdentity(participant);
		assertThat(sut.getParticipations(params).get(0)).isEqualTo(participation);
		
		softly.assertAll();
	}

	@Test
	public void rebookParticipationShouldNotCreateParticipationIfNoParticipations() {
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		Appointment currentAppointment = createRandomAppointment();
		Appointment rebookAppointment = createRandomAppointment();
		ParticipationResult participationResult = sut.createParticipation(currentAppointment, participant, true, false);
		Participation participation = participationResult.getParticipations().get(0);
		dbInstance.commitAndCloseSession();
		sut.deleteParticipation(participation);
		dbInstance.commitAndCloseSession();
		
		ParticipationResult rebooked = sut.rebookParticipations(rebookAppointment, singletonList(participation), false);
		dbInstance.commitAndCloseSession();
		
		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(rebooked.getStatus()).isEqualTo(Status.noParticipations);
		softly.assertThat(rebooked.getParticipations()).isNull();
		softly.assertAll();
	}
	
	@Test
	public void rebookParticipationShouldNotCreateParticipationIfNoFreePlaces() {
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		Appointment currentAppointment = createRandomAppointment();
		Appointment rebookAppointment = createRandomAppointment();
		ParticipationResult participationResult = sut.createParticipation(currentAppointment, participant, true, false);
		Participation participation = participationResult.getParticipations().get(0);
		dbInstance.commitAndCloseSession();
		rebookAppointment.setMaxParticipations(2);
		sut.saveAppointment(rebookAppointment);
		dbInstance.commitAndCloseSession();
		Identity participantA = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		sut.createParticipation(rebookAppointment, participantA, true, false);
		Identity participantB = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		sut.createParticipation(rebookAppointment, participantB, true, false);
		dbInstance.commitAndCloseSession();
		
		ParticipationResult rebooked = sut.rebookParticipations(rebookAppointment, singletonList(participation), false);
		dbInstance.commitAndCloseSession();
		
		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(rebooked.getStatus()).isEqualTo(Status.appointmentFull);
		softly.assertThat(rebooked.getParticipations()).isNull();
		
		ParticipationSearchParams params = new ParticipationSearchParams();
		params.setAppointment(currentAppointment);
		params.setIdentity(participant);
		assertThat(sut.getParticipations(params).get(0)).isEqualTo(participation);
		
		softly.assertAll();
	}
	

	@Test
	public void rebookParticipationShouldAutoconfirm() {
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		Appointment currentAppointment = createRandomAppointment();
		Appointment rebookAppointment = createRandomAppointment();
		ParticipationResult participationResult = sut.createParticipation(currentAppointment, participant, true, false);
		Participation participation = participationResult.getParticipations().get(0);
		dbInstance.commitAndCloseSession();
		
		sut.rebookParticipations(rebookAppointment, singletonList(participation), true);
		dbInstance.commitAndCloseSession();
		
		AppointmentSearchParams params = new AppointmentSearchParams();
		params.setAppointment(rebookAppointment);
		List<Appointment> appointments = sut.getAppointments(params);
		assertThat(appointments).hasSize(1)
				.element(0).extracting(Appointment::getStatus).isEqualTo(Appointment.Status.confirmed);
	}
	
	@Test
	public void rebookParticipationShouldNotLeadToTwoParticipationsInTheSameAppointment() {
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		Appointment currentAppointment = createRandomAppointment();
		Appointment rebookAppointment = createRandomAppointment();
		ParticipationResult participationResult1 = sut.createParticipation(currentAppointment, participant, true, false);
		Participation participation1 = participationResult1.getParticipations().get(0);
		dbInstance.commitAndCloseSession();
		sut.createParticipation(rebookAppointment, participant, true, false);
		dbInstance.commitAndCloseSession();
		
		ParticipationResult rebooked = sut.rebookParticipations(rebookAppointment, asList(participation1), false);
		dbInstance.commitAndCloseSession();
		
		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(rebooked.getStatus()).isEqualTo(Status.ok);
		softly.assertThat(rebooked.getParticipations()).hasSize(0);
		
		ParticipationSearchParams rebookedParams = new ParticipationSearchParams();
		rebookedParams.setAppointment(rebookAppointment);
		List<Participation> rebookedParticipations = sut.getParticipations(rebookedParams);
		softly.assertThat(rebookedParticipations).hasSize(1);
		
		ParticipationSearchParams currentParams = new ParticipationSearchParams();
		currentParams.setAppointment(currentAppointment);
		List<Participation> currentParticipations = sut.getParticipations(currentParams);
		softly.assertThat(currentParticipations).hasSize(0);
		
		softly.assertAll();
	}
	
	private Topic createRandomTopic() {
		Identity author = JunitTestHelper.createAndPersistIdentityAsRndUser("ap");
		RepositoryEntry entry = JunitTestHelper.deployBasicCourse(author);
		Topic topic = sut.createTopic(entry, String.valueOf(new Random().nextInt()));
		topic.setTitle(random());
		return topic;
	}
	
	private Appointment createRandomAppointment() {
		Topic topic = createRandomTopic();
		return createRandomAppointment(topic);
	}
	
	private Appointment createRandomAppointment(Topic topic) {
		Appointment appointment = sut.createUnsavedAppointment(topic);
		appointment.setStart(new GregorianCalendar(2020, 7, 16, 8, 30, 0).getTime());
		appointment.setEnd(new GregorianCalendar(2020, 7, 16, 15, 30, 0).getTime());
		appointment.setLocation(random());
		appointment = sut.saveAppointment(appointment);
		return appointment;
	}
	
}
