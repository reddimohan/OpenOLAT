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
package org.olat.course.nodes.appointments.ui;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.date.DateComponentFactory;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.stack.BreadcrumbedStackedPanel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.util.DateUtils;
import org.olat.core.util.StringHelper;
import org.olat.course.nodes.appointments.Appointment;
import org.olat.course.nodes.appointments.Appointment.Status;
import org.olat.course.nodes.appointments.AppointmentSearchParams;
import org.olat.course.nodes.appointments.AppointmentsSecurityCallback;
import org.olat.course.nodes.appointments.AppointmentsService;
import org.olat.course.nodes.appointments.Organizer;
import org.olat.course.nodes.appointments.Participation;
import org.olat.course.nodes.appointments.ParticipationSearchParams;
import org.olat.course.nodes.appointments.Topic;
import org.olat.course.nodes.appointments.Topic.Type;
import org.olat.course.nodes.appointments.TopicRef;
import org.olat.repository.RepositoryEntry;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 20 Apr 2020<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class TopicsRunController extends BasicController implements Activateable2 {

	private static final String CMD_OPEN = "open";
	private static final String CMD_EMAIL = "email";

	private final VelocityContainer mainVC;

	private final BreadcrumbedStackedPanel stackPanel;
	private CloseableModalController cmc;
	private AppointmentListSelectionController topicRunCtrl;
	private OrganizerMailController mailCtrl;
	
	private final RepositoryEntry entry;
	private final String subIdent;
	private final AppointmentsSecurityCallback secCallback;

	private List<TopicWrapper> topics;
	private int counter;
	
	@Autowired
	private AppointmentsService appointmentsService;
	@Autowired
	private UserManager userManager;

	public TopicsRunController(UserRequest ureq, WindowControl wControl, BreadcrumbedStackedPanel stackPanel,
			RepositoryEntry entry, String subIdent, AppointmentsSecurityCallback secCallback) {
		super(ureq, wControl);
		this.stackPanel = stackPanel;
		this.entry = entry;
		this.subIdent = subIdent;
		this.secCallback = secCallback;
		
		mainVC = createVelocityContainer("topics_run");
		
		refresh();
		putInitialPanel(mainVC);
	}
	
	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if (topics.size() == 1
				&& (topics.get(0).getSelectedAppointments() == null
					|| topics.get(0).getSelectedAppointments().intValue() == 0)) {
			doOpenTopic(ureq, topics.get(0).getTopic());
		}
	}
	
	private void refresh() {
		mainVC.clear();
		topics = loadTopicWrappers();
		mainVC.contextPut("topics", topics);
	}

	private List<TopicWrapper> loadTopicWrappers() {
		List<Topic> topics = appointmentsService.getTopics(entry, subIdent);
		Map<Long, List<Organizer>> topicKeyToOrganizer = appointmentsService
				.getOrganizers(entry, subIdent).stream()
				.collect(Collectors.groupingBy(o -> o.getTopic().getKey()));
		
		AppointmentSearchParams freeAppointmentsParams = new AppointmentSearchParams();
		freeAppointmentsParams.setEntry(entry);
		freeAppointmentsParams.setSubIdent(subIdent);
		Map<Long, Long> topicKeyToAppointmentCount = appointmentsService.getTopicKeyToAppointmentCount(freeAppointmentsParams, true);
		
		ParticipationSearchParams myParticipationsParams = new ParticipationSearchParams();
		myParticipationsParams.setEntry(entry);
		myParticipationsParams.setSubIdent(subIdent);
		myParticipationsParams.setIdentity(getIdentity());
		myParticipationsParams.setFetchAppointments(true);
		Map<Long, List<Participation>> topicKeyToMyEnrollmentParticipation = appointmentsService
				.getParticipations(myParticipationsParams).stream()
				.collect(Collectors.groupingBy(p -> p.getAppointment().getTopic().getKey()));
		
		List<Topic> topicsFinding = topics.stream()
				.filter(topic -> Type.finding == topic.getType())
				.collect(Collectors.toList());
		
		AppointmentSearchParams confirmedFindingsParams = new AppointmentSearchParams();
		confirmedFindingsParams.setTopics(topicsFinding);
		confirmedFindingsParams.setStatus(Status.confirmed);
		Map<Long, List<Appointment>> topicKeyToFindingConfirmed = appointmentsService
				.getAppointments(confirmedFindingsParams).stream()
				.collect(Collectors.groupingBy(a -> a.getTopic().getKey()));
		
		List<TopicWrapper> wrappers = new ArrayList<>(topics.size());
		for (Topic topic : topics) {
			TopicWrapper wrapper = new TopicWrapper(topic);
			List<Organizer> organizers = topicKeyToOrganizer.getOrDefault(topic.getKey(), emptyList());
			wrapOrganizers(wrapper, organizers);
			wrapAppointment(wrapper, topicKeyToAppointmentCount, topicKeyToFindingConfirmed, topicKeyToMyEnrollmentParticipation);
			wrappers.add(wrapper);
		}
		return wrappers;
	}

	private void wrapOrganizers(TopicWrapper wrapper, List<Organizer> organizers) {
		List<String> organizerNames = new ArrayList<>(organizers.size());
		for (Organizer organizer : organizers) {
			String name = userManager.getUserDisplayName(organizer.getIdentity().getKey());
			organizerNames.add(name);
		}
		wrapper.setOrganizerNames(organizerNames);
		wrapper.setOrganizers(organizers);
		if (!organizers.isEmpty()) {
			Link link = LinkFactory.createCustomLink("email" + counter++, CMD_EMAIL, null, Link.NONTRANSLATED, mainVC, this);
			link.setIconLeftCSS("o_icon o_icon_mail");
			link.setElementCssClass("o_mail");
			link.setUserObject(wrapper);
			wrapper.setEmailLinkName(link.getComponentName());
		}
	}

	private void wrapAppointment(TopicWrapper wrapper, Map<Long, Long> topicKeyToAppointmentCount,
			Map<Long, List<Appointment>> topicKeyToFindingConfirmed,
			Map<Long, List<Participation>> topicKeyToMyEnrollmentParticipation) {
		
		Topic topic = wrapper.getTopic();
		Long freeAppointments = topicKeyToAppointmentCount.getOrDefault(topic.getKey(), Long.valueOf(0));
		wrapper.setFreeAppointments(freeAppointments);
		
		List<Participation> myTopicParticipations = topicKeyToMyEnrollmentParticipation.getOrDefault(topic.getKey(), emptyList());
		wrapper.setSelectedAppointments(Integer.valueOf(myTopicParticipations.size()));
		
		if (Type.finding == topic.getType()) {
			wrapFindindAppointment(wrapper, topicKeyToFindingConfirmed);
		} else {
			wrapEnrollmentAppointment(wrapper, myTopicParticipations);
		}
		
		if (topic.isMultiParticipation()) {
			wrapOpenLink(wrapper, topic, "appointments.select");
		} else if (Type.finding == topic.getType()) {
			wrapOpenLink(wrapper, topic, "appointment.select");
		} else if (wrapper.getStatus() == null || wrapper.getStatus() == Status.planned) {
			wrapOpenLink(wrapper, topic, "appointment.select");
		}
		
		wrapMessage(wrapper);
	}

	private void wrapFindindAppointment(TopicWrapper wrapper, Map<Long, List<Appointment>> topicKeyToFindingConfirmed) {
		List<Appointment> appointments = topicKeyToFindingConfirmed.getOrDefault(wrapper.getTopic().getKey(), emptyList());
		if (!appointments.isEmpty()) {
			Appointment appointment = appointments.get(0);
			wrapAppointmentView(wrapper, appointment);
		}
	}

	private void wrapEnrollmentAppointment(TopicWrapper wrapper, List<Participation> myTopicParticipations) {
		if (!myTopicParticipations.isEmpty()) {
			Date now = new Date();
			Optional<Appointment> nextAppointment = myTopicParticipations.stream()
					.map(Participation::getAppointment)
					.filter(a1 -> now.before(a1.getEnd()))
					.sorted((a1, a2) -> a1.getStart().compareTo(a2.getStart()))
					.findFirst();
			Appointment appointment = nextAppointment.isPresent()
					? nextAppointment.get() // Next appointment ...
					: myTopicParticipations.stream()
						.map(Participation::getAppointment)
						.sorted((a1, a2) -> a2.getStart().compareTo(a1.getStart()))
						.findFirst().get(); // ... or the most recent one.
			wrapper.setFuture(Boolean.valueOf(appointment.getStart().after(now)));
			
			wrapAppointmentView(wrapper, appointment);
			
			ParticipationSearchParams allParticipationParams = new ParticipationSearchParams();
			allParticipationParams.setAppointment(appointment);
			List<Participation> appointmentParticipations = appointmentsService.getParticipations(allParticipationParams);

			List<String> participants = appointmentParticipations.stream()
					.map(p -> userManager.getUserDisplayName(p.getIdentity().getKey()))
					.sorted(String.CASE_INSENSITIVE_ORDER)
					.collect(Collectors.toList());
			wrapper.setParticipants(participants);
		}
	}

	private void wrapAppointmentView(TopicWrapper wrapper, Appointment appointment) {
		Locale locale = getLocale();
		Date begin = appointment.getStart();
		Date end = appointment.getEnd();
		String date = null;
		String date2 = null;
		String time = null;
		
		boolean sameDay = DateUtils.isSameDay(begin, end);
		boolean sameTime = DateUtils.isSameTime(begin, end);
		String startDate = StringHelper.formatLocaleDateFull(begin.getTime(), locale);
		String startTime = StringHelper.formatLocaleTime(begin.getTime(), locale);
		String endDate = StringHelper.formatLocaleDateFull(end.getTime(), locale);
		String endTime = StringHelper.formatLocaleTime(end.getTime(), locale);
		if (sameDay) {
			StringBuilder timeSb = new StringBuilder();
			if (sameTime) {
				timeSb.append(translate("full.day"));
			} else {
				timeSb.append(startTime);
				timeSb.append(" - ");
				timeSb.append(endTime);
			}
			time = timeSb.toString();
		} else {
			StringBuilder dateSbShort1 = new StringBuilder();
			dateSbShort1.append(startDate);
			dateSbShort1.append(" ");
			dateSbShort1.append(startTime);
			dateSbShort1.append(" -");
			date = dateSbShort1.toString();
			StringBuilder dateSb2 = new StringBuilder();
			dateSb2.append(endDate);
			dateSb2.append(" ");
			dateSb2.append(endTime);
			date2 = dateSb2.toString();
		}
		
		wrapper.setDate(date);
		wrapper.setDate2(date2);
		wrapper.setTime(time);
		wrapper.setLocation(appointment.getLocation());
		wrapper.setDetails(appointment.getDetails());
		
		String dayName = "day_" + counter++;
		DateComponentFactory.createDateComponentWithYear(dayName, appointment.getStart(), mainVC);
		wrapper.setDayName(dayName);
		
		wrapper.setStatus(appointment.getStatus());
		wrapper.setTranslatedStatus(translate("appointment.status." + appointment.getStatus().name()));
		wrapper.setStatusCSS("o_ap_status_" + appointment.getStatus().name());
	}

	private void wrapOpenLink(TopicWrapper wrapper, TopicRef topic, String i18n) {
		Link openLink = LinkFactory.createCustomLink("open" + counter++, CMD_OPEN, i18n, Link.LINK, mainVC, this);
		openLink.setIconRightCSS("o_icon o_icon_start");
		openLink.setUserObject(topic);
		wrapper.setOpenLinkName(openLink.getComponentName());
	}
	
	private void wrapMessage(TopicWrapper wrapper) {
		Topic topic = wrapper.getTopic();
		int selectedAppointments = wrapper.getSelectedAppointments() != null
				? wrapper.getSelectedAppointments().intValue()
				: 0;
		Long freeAppointments = wrapper.getFreeAppointments();
		Status status = wrapper.getStatus();
		
		List<String> messages = new ArrayList<>(2);
		
		if (selectedAppointments == 0) {
			if (Type.finding != topic.getType()) {
				if (freeAppointments != null) {
					if (freeAppointments == 1) {
						messages.add(translate("appointments.free.one"));
					} else if (freeAppointments > 1) {
						messages.add(translate("appointments.free", new String[] { freeAppointments.toString() }));
					}
				}
			}
			
			if (freeAppointments != null && freeAppointments.longValue() == 0) {
				messages.add(translate("appointments.free.no"));
			} else if (topic.isMultiParticipation()) {
				messages.add(translate("appointments.select.multi.message"));
			} else {
				messages.add(translate("appointments.select.one.message"));
			}
		} 
		
		if (selectedAppointments > 0) {
			if (Type.finding == topic.getType()) {
				if (status == null) {
					messages.add(translate("appointments.selected.not.confirmed"));
				}
			} else {
				if (topic.isMultiParticipation()) {
					if (selectedAppointments > 1) {
						messages.add(translate("appointments.selected", new String[] { String.valueOf(selectedAppointments) }));
					}
				}
			}
		}
		
		String message = messages.isEmpty()? null: messages.stream().collect(Collectors.joining("<br>"));
		wrapper.setMessage(message);
	}
	
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if (source == topicRunCtrl) {
			if (event == Event.DONE_EVENT) {
				refresh();
			}
			stackPanel.popUpToRootController(ureq);
			cleanUp();
		} else if (source == mailCtrl) {
			cmc.deactivate();
			cleanUp();
		} else if (source == cmc) {
			cleanUp();
		}
		super.event(ureq, source, event);
	}

	private void cleanUp() {
		removeAsListenerAndDispose(topicRunCtrl);
		removeAsListenerAndDispose(mailCtrl);
		removeAsListenerAndDispose(cmc);
		topicRunCtrl = null;
		mailCtrl = null;
		cmc = null;
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if (source instanceof Link) {
			Link link = (Link)source;
			String cmd = link.getCommand();
			if (CMD_OPEN.equals(cmd)) {
				Topic topic = (Topic)link.getUserObject();
				doOpenTopic(ureq, topic);
			} else if(CMD_EMAIL.equals(CMD_EMAIL)) {
				TopicWrapper wrapper = (TopicWrapper)link.getUserObject();
				doOrganizerEmail(ureq, wrapper.getTopic(), wrapper.getOrganizers());
			} 
		}
	}

	private void doOpenTopic(UserRequest ureq, Topic topic) {
		removeAsListenerAndDispose(topicRunCtrl);
		
		topicRunCtrl = new AppointmentListSelectionController(ureq, getWindowControl(), topic, secCallback);
		listenTo(topicRunCtrl);
		
		String title = topic.getTitle();
		String panelTitle = title.length() > 50? title.substring(0, 50) + "...": title;;
		stackPanel.pushController(panelTitle, topicRunCtrl);
	}

	private void doOrganizerEmail(UserRequest ureq, Topic topic, Collection<Organizer> organizers) {
		mailCtrl = new OrganizerMailController(ureq, getWindowControl(), topic, organizers);
		listenTo(mailCtrl);

		cmc = new CloseableModalController(getWindowControl(), translate("close"), mailCtrl.getInitialComponent(), true,
				translate("email.title"));
		listenTo(cmc);
		cmc.activate();
	}

	@Override
	protected void doDispose() {
		//
	}
	
	public static final class TopicWrapper {

		private final Topic topic;
		private Collection<Organizer> organizers;
		private List<String> organizerNames;
		private String emailLinkName;
		private List<String> participants;
		private Boolean future;
		private String dayName;
		private String date;
		private String date2;
		private String time;
		private String location;
		private String details;
		private Appointment.Status status;
		private String translatedStatus;
		private String statusCSS;
		private String message;
		private Long freeAppointments;
		private Integer selectedAppointments;
		private String openLinkName;

		public TopicWrapper(Topic topic) {
			this.topic = topic;
		}

		public Topic getTopic() {
			return topic;
		}
		
		public String getTitle() {
			return topic.getTitle();
		}
		
		public String getDescription() {
			return topic.getDescription();
		}
		
		public Collection<Organizer> getOrganizers() {
			return organizers;
		}

		public void setOrganizers(Collection<Organizer> organizers) {
			this.organizers = organizers;
		}

		public List<String> getOrganizerNames() {
			return organizerNames;
		}
		
		public void setOrganizerNames(List<String> organizerNames) {
			this.organizerNames = organizerNames;
		}

		public String getEmailLinkName() {
			return emailLinkName;
		}

		public void setEmailLinkName(String emailLinkName) {
			this.emailLinkName = emailLinkName;
		}

		public List<String> getParticipants() {
			return participants;
		}

		public void setParticipants(List<String> participants) {
			this.participants = participants;
		}

		public Boolean getFuture() {
			return future;
		}

		public void setFuture(Boolean future) {
			this.future = future;
		}
		
		public String getDayName() {
			return dayName;
		}

		public void setDayName(String dayName) {
			this.dayName = dayName;
		}

		public String getDate() {
			return date;
		}

		public void setDate(String date) {
			this.date = date;
		}

		public String getDate2() {
			return date2;
		}

		public void setDate2(String date2) {
			this.date2 = date2;
		}

		public String getTime() {
			return time;
		}

		public void setTime(String time) {
			this.time = time;
		}

		public String getLocation() {
			return location;
		}

		public void setLocation(String location) {
			this.location = location;
		}

		public String getDetails() {
			return details;
		}

		public void setDetails(String details) {
			this.details = details;
		}

		public Appointment.Status getStatus() {
			return status;
		}

		public void setStatus(Appointment.Status status) {
			this.status = status;
		}

		public String getTranslatedStatus() {
			return translatedStatus;
		}

		public void setTranslatedStatus(String translatedStatus) {
			this.translatedStatus = translatedStatus;
		}

		public String getStatusCSS() {
			return statusCSS;
		}

		public void setStatusCSS(String statusCSS) {
			this.statusCSS = statusCSS;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public Long getFreeAppointments() {
			return freeAppointments;
		}

		public void setFreeAppointments(Long freeAppointments) {
			this.freeAppointments = freeAppointments;
		}

		public Integer getSelectedAppointments() {
			return selectedAppointments;
		}

		public void setSelectedAppointments(Integer selectedAppointments) {
			this.selectedAppointments = selectedAppointments;
		}

		public String getOpenLinkName() {
			return openLinkName;
		}

		public void setOpenLinkName(String openLinkName) {
			this.openLinkName = openLinkName;
		}
		
	}

}
