/*
 * Copyright (c) 2009-2011 Lockheed Martin Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eurekastreams.web.client.ui.common.widgets.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eurekastreams.server.action.request.profile.GetCurrentUserFollowingStatusRequest;
import org.eurekastreams.server.action.request.profile.SetFollowingStatusRequest;
import org.eurekastreams.server.domain.EntityType;
import org.eurekastreams.server.domain.Follower;
import org.eurekastreams.server.domain.Page;
import org.eurekastreams.server.search.modelview.DomainGroupModelView;
import org.eurekastreams.server.search.modelview.PersonModelView;
import org.eurekastreams.web.client.events.EventBus;
import org.eurekastreams.web.client.events.HistoryViewsChangedEvent;
import org.eurekastreams.web.client.events.Observer;
import org.eurekastreams.web.client.events.UpdatedHistoryParametersEvent;
import org.eurekastreams.web.client.events.data.GotGroupModelViewInformationResponseEvent;
import org.eurekastreams.web.client.events.data.GotPersonFollowerStatusResponseEvent;
import org.eurekastreams.web.client.events.data.GotPersonFollowersResponseEvent;
import org.eurekastreams.web.client.events.data.GotPersonFollowingResponseEvent;
import org.eurekastreams.web.client.events.data.GotPersonalInformationResponseEvent;
import org.eurekastreams.web.client.history.CreateUrlRequest;
import org.eurekastreams.web.client.model.BaseModel;
import org.eurekastreams.web.client.model.CurrentUserPersonFollowingStatusModel;
import org.eurekastreams.web.client.model.Deletable;
import org.eurekastreams.web.client.model.GroupMembersModel;
import org.eurekastreams.web.client.model.Insertable;
import org.eurekastreams.web.client.model.PersonFollowersModel;
import org.eurekastreams.web.client.ui.Session;
import org.eurekastreams.web.client.ui.common.animation.ExpandCollapseAnimation;
import org.eurekastreams.web.client.ui.common.avatar.AvatarWidget.Size;
import org.eurekastreams.web.client.ui.common.charts.StreamAnalyticsChart;
import org.eurekastreams.web.client.ui.common.pager.PagerComposite;
import org.eurekastreams.web.client.ui.common.stream.renderers.AvatarRenderer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * Post box.
 */
public class StreamDetailsComposite extends Composite
{
    /**
     * Binder for building UI.
     */
    private static LocalUiBinder binder = GWT.create(LocalUiBinder.class);

    /**
     * CSS resource.
     */
    interface StreamDetailsStyle extends CssResource
    {
        /**
         * Condensed Stream view.
         * 
         * @return Condensed Stream view.
         */
        String condensedStream();

        /**
         * Unfollow style.
         * 
         * @return Unfollow style.
         */
        String unFollowLink();
    }

    /**
     * CSS style.
     */
    @UiField
    StreamDetailsStyle style;

    /**
     * 
     * Binder for building UI.
     */
    interface LocalUiBinder extends UiBinder<Widget, StreamDetailsComposite>
    {
    }

    /**
     * Default constructor.
     */
    public StreamDetailsComposite()
    {
        initWidget(binder.createAndBindUi(this));
        buildPage();
    }

    /**
     * UI element for stream about panel.
     */
    @UiField
    HTMLPanel streamAbout;

    /**
     * UI element for stream details.
     */
    @UiField
    DivElement streamDetailsContainer;

    /**
     * UI element for chart.
     */
    @UiField
    HTMLPanel analyticsChartContainer;

    /**
     * UI element for about link.
     */
    @UiField
    Anchor aboutLink;

    /**
     * UI element for followers link.
     */
    @UiField
    Anchor followersLink;

    /**
     * UI element for following link.
     */
    @UiField
    Anchor followingLink;

    /**
     * UI element for toggling details.
     */
    @UiField
    Anchor toggleDetails;

    /**
     * UI element for follower count.
     */
    @UiField
    SpanElement followerCount;

    /**
     * UI element for following count.
     */
    @UiField
    SpanElement followingCount;

    /**
     * UI element for stream name.
     */
    @UiField
    SpanElement streamName;

    /**
     * UI element for stream meta info.
     */
    @UiField
    SpanElement streamMeta;

    /**
     * UI element for stream avatar.
     */
    @UiField
    HTMLPanel streamAvatar;

    /**
     * UI element for stream description.
     */
    @UiField
    DivElement streamDescription;

    /**
     * UI element for stream interests.
     */
    @UiField
    DivElement streamInterests;

    /**
     * UI element for follow link.
     */
    @UiField
    Label followLink;

    /**
     * UI element for stream hash tags.
     */
    @UiField
    DivElement streamHashtags;

    /**
     * UI element for stream followers.
     */
    @UiField
    PagerComposite streamFollowers;

    /**
     * UI element for stream following.
     */
    @UiField
    PagerComposite streamFollowing;

    /**
     * Current status.
     */
    private Follower.FollowerStatus status;

