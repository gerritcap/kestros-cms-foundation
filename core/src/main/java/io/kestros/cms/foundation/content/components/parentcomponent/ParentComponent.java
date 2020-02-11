package io.kestros.cms.foundation.content.components.parentcomponent;

import static io.kestros.cms.foundation.design.DesignConstants.NN_VARIATIONS;

import io.kestros.cms.foundation.componenttypes.frameworkview.ComponentUiFrameworkView;
import io.kestros.cms.foundation.componenttypes.variation.ComponentVariation;
import io.kestros.cms.foundation.content.BaseComponent;
import io.kestros.cms.foundation.design.theme.Theme;
import io.kestros.cms.foundation.design.uiframework.UiFramework;
import io.kestros.cms.foundation.exceptions.InvalidComponentTypeException;
import io.kestros.cms.foundation.exceptions.InvalidComponentUiFrameworkViewException;
import io.kestros.cms.foundation.exceptions.InvalidScriptException;
import io.kestros.cms.foundation.exceptions.InvalidThemeException;
import io.kestros.cms.foundation.exceptions.InvalidUiFrameworkException;
import io.kestros.cms.foundation.services.scriptprovider.ScriptProviderService;
import io.kestros.cms.foundation.services.themeprovider.ThemeProviderService;
import io.kestros.commons.structuredslingmodels.annotation.StructuredModel;
import io.kestros.commons.structuredslingmodels.exceptions.ModelAdaptionException;
import io.kestros.commons.structuredslingmodels.exceptions.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides logic for dynamic script resolution, component variations and HTML attributes to the
 * kestros-parent wrapper component.
 */
@StructuredModel(docPaths = {
    "/content/guide-articles/kestros-cms/foundation/extending-the-parent-component",
    "/content/guide-articles/kestros-cms/foundation/creating-new-component-types",
    "/content/guide-articles/kestros-cms/foundation/implementing-ui-framework-views",
    "/content/guide-articles/kestros-cms/foundation/creating-component-variations"})
@Model(adaptables = Resource.class,
       resourceType = "kestros/commons/components/kestros-parent")
@Exporter(name = "jackson",
          selector = "parent-component",
          extensions = "json")
public class ParentComponent extends BaseComponent {

  private static final Logger LOG = LoggerFactory.getLogger(ParentComponent.class);

  @OSGiService
  private ThemeProviderService themeProviderService;

  @OSGiService
  private ScriptProviderService scriptProviderService;

  private Theme theme;

  private UiFramework uiFramework;

  /**
   * HTML element ID to give to the component.
   *
   * @return HTML element ID to give to the component.
   */
  @Nonnull
  public String getId() {
    return getProperties().get("id", StringUtils.EMPTY);
  }

  /**
   * Additional CSS classes to add to the component.
   *
   * @return Additional CSS classes to add to the component.
   */
  @Nonnull
  public String getCssClass() {
    return getProperties().get("class", StringUtils.EMPTY);
  }

  /**
   * Retrieves the ComponentUiFramework the current Component will use to render.
   *
   * @return The ComponentUiFramework the current Component will use to render
   * @throws InvalidComponentUiFrameworkViewException ComponentUiFramework could not be found,
   *     or failed adaption.
   * @throws InvalidComponentTypeException ComponentType could not be found, or failed
   *     adaption.
   * @throws InvalidThemeException Theme could not be found, or failed adaption.
   * @throws InvalidUiFrameworkException UiFramework for the current component failed
   *     adaptation.
   * @throws ResourceNotFoundException UiFramework for the current component could not be
   *     found.
   */
  public ComponentUiFrameworkView getComponentUiFrameworkView()
      throws InvalidComponentUiFrameworkViewException, InvalidComponentTypeException,
             InvalidThemeException, ResourceNotFoundException, InvalidUiFrameworkException {
    LOG.trace("Getting Component UiFrameworkView.");
    try {
      final ComponentUiFrameworkView componentUiFrameworkView
          = getComponentType().getComponentUiFrameworkView(getUiFramework());
      LOG.trace("Retrieved Component UI FrameworkView.");
      return componentUiFrameworkView;
    } catch (final Exception exception) {
      LOG.debug("Unable to retrieve ComponentUiFrameworkView for {}. {}.", getPath(),
          exception.getMessage());
      throw exception;
    }
  }

