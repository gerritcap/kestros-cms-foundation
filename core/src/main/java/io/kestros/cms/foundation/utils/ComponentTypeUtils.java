/*
 *      Copyright (C) 2020  Kestros, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.kestros.cms.foundation.utils;

import static io.kestros.commons.structuredslingmodels.utils.SlingModelUtils.getAllDescendantsOfType;
import static io.kestros.commons.structuredslingmodels.utils.SlingModelUtils.getResourceAsBaseResource;

import io.kestros.cms.foundation.componenttypes.ComponentType;
import io.kestros.cms.foundation.componenttypes.ComponentTypeGroup;
import io.kestros.cms.foundation.componenttypes.HtmlFile;
import io.kestros.cms.foundation.componenttypes.frameworkview.ComponentUiFrameworkView;
import io.kestros.cms.foundation.design.htltemplate.usage.HtlTemplateUsage;
import io.kestros.cms.foundation.exceptions.InvalidScriptException;
import io.kestros.commons.structuredslingmodels.BaseResource;
import io.kestros.commons.structuredslingmodels.exceptions.ResourceNotFoundException;
import io.kestros.commons.structuredslingmodels.utils.FileModelUtils;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for retrieving ComponentTypes and ComponentTypeGroups.
 */
public class ComponentTypeUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ComponentTypeUtils.class);
  public static final String PATH_LIBS_KESTROS_COMMONS = "/libs/kestros/commons";
  public static final String PATH_LIBS = "/libs";
  public static final String PATH_APPS = "/apps";

  private ComponentTypeUtils() {
  }

  /**
   * Retrieve all ComponentTypeGroups from components within the specified paths.
   *
   * @param includeApps include ComponentTypes from /apps.
   * @param includeLibs include ComponentTypes from /libs.
   * @param includeLibsCommons include ComponentTypes from /libs/kestros/commons.
   * @param resourceResolver ResourceResolver.
   * @return All ComponentTypeGroups from components within the specified paths.
   */
  @Nonnull
  public static List<ComponentTypeGroup> getAllComponentTypeGroups(final boolean includeApps,
      final boolean includeLibs, final boolean includeLibsCommons,
      @Nonnull final ResourceResolver resourceResolver) {
    return getComponentTypeGroupsFromComponentTypeList(
        getAllComponentTypes(includeApps, includeLibs, includeLibsCommons, resourceResolver));
  }

  /**
   * Builds a list of ComponentTypeGroups from a List of ComponentTypes.
   *
   * @param componentTypes ComponentType list to build ComponentTypeGroups from.
   * @param <T> extends ComponentType.
   * @return List of ComponentTypeGroups from a List of ComponentTypes.
   */
  @Nonnull
  public static <T extends ComponentType> List<ComponentTypeGroup>
      getComponentTypeGroupsFromComponentTypeList(
      @Nonnull final List<T> componentTypes) {
    final List<ComponentTypeGroup> componentTypeGroups = new ArrayList<>();

    final Map<String, ComponentTypeGroup> componentGroupsMap = new HashMap<>();

    final ComponentTypeGroup unknownGroup = new ComponentTypeGroup();

    for (final ComponentType componentType : componentTypes) {
      if (StringUtils.isNotEmpty(componentType.getComponentGroup())) {

        final String groupName = componentType.getComponentGroup();

        if (!componentGroupsMap.containsKey(componentType.getComponentGroup())) {
          final ComponentTypeGroup componentTypeGroup = new ComponentTypeGroup();
          componentTypeGroup.setTitle(groupName);
          componentTypeGroup.addComponentType(componentType);
          componentGroupsMap.put(groupName, componentTypeGroup);
        } else {
          componentGroupsMap.get(groupName).addComponentType(componentType);
        }
      } else {
        unknownGroup.addComponentType(componentType);
      }
    }
    for (final Entry group : componentGroupsMap.entrySet()) {
      componentTypeGroups.add(componentGroupsMap.get(group.getKey()));
    }
    if (!unknownGroup.getComponentTypes().isEmpty()) {
      componentTypeGroups.add(unknownGroup);
    }

    componentTypeGroups.sort(new ComponentTypeGroupSorter());
    return componentTypeGroups;
  }

  /**
   * Builds ComponentTypeGroups from a specified list of ComponentTypes, and filters them based on
   * allowed/excluded componentType paths and group names.
   *
   * @param componentTypeList List of ComponentTypes to build a ComponentTypeGroups from.
   * @param allowedComponentTypePaths ComponentType paths to allow.  All other groups are
   *     excluded unless this is null or an empty list.
   * @param excludedComponentTypePaths ComponentType paths to exclude.
   * @param allowedGroupNames Group names to allow.  All other groups are excluded unless this
   *     is null or an empty list.
   * @param excludedGroupNames Group names to exclude.
   * @return List of ComponentTypeGroups from a list of ComponentTypes.
   */
  @Nonnull
  public static List<ComponentTypeGroup> getComponentTypeGroups(
      @Nonnull final List<ComponentType> componentTypeList,
      @Nullable final List<String> allowedComponentTypePaths,
      @Nullable final List<String> excludedComponentTypePaths,
      @Nullable final List<String> allowedGroupNames,
      @Nullable final List<String> excludedGroupNames) {
    return getComponentTypeGroups(componentTypeList, allowedComponentTypePaths,
        excludedComponentTypePaths, allowedGroupNames, excludedGroupNames, false);
  }

  /**
   * Builds a list of ComponentTypeGroups from a list of ComponentTypes.
   *
   * @param componentTypeList List of ComponentTypes to build a ComponentTypeGroups from.
   * @param allowedComponentTypePaths ComponentType paths to allow.  All other groups are
   *     excluded unless this is null or an empty list.
   * @param excludedComponentTypePaths ComponentType paths to exclude.
   * @param allowedGroupNames Group names to allow.  All other groups are excluded unless this
   *     is null or an empty list.
   * @param excludedGroupNames Group names to exclude.
   * @param showHidden Whether to include componentTypes with .hidden as the componentGroup.
   * @return List of ComponentTypeGroups from a list of ComponentTypes.
   */
  @Nonnull
  public static List<ComponentTypeGroup> getComponentTypeGroups(
      @Nonnull final List<ComponentType> componentTypeList,
      @Nullable final List<String> allowedComponentTypePaths,
      @Nullable final List<String> excludedComponentTypePaths,
      @Nullable final List<String> allowedGroupNames,
      @Nullable final List<String> excludedGroupNames, final boolean showHidden) {
    List<ComponentTypeGroup> componentTypeGroups = new ArrayList<>();
    if (CollectionUtils.isEmpty(allowedComponentTypePaths) && CollectionUtils.isEmpty(
        allowedGroupNames) && CollectionUtils.isEmpty(excludedGroupNames)) {
      componentTypeGroups = getComponentTypeGroupsFromComponentTypeList(componentTypeList);
    }

    for (final ComponentTypeGroup componentTypeGroup : getComponentTypeGroupsFromComponentTypeList(
        componentTypeList)) {
      if (allowedGroupNames != null && allowedGroupNames.contains(componentTypeGroup.getTitle())) {
        componentTypeGroups.add(componentTypeGroup);
      } else if (excludedGroupNames != null && !excludedGroupNames.contains(
          componentTypeGroup.getTitle()) || (!showHidden && componentTypeGroup.getTitle().equals(
          ".hidden"))) {
        if (allowedGroupNames == null) {
          componentTypeGroups.add(componentTypeGroup);
        }
      } else {
        for (final ComponentType componentType : componentTypeGroup.getComponentTypes()) {
          if (allowedComponentTypePaths != null && allowedComponentTypePaths.contains(
              componentType.getPath())) {
            boolean belongsToExistingGroup = false;
            for (final ComponentTypeGroup group : componentTypeGroups) {
              if (group.getTitle().equals(componentType.getComponentGroup())
                  || group.getTitle().equals("No Group") && StringUtils.isEmpty(
                  componentType.getComponentGroup())) {
                belongsToExistingGroup = true;
                group.addComponentType(componentType);
                break;
              }
            }
            if (!belongsToExistingGroup) {
              final ComponentTypeGroup adhocGroup = new ComponentTypeGroup();
              if (StringUtils.isNotEmpty(componentType.getComponentGroup())) {
                adhocGroup.setTitle(componentType.getComponentGroup());
              }
              adhocGroup.addComponentType(componentType);
              componentTypeGroups.add(adhocGroup);
            }
          }
        }
      }
    }

    if (CollectionUtils.isNotEmpty(excludedComponentTypePaths)) {
      for (final ComponentTypeGroup group : componentTypeGroups) {
        for (final String excludedComponentTypePath : excludedComponentTypePaths) {
          group.removeComponentType(excludedComponentTypePath);
        }
      }
    }

    componentTypeGroups.sort(new ComponentTypeGroupSorter());
    return componentTypeGroups;
  }

  /**
   * Retrieves all ComponentTypes within /apps and /libs, as specified.
   *
   * @param includeApps Include ComponentTypes from /apps.
   * @param includeLibs Include ComponentTypes from /libs.
   * @param includeLibsCommons Include ComponentTypes from /libs/kestros/commons.
   * @param resourceResolver ResourceResolver used to retrieve ComponentType resources.
   * @return All ComponentTypes within /apps and /libs, as specified.
   */
  @Nonnull
  public static List<ComponentType> getAllComponentTypes(final boolean includeApps,
      final boolean includeLibs, final boolean includeLibsCommons,
      final ResourceResolver resourceResolver) {
    final List<ComponentType> componentTypes = new ArrayList<>();
    if (includeLibs) {
      try {
        componentTypes.addAll(
            getAllDescendantComponentTypes(getLibsRootResource(resourceResolver)));
      } catch (final ResourceNotFoundException e) {
        LOG.warn("Unable retrieve /libs ComponentTypes. {}", e.getMessage());
      }
    } else if (includeLibsCommons) {
      try {
        componentTypes.addAll(
            getAllDescendantComponentTypes(getLibsKestrosCommonsRootResource(resourceResolver)));
      } catch (final ResourceNotFoundException e) {
        LOG.warn("Unable retrieve /libs/kestros/commons ComponentTypes. {}", e.getMessage());
      }
    }
    if (includeApps) {
      try {
        List<ComponentType> appsComponentTypes = new ArrayList<>();
        appsComponentTypes.addAll(
            getAllDescendantComponentTypes(getAppsRootResource(resourceResolver)));
        if (includeLibs || includeLibsCommons) {
          for (ComponentType appsComponentType : appsComponentTypes) {
            String libsPath = appsComponentType.getPath().replace("/apps/", "/libs/");
            boolean componentTypeExists = componentTypes.stream().filter(
                o -> o.getPath().equals(libsPath)).findFirst().isPresent();
            LOG.debug("Excluding {} from all componentTypes list. ComponentType lives under /libs.",
                appsComponentType.getPath());
            if (!componentTypeExists) {
              componentTypes.add(appsComponentType);
            }
          }
        } else {
          componentTypes.addAll(appsComponentTypes);
        }
      } catch (final ResourceNotFoundException e) {
        LOG.warn("Unable retrieve /apps ComponentTypes. {}", e.getMessage());
      }

    }
    return componentTypes;
  }

  /**
   * Retrieves all ComponentTypes that are descendants of the specified resource.
   *
   * @param rootResource Resource to retrieve  descendant ComponentTypes from.
   * @return All ComponentTypes that are descendants of the specified resource.
   */
  @Nonnull
  public static List<ComponentType> getAllDescendantComponentTypes(
      final BaseResource rootResource) {
    return getAllDescendantsOfType(rootResource, ComponentType.class);
  }

  /**
   * Retrieves /apps root Resource.
   *
   * @param resourceResolver ResourceResolver.
   * @return Apps root Resource.
   * @throws ResourceNotFoundException Resource not found.
   */
  @Nonnull
  public static BaseResource getAppsRootResource(final ResourceResolver resourceResolver)
      throws ResourceNotFoundException {
    return getResourceAsBaseResource(PATH_APPS, resourceResolver);
  }

  /**
   * Retrieves /libs root Resource.
   *
   * @param resourceResolver ResourceResolver.
   * @return Libs root Resource.
   * @throws ResourceNotFoundException Resource not found.
   */
  @Nonnull
  public static BaseResource getLibsRootResource(final ResourceResolver resourceResolver)
      throws ResourceNotFoundException {
    return getResourceAsBaseResource(PATH_LIBS, resourceResolver);
  }

  /**
   * Retrieves Kestros commons root Resource.
   *
   * @param resourceResolver ResourceResolver.
   * @return Kestros commons root Resource.
   * @throws ResourceNotFoundException Resource not found.
   */
  @Nonnull
  public static BaseResource getLibsKestrosCommonsRootResource(
      final ResourceResolver resourceResolver) throws ResourceNotFoundException {
    return getResourceAsBaseResource(PATH_LIBS_KESTROS_COMMONS, resourceResolver);
  }

  /**
   * Groups ComponentTypes within a ComponentTypeGroup alphabetically.
   */
  private static class ComponentTypeGroupSorter
      implements Comparator<ComponentTypeGroup>, Serializable {

    private static final long serialVersionUID = -365044729256904870L;

    @Override
    public int compare(final ComponentTypeGroup o1, final ComponentTypeGroup o2) {
      return o1.getTitle().compareToIgnoreCase(o2.getTitle());
    }
  }

  /**
   * The name of all templates a specified ComponentUiFrameworkView attempts to call.
   *
   * @param componentUiFrameworkView View to retrieve called template names from.
   * @return The name of all templates a specified ComponentUiFrameworkView attempts to call.
   * @throws InvalidScriptException Failed to find a valid content.html script.
   * @throws IOException Failed to read view's content.html script.
   */
  @Nonnull
  public static List<String> getTemplateNamesAComponentViewAttemptsToCall(
      @Nonnull ComponentUiFrameworkView componentUiFrameworkView)
      throws InvalidScriptException, IOException {
    List<String> templateNameList = new ArrayList<>();
    for (HtmlFile htlScriptFile : FileModelUtils.getChildrenOfFileType(componentUiFrameworkView,
        HtmlFile.class)) {

      final Document contentScriptDocument = Jsoup.parse(htlScriptFile.getFileContent());
      contentScriptDocument.outputSettings().outline(true);
      contentScriptDocument.outputSettings().prettyPrint(false);

      for (Element element : contentScriptDocument.body().getElementsByAttributeStarting(
          "data-sly-call")) {
        if (element.hasAttr("data-sly-call")) {
          String templateName = element.attr("data-sly-call").split("@")[0];
          templateName = templateName.split("templates.")[1];
          templateName = templateName.replaceAll(" ", "");
          templateNameList.add(templateName);
        }
      }
    }

    return templateNameList;
  }

  /**
   * All template usages within a specified ComponentUiFrameworkView.
   *
   * @param componentUiFrameworkView view to find templates usages from.
   * @return All template usages within a specified ComponentUiFrameworkView.
   * @throws InvalidScriptException Failed to find a valid content.html script.
   * @throws IOException Failed to read view's content.html script.
   */
  public static List<HtlTemplateUsage> getHtlTemplateUsageList(
      @Nonnull ComponentUiFrameworkView componentUiFrameworkView)
      throws InvalidScriptException, IOException {
    List<HtlTemplateUsage> templateNameList = new ArrayList<>();
    for (HtmlFile htlScriptFile : FileModelUtils.getChildrenOfFileType(componentUiFrameworkView,
        HtmlFile.class)) {

      if (htlScriptFile != null) {
        final Document contentScriptDocument = Jsoup.parse(htlScriptFile.getFileContent());
        contentScriptDocument.outputSettings().outline(true);
        contentScriptDocument.outputSettings().prettyPrint(false);

        for (Element element : contentScriptDocument.body().getElementsByAttributeStarting(
            "data-sly-call")) {
          HtlTemplateUsage templateUsage = new HtlTemplateUsage(element, componentUiFrameworkView);
          if (templateUsage.getName() != null) {
            templateNameList.add(templateUsage);
          }
        }
      }
    }

    return templateNameList;
  }

}