    /**
     * Expand animation duration.
     */
    private static final int EXPAND_ANIMATION_DURATION = 500;

    /**
     * Default stream details container size.
     */
    private static final int DEFAULT_STREAM_DETAILS_CONTAINER_SIZE = 330;

    /**
     * Avatar Renderer.
     */
    private AvatarRenderer avatarRenderer = new AvatarRenderer();

    /**
     * Expand/Collapse animation.
     */
    private ExpandCollapseAnimation detailsContainerAnimation;

    /**
     * Last following handler.
     */
    private HandlerRegistration lastHandler;

    /**
     * Model used to set following status.
     */
    private BaseModel followModel;

    /**
     * Build page.
     */
    private void buildPage()
    {
        final StreamDetailsComposite thisClass = this;

        detailsContainerAnimation = new ExpandCollapseAnimation(streamDetailsContainer,
                DEFAULT_STREAM_DETAILS_CONTAINER_SIZE, EXPAND_ANIMATION_DURATION);
        final StreamAnalyticsChart chart = new StreamAnalyticsChart();

        streamAvatar.add(avatarRenderer.render(0L, null, EntityType.PERSON, Size.Normal));
        analyticsChartContainer.add(chart);
        chart.update();

        streamFollowers.setVisible(false);
        streamFollowing.setVisible(false);

        EventBus.getInstance().addObserver(HistoryViewsChangedEvent.class, new Observer<HistoryViewsChangedEvent>()
        {
            public void update(final HistoryViewsChangedEvent event)
            {
                List<String> views = new ArrayList<String>(event.getViews());

                HashMap<String, String> params = new HashMap<String, String>();
                params.put("details", "about");
                aboutLink.setHref("#"
                        + Session.getInstance().generateUrl(new CreateUrlRequest(Page.ACTIVITY, views, params)));

                params.put("details", "followers");
                followersLink.setHref("#"
                        + Session.getInstance().generateUrl(new CreateUrlRequest(Page.ACTIVITY, views, params)));

                params.put("details", "following");
                followingLink.setHref("#"
                        + Session.getInstance().generateUrl(new CreateUrlRequest(Page.ACTIVITY, views, params)));
            }
        });

        EventBus.getInstance().addObserver(UpdatedHistoryParametersEvent.class,
                new Observer<UpdatedHistoryParametersEvent>()
                {
                    public void update(final UpdatedHistoryParametersEvent event)
                    {
                        if (event.getParameters().containsKey("details"))
                        {
                            streamFollowers.setVisible("followers".equals(event.getParameters().get("details")));
                            streamFollowing.setVisible("following".equals(event.getParameters().get("details")));
                            streamAbout.setVisible("about".equals(event.getParameters().get("details")));

                        }
                    }
                });

        EventBus.getInstance().addObserver(GotPersonalInformationResponseEvent.class,
                new Observer<GotPersonalInformationResponseEvent>()
                {
                    public void update(final GotPersonalInformationResponseEvent event)
                    {
                        PersonModelView person = event.getResponse();

                        updateFollowLink(person.getAccountId(), EntityType.PERSON);

                        streamName.setInnerText(person.getDisplayName());
                        streamMeta.setInnerText(person.getTitle());
                        streamAvatar.clear();
                        streamAvatar.add(avatarRenderer.render(person.getEntityId(), person.getAvatarId(),
                                EntityType.PERSON, Size.Normal));

                        followerCount.setInnerText(Integer.toString(person.getFollowersCount()));
                        followingCount.setInnerText(Integer.toString(person.getFollowingCount()));
                        streamDescription.setInnerText(person.getJobDescription());
                        String interestString = "";
                        for (String interest : person.getInterests())
                        {
                            interestString += "<a href='#" + interest + "'>" + interest + "</a>";
                        }
                        streamInterests.setInnerHTML(interestString);

                        streamHashtags.setInnerHTML("<a href='#something'>#something</a>");

                        // streamFollowers.clear();
                        // streamFollowing.clear();
                    }
                });

        EventBus.getInstance().addObserver(GotGroupModelViewInformationResponseEvent.class,
                new Observer<GotGroupModelViewInformationResponseEvent>()
                {
                    public void update(final GotGroupModelViewInformationResponseEvent event)
                    {
                        DomainGroupModelView group = event.getResponse();

                        updateFollowLink(group.getShortName(), EntityType.GROUP);

                        streamName.setInnerText(group.getName());
                        // streamMeta.setInnerText(group.get);
                        streamAvatar.clear();
                        streamAvatar.add(avatarRenderer.render(group.getEntityId(), group.getAvatarId(),
                                EntityType.GROUP, Size.Normal));

                        followerCount.setInnerText(Integer.toString(group.getFollowersCount()));
                        streamDescription.setInnerText(group.getDescription());
                        String interestString = "";
                        for (String interest : group.getCapabilities())
                        {
                            interestString += "<a href='#" + interest + "'>" + interest + "</a>";
                        }
                        streamInterests.setInnerHTML(interestString);
                        streamHashtags.setInnerHTML("<a href='#something'>#something</a>");

                        // streamFollowers.clear();
                        // streamFollowing.clear();
                    }
                });

        EventBus.getInstance().addObserver(GotPersonFollowersResponseEvent.class,
                new Observer<GotPersonFollowersResponseEvent>()
                {
                    public void update(final GotPersonFollowersResponseEvent event)
                    {
                        // streamFollowers.render(event.getResponse(), "No one is following this person");
                    }
                });

        EventBus.getInstance().addObserver(GotPersonFollowingResponseEvent.class,
                new Observer<GotPersonFollowingResponseEvent>()
                {
                    public void update(final GotPersonFollowingResponseEvent event)
                    {
                        // streamFollowing.render(event.getResponse(), "This person is not following anyone");
                    }
                });

        EventBus.getInstance().addObserver(HistoryViewsChangedEvent.class, new Observer<HistoryViewsChangedEvent>()
        {
            public void update(final HistoryViewsChangedEvent event)
            {
                List<String> views = new ArrayList<String>(event.getViews());

                if (views == null || views.size() == 0 || views.get(0).equals("following"))
                {
                    streamName.setInnerText("Following");
                    thisClass.addStyleName(style.condensedStream());
                }
                else if (views.get(0).equals("person") && views.size() >= 2)
                {
                    thisClass.removeStyleName(style.condensedStream());
                }
                else if (views.get(0).equals("group") && views.size() >= 2)
                {
                    thisClass.removeStyleName(style.condensedStream());
                }
                else if (views.get(0).equals("custom") && views.size() >= 3)
                {
                    streamName.setInnerText("Custom");
                    thisClass.addStyleName(style.condensedStream());
                }
                else if (views.get(0).equals("everyone"))
                {
                    streamName.setInnerText("Everyone");
                    thisClass.addStyleName(style.condensedStream());
                }
                else if (views.size() == 1)
                {
                    thisClass.addStyleName(style.condensedStream());
                }
            }
        }, true);

        toggleDetails.addClickHandler(new ClickHandler()
        {
            public void onClick(final ClickEvent event)
            {
                detailsContainerAnimation.toggle();
            }
        });
    }