  /**
   * Applied ComponentVariation names, to be read by the HTML class attribute.
   *
   * @return Applied ComponentVariation names.
   */
  @Nonnull
  public String getAppliedVariationsAsString() {
    LOG.trace("Getting Applied Variations as String.");
    final StringBuilder variationsStringBuffer = new StringBuilder();
    for (final ComponentVariation variation : getAppliedVariations()) {
      variationsStringBuffer.append(variation.getName());
      variationsStringBuffer.append(" ");
    }
    LOG.trace("Retrieved AppliedVariations as string.");
    return variationsStringBuffer.toString();
  }

  /**
   * List of ComponentVariations applied to the current Component.
   *
   * @return List of ComponentVariations applied to the current Component.
   */
  @Nonnull
  public List<ComponentVariation> getAppliedVariations() {
    final List<ComponentVariation> appliedVariations = new ArrayList<>();
    final List<String> appliedVariationNames = Arrays.asList(
        getProperties().get(NN_VARIATIONS, new String[]{}));

    if (!appliedVariationNames.isEmpty()) {
      try {
        final ComponentUiFrameworkView uiFrameworkView = getComponentUiFrameworkView();
        for (final String appliedVariation : appliedVariationNames) {
          for (final ComponentVariation variation : uiFrameworkView.getVariations()) {
            if (variation.getPath().equals(appliedVariation) || variation.getName().equals(
                appliedVariation)) {
              appliedVariations.add(variation);
            }
          }
        }
      } catch (final ModelAdaptionException exception) {
        LOG.warn("Unable to variations list for {}. {}", getPath(), exception.getMessage());
      }
    }

    return appliedVariations;
  }

  /**
   * The current {@link Theme} for the current Page/Component.
   *
   * @return The current Theme.
   * @throws InvalidThemeException Theme could not be adapted to Theme.
   * @throws ResourceNotFoundException Component's expected Theme resource was missing.
   */
  @Nullable
  public Theme getTheme() throws ResourceNotFoundException, InvalidThemeException {
    if (theme == null) {
      theme = themeProviderService.getThemeForComponent(this);
    }
    return theme;
  }

  protected void setTheme(final Theme theme) {
    this.theme = theme;
  }

  /**
   * The path to the content.html script.
   *
   * @return The path to the content.html script.
   * @throws InvalidScriptException The script was not found, or could not be adapt to
   *     HtmlFile.
   */
  @Nonnull
  public String getContentScriptPath() throws InvalidScriptException {
    LOG.trace("Getting Content Script Path for {}", getPath());
    final String contentScriptPath = getScriptPath("content.html");
    LOG.trace("Retrieved Content Script Path for {}", getPath());
    return contentScriptPath;
  }

  /**
   * Retrieves the path to a specified script name, resolved proper to the ComponentUiFramework
   * view.
   *
   * @param scriptName Script to retrieve.
   * @return The path to a specified script.
   * @throws InvalidScriptException The script was not found, or could not be adapt to *
   *     HtmlFile.
   */
  @Nonnull
  public String getScriptPath(final String scriptName) throws InvalidScriptException {
    return scriptProviderService.getScriptPath(this, scriptName);
  }

  private UiFramework getUiFramework()
      throws InvalidThemeException, ResourceNotFoundException, InvalidUiFrameworkException {
    if (uiFramework == null) {
      uiFramework = getTheme().getUiFramework();
    }
    return uiFramework;
  }


}