/*
 * Copyright 2010, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.zanata.annotation.CachedMethodResult;
import org.zanata.common.ActivityType;
import org.zanata.common.EntityStatus;
import org.zanata.dao.DocumentDAO;
import org.zanata.dao.ProjectDAO;
import org.zanata.dao.ProjectIterationDAO;
import org.zanata.model.Activity;
import org.zanata.model.HAccount;
import org.zanata.model.HDocument;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HTextFlowTarget;
import org.zanata.model.type.EntityType;
import org.zanata.security.ZanataIdentity;
import org.zanata.service.ActivityService;
import org.zanata.service.GravatarService;
import org.zanata.util.DateUtil;
import org.zanata.util.UrlUtil;

@Name("dashboardAction")
@Scope(ScopeType.PAGE)
public class DashboardAction implements Serializable
{
   private static final long serialVersionUID = 1L;

   @Logger
   private Log log;

   @In
   private GravatarService gravatarServiceImpl;

   @In
   private ActivityService activityServiceImpl;

   @In
   private ProjectIterationDAO projectIterationDAO;

   @In
   private ProjectDAO projectDAO;

   @In
   private DocumentDAO documentDAO;
   
   @In
   private UrlUtil urlUtil;

   @In(required = false, value = JpaIdentityStore.AUTHENTICATED_USER)
   private HAccount authenticatedAccount;

   @In
   private ZanataIdentity identity;

   private final int USER_IMAGE_SIZE = 115;
   private final int ACTIVITIES_COUNT_PER_PAGE = 10;
   private final int MAX_CONTENT_LENGTH = 50;
   private final String WRAPPED_POSTFIX = "...";
   private int activityPageIndex = 0;
   
   private final Date now = new Date();

   private final Comparator<HProject> projectCreationDateComparator = new Comparator<HProject>()
   {
      @Override
      public int compare(HProject o1, HProject o2)
      {
         return o2.getCreationDate().after(o1.getCreationDate()) ? 1 : -1;
      }
   };

   public String getUserImageUrl()
   {
      return gravatarServiceImpl.getUserImageUrl(USER_IMAGE_SIZE);
   }

   public String getUsername()
   {
      return authenticatedAccount.getPerson().getAccount().getUsername();
   }

   public String getUserFullName()
   {
      return authenticatedAccount.getPerson().getName();
   }

   @CachedMethodResult
   public int getUserMaintainedProjectsSize()
   {
      return authenticatedAccount.getPerson().getMaintainerProjects().size();
   }

   @CachedMethodResult
   public List<HProject> getUserMaintainedProjects()
   {
      List<HProject> sortedList = new ArrayList<HProject>();

      if (checkViewObsolete())
      {
         sortedList.addAll(authenticatedAccount.getPerson().getMaintainerProjects());
      }
      else
      {
         for (HProject project : authenticatedAccount.getPerson().getMaintainerProjects())
         {
            if (project.getStatus() != EntityStatus.OBSOLETE)
            {
               sortedList.add(project);
            }
         }
      }
      Collections.sort(sortedList, projectCreationDateComparator);

      return sortedList;
   }

   @CachedMethodResult
   public boolean checkViewObsolete()
   {
      return identity != null && identity.hasPermission("HProject", "view-obsolete");
   }

   @CachedMethodResult
   public List<HProjectIteration> getProjectVersions(Long projectId)
   {
      List<HProjectIteration> result = new ArrayList<HProjectIteration>();
      if (checkViewObsolete())
      {
         result.addAll(projectIterationDAO.searchByProjectId(projectId));
      }
      else
      {
         result.addAll(projectIterationDAO.searchByProjectIdExcludeObsolete(projectId));
      }
      return result;
   }

   @CachedMethodResult
   public boolean isUserMaintainer(Long projectId)
   {
      HProject project = getProject(projectId);
      return authenticatedAccount.getPerson().isMaintainer(project);
   }

   @CachedMethodResult
   public int getDocumentCount(Long versionId)
   {
      HProjectIteration version = getProjectVersion(versionId);
      return version.getDocuments().size();
   }

   public String getFormattedDate(Date date)
   {
      return DateUtil.formatShortDate(date);
   }

   @CachedMethodResult
   private HProjectIteration getProjectVersion(Long versionId)
   {
      return projectIterationDAO.findById(versionId, false);
   }

   @CachedMethodResult
   private HProject getProject(Long projectId)
   {
      return projectDAO.findById(projectId, false);
   }

   public List<Activity> getActivities()
   {
      return activityServiceImpl.findLatestActivities(authenticatedAccount.getPerson().getId(), activityPageIndex, ACTIVITIES_COUNT_PER_PAGE);
   }

   @CachedMethodResult
   public String getTime(Activity activity)
   {
      Date then = DateUtils.addMilliseconds(activity.getApproxTime(), (int) activity.getEndOffsetMillis());

      return DateUtil.getReadableTime(now, then);
   }

   @CachedMethodResult
   public String getProjectName(Activity activity)
   {
      Object context = getEntity(activity.getContextType(), activity.getContextId());

      if (isTranslationUpdateActivity(activity.getActionType())
            || activity.getActionType() == ActivityType.UPLOAD_SOURCE_DOCUMENT
            || activity.getActionType() == ActivityType.UPLOAD_TRANSLATION_DOCUMENT)
      {
         HProjectIteration version = (HProjectIteration) context;
         return version.getProject().getName();
      }
      return "";
   }

   @CachedMethodResult
   public String getProjectUrl(Activity activity)
   {
      Object context = getEntity(activity.getContextType(), activity.getContextId());

      if (isTranslationUpdateActivity(activity.getActionType())
            || activity.getActionType() == ActivityType.UPLOAD_SOURCE_DOCUMENT
            || activity.getActionType() == ActivityType.UPLOAD_TRANSLATION_DOCUMENT)
      {
         HProjectIteration version = (HProjectIteration) context;
         return urlUtil.projectUrl(version.getProject().getSlug());
      }
      return "";
   }

   

   @CachedMethodResult
   public String getTrimmedContent(Activity activity)
   {
      String wrappedContent = "";
      Object lastTarget = getEntity(activity.getLastTargetType(), activity.getLastTargetId());

      if (isTranslationUpdateActivity(activity.getActionType()))
      {
         HTextFlowTarget tft = (HTextFlowTarget) lastTarget;
         wrappedContent = tft.getContents().get(0);
      }
      else if(activity.getActionType() == ActivityType.UPLOAD_SOURCE_DOCUMENT)
      {
         //not supported for upload source action
      }
      else if (activity.getActionType() == ActivityType.UPLOAD_TRANSLATION_DOCUMENT)
      {
         HDocument document = (HDocument) lastTarget;
         HTextFlowTarget tft = documentDAO.getLastTranslatedTarget(document.getId());
         wrappedContent = tft.getContents().get(0);
      }

      return trimString(wrappedContent);
   }

   @CachedMethodResult
   public String getEditorUrl(Activity activity)
   {
      String url = "";
      Object context = getEntity(activity.getContextType(), activity.getContextId());
      Object lastTarget = getEntity(activity.getLastTargetType(), activity.getLastTargetId());

      if (isTranslationUpdateActivity(activity.getActionType()))
      {
         HProjectIteration version = (HProjectIteration) context;
         HTextFlowTarget tft = (HTextFlowTarget) lastTarget;

         url = urlUtil.editorTransUnitUrl(version.getProject().getSlug(), version.getSlug(), tft.getLocaleId(),
               tft.getTextFlow().getLocale(), tft.getTextFlow().getDocument().getDocId(), tft.getTextFlow().getId());
      }
      else if(activity.getActionType() == ActivityType.UPLOAD_SOURCE_DOCUMENT)
      {
         // not supported for upload source action
      }
      else if (activity.getActionType() == ActivityType.UPLOAD_TRANSLATION_DOCUMENT)
      {
         HProjectIteration version = (HProjectIteration) context;
         HDocument document = (HDocument) lastTarget;
         HTextFlowTarget tft = documentDAO.getLastTranslatedTarget(document.getId());

         url = urlUtil.editorTransUnitUrl(version.getProject().getSlug(), version.getSlug(), tft.getLocaleId(),
               document.getSourceLocaleId(), tft.getTextFlow().getDocument().getDocId(), tft.getTextFlow().getId());
      }
      

      return url;
   }

   @CachedMethodResult
   public String getDocumentUrl(Activity activity)
   {
      String url = "";
      Object context = getEntity(activity.getContextType(), activity.getContextId());
      Object lastTarget = getEntity(activity.getLastTargetType(), activity.getLastTargetId());

      if (isTranslationUpdateActivity(activity.getActionType()))
      {
         HProjectIteration version = (HProjectIteration) context;
         HTextFlowTarget tft = (HTextFlowTarget) lastTarget;

         url = urlUtil.editorDocumentUrl(version.getProject().getSlug(), version.getSlug(), tft.getLocaleId(),
               tft.getTextFlow().getLocale(), tft.getTextFlow().getDocument().getDocId());
      }
      else if(activity.getActionType() == ActivityType.UPLOAD_SOURCE_DOCUMENT)
      {
         HProjectIteration version = (HProjectIteration) context;
         url = urlUtil.sourceFilesUrl(version.getProject().getSlug(), version.getSlug());
      }
      else if (activity.getActionType() == ActivityType.UPLOAD_TRANSLATION_DOCUMENT)
      {
         HProjectIteration version = (HProjectIteration) context;
         HDocument document = (HDocument) lastTarget;
         HTextFlowTarget tft = documentDAO.getLastTranslatedTarget(document.getId());

         url = urlUtil.editorDocumentUrl(version.getProject().getSlug(), version.getSlug(), tft.getLocaleId(),
               document.getSourceLocaleId(), tft.getTextFlow().getDocument().getDocId());
      }
      return url;
   }

   @CachedMethodResult
   public String getDocumentName(Activity activity)
   {
      Object lastTarget = getEntity(activity.getLastTargetType(), activity.getLastTargetId());
      String docName = "";

      if (isTranslationUpdateActivity(activity.getActionType()))
      {
         HTextFlowTarget tft = (HTextFlowTarget) lastTarget;
         docName = tft.getTextFlow().getDocument().getName();
      }
      else if (activity.getActionType() == ActivityType.UPLOAD_SOURCE_DOCUMENT
            || activity.getActionType() == ActivityType.UPLOAD_TRANSLATION_DOCUMENT)
      {
         HDocument document = (HDocument) lastTarget;
         docName = document.getName();
      }
      return docName;
   }

   @CachedMethodResult
   public String getVersionUrl(Activity activity)
   {
      Object context = getEntity(activity.getContextType(), activity.getContextId());
      String url = "";

      if (isTranslationUpdateActivity(activity.getActionType())
            || activity.getActionType() == ActivityType.UPLOAD_SOURCE_DOCUMENT
            || activity.getActionType() == ActivityType.UPLOAD_TRANSLATION_DOCUMENT)
      {
         HProjectIteration version = (HProjectIteration) context;
         url = urlUtil.versionUrl(version.getProject().getSlug(), version.getSlug());
      }

      return url;
   }

   @CachedMethodResult
   public String getVersionName(Activity activity)
   {
      Object context = getEntity(activity.getContextType(), activity.getContextId());
      String name = "";

      if (isTranslationUpdateActivity(activity.getActionType())
            || activity.getActionType() == ActivityType.UPLOAD_SOURCE_DOCUMENT
            || activity.getActionType() == ActivityType.UPLOAD_TRANSLATION_DOCUMENT)
      {
         HProjectIteration version = (HProjectIteration) context;
         name = version.getSlug();
      }
      return name;
   }

   @CachedMethodResult
   public String getDocumentListUrl(Activity activity)
   {
      Object context = getEntity(activity.getContextType(), activity.getContextId());
      Object lastTarget = getEntity(activity.getLastTargetType(), activity.getLastTargetId());
      String url = "";

      if (isTranslationUpdateActivity(activity.getActionType()))
      {
         HProjectIteration version = (HProjectIteration) context;
         HTextFlowTarget tft = (HTextFlowTarget) lastTarget;
         
         url = urlUtil.editorDocumentListUrl(version.getProject().getSlug(), version.getSlug(), tft.getLocaleId(), tft.getTextFlow().getLocale());
      }
      else if(activity.getActionType() == ActivityType.UPLOAD_SOURCE_DOCUMENT)
      {
         // not supported for upload source action
      }
      else if (activity.getActionType() == ActivityType.UPLOAD_TRANSLATION_DOCUMENT)
      {
         HProjectIteration version = (HProjectIteration) context;
         HDocument document = (HDocument) lastTarget;
         HTextFlowTarget tft = documentDAO.getLastTranslatedTarget(document.getId());
         
         url = urlUtil.editorDocumentListUrl(version.getProject().getSlug(), version.getSlug(), tft.getLocaleId(), tft.getTextFlow().getLocale());
      }
      return url;
   }

   @CachedMethodResult
   public String getLanguageName(Activity activity)
   {
      Object lastTarget = getEntity(activity.getLastTargetType(), activity.getLastTargetId());
      String name = "";

      if (isTranslationUpdateActivity(activity.getActionType()))
      {
         HTextFlowTarget tft = (HTextFlowTarget) lastTarget;
         name = tft.getLocaleId().getId();
      }
      else if(activity.getActionType() == ActivityType.UPLOAD_SOURCE_DOCUMENT)
      {
         // not supported for upload source action
      }
      else if (activity.getActionType() == ActivityType.UPLOAD_TRANSLATION_DOCUMENT)
      {
         HDocument document = (HDocument) lastTarget;
         HTextFlowTarget tft = documentDAO.getLastTranslatedTarget(document.getId());
         name = tft.getLocaleId().getId();
      }

      return name;
   }

   @CachedMethodResult
   private Object getEntity(EntityType contextType, long id)
   {
      return activityServiceImpl.getEntity(contextType, id);
   }

   private String trimString(String text)
   {
      if (StringUtils.length(text) > (MAX_CONTENT_LENGTH + StringUtils.length(WRAPPED_POSTFIX)))
      {
         text = StringUtils.substring(text, 0, MAX_CONTENT_LENGTH + StringUtils.length(WRAPPED_POSTFIX));
         text = text + WRAPPED_POSTFIX;
      }
      return text;
   }
   
   private boolean isTranslationUpdateActivity(ActivityType activityType)
   {
      return activityType == ActivityType.UPDATE_TRANSLATION
            || activityType == ActivityType.REVIEWED_TRANSLATION;
   }
}