    /**
     * Update the following element.
     * 
     * @param entityId
     *            the id of the entity.
     * @param type
     *            the type.
     */
    public void updateFollowLink(final String entityId, final EntityType type)
    {
        if (!entityId.equals(Session.getInstance().getCurrentPerson().getAccountId()))
        {
            followLink.setVisible(true);
            followModel = GroupMembersModel.getInstance();

            if (type.equals(EntityType.PERSON))
            {
                followModel = PersonFollowersModel.getInstance();
            }

            if (lastHandler != null)
            {
                lastHandler.removeHandler();
            }

            lastHandler = followLink.addClickHandler(new ClickHandler()
            {
                public void onClick(final ClickEvent event)
                {
                    SetFollowingStatusRequest request;

                    switch (status)
                    {
                    case FOLLOWING:
                        request = new SetFollowingStatusRequest(
                                Session.getInstance().getCurrentPerson().getAccountId(), entityId, type, false,
                                Follower.FollowerStatus.NOTFOLLOWING);
                        ((Deletable<SetFollowingStatusRequest>) followModel).delete(request);
                        onFollowerStatusChanged(Follower.FollowerStatus.NOTFOLLOWING);
                        break;
                    case NOTFOLLOWING:
                        request = new SetFollowingStatusRequest(
                                Session.getInstance().getCurrentPerson().getAccountId(), entityId, type, false,
                                Follower.FollowerStatus.FOLLOWING);
                        ((Insertable<SetFollowingStatusRequest>) followModel).insert(request);
                        onFollowerStatusChanged(Follower.FollowerStatus.FOLLOWING);
                        break;
                    default:
                        // do nothing.
                        break;
                    }
                }
            });

            Session.getInstance().getEventBus().addObserver(GotPersonFollowerStatusResponseEvent.class,
                    new Observer<GotPersonFollowerStatusResponseEvent>()
                    {
                        public void update(final GotPersonFollowerStatusResponseEvent event)
                        {
                            onFollowerStatusChanged(event.getResponse());
                        }
                    });

            CurrentUserPersonFollowingStatusModel.getInstance().fetch(
                    new GetCurrentUserFollowingStatusRequest(entityId, type), true);
        }
        else
        {
            followLink.setVisible(false);
        }
    }

    /**
     * When the following status changes.
     * 
     * @param inStatus
     *            status.
     */
    private void onFollowerStatusChanged(final Follower.FollowerStatus inStatus)
    {
        status = inStatus;

        switch (inStatus)
        {
        case FOLLOWING:
            followLink.addStyleName(style.unFollowLink());
            break;
        case NOTFOLLOWING:
            followLink.removeStyleName(style.unFollowLink());
            break;
        default:
            break;
        }
    }
}